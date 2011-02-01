package test;

import general.Streams;
import general.collections.Pair;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

@SuppressWarnings("serial")
public class ViaFilterNegatives extends JPanel implements KeyListener, MouseListener,
    WindowListener {
  
  private double[][] image;
  private int sx, sy;
  
  private BufferedImage scaledImage;
  
  private TreeSet<Pair<Integer, Integer>> selectedVias;
  
  final int overlap = 8;
  
  final int scale = 4;
  final int wu = 1000;
  final int wv = 750;
  
  int su, sv;
  int iu, iv;
  
  int nu, nv;
  int pu, pv;
  
  private boolean[] closed;
  
  private JFrame frame;
  
  private final String STEP = "3-4";
  
  public ViaFilterNegatives() throws IOException, InterruptedException {
    prepareImage();
    
    /*TreeMap<Pair<Double, Double>, Double> vias = ViaGaussianFitter
        .readDoublePairToDoubleMap(DataTools.DIR + "vias/" + STEP + ".dat");

    selectedVias = new TreeSet<Pair<Integer, Integer>>();
    for (Pair<Double, Double> via : vias.keySet()) {
      double quality = vias.get(via);
      if (quality >= 500 && quality <= 680) {
        int x = (int) Math.round(via.getFirst());
        int y = (int) Math.round(via.getSecond());
        selectedVias.add(new Pair<Integer, Integer>(x, y));
      }
    }*/

    selectedVias = Streams.readObject(DataTools.DIR + "sel-vias/" + STEP + ".dat");
    System.out.println(selectedVias.size() + " vias");
    
    //selectedVias.remove(new Pair<Integer, Integer>(373, 1544));
    
    su = (sx + sy + 2) / 2;
    sv = (sx + sy + 2) / 2;
    
    nu = (su - overlap) / (wu / scale - overlap) + 1;
    nv = (sv - overlap) / (wv / scale - overlap) + 1;
    
    Dimension dim = new Dimension(wu, wv);
    setMinimumSize(dim);
    setMaximumSize(dim);
    setPreferredSize(dim);
    setSize(dim);
    addMouseListener(this);
    
    closed = new boolean[] {false};
    
    frame = new JFrame();
    frame.addKeyListener(this);
    frame.addWindowListener(this);
    frame.setResizable(false);
    frame.getContentPane().add(this);
    frame.pack();
    
    setImagePosition(nu / 2, nv / 2);
    
    frame.setVisible(true);
    
    synchronized (closed) {
      if (!closed[0])
        closed.wait();
    }
    
    Streams.writeObject(DataTools.DIR + "sel-vias/" + STEP + ".dat", selectedVias);
  }
  
  private void prepareImage() throws IOException {
    double[][] matrix =
        DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat"));
    image =
        Tools.getMatrixFromImage(ImageIO.read(new File(DataTools.DIR + "images/" + STEP + ".png")));
    sx = image[0].length;
    sy = image.length;
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 0)
          image[y][x] = 0;
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if (x % 2 == 1 && y % 2 == 0)
          image[y][x] *= 0.9757262;
        image[y][x] /= matrix[y][x];
      }
    
    double[][] scaledMatrix = Tools.rotateMatrix(image);
    Tools.scaleMatrix(scaledMatrix, 0.25);
    scaledImage = Tools.getGreyscaleImageFromMatrix(scaledMatrix);
    Tools.display(new PointChooser(scaledImage));
  }
  
  private void setImagePosition(int pu, int pv) {
    this.pu = pu;
    this.pv = pv;
    
    frame.setTitle("tile (" + pu + ", " + pv + ")");
    
    iu = pu * (su - overlap) / nu;
    iv = pv * (sv - overlap) / nv;
    
    invalidate();
    repaint();
  }
  
  private void scrollImage(int du, int dv) {
    int newu = pu + du;
    int newv = pv + dv;
    
    newu = Math.max(0, newu);
    newv = Math.max(0, newv);
    
    newu = Math.min(nu - 1, newu);
    newv = Math.min(nv - 1, newv);
    
    setImagePosition(newu, newv);
  }
  
  public void keyTyped(KeyEvent e) {
  }
  
  public void keyPressed(KeyEvent e) {
    int c = e.getKeyCode();
    if (c == KeyEvent.VK_LEFT)
      scrollImage(-1, 0);
    else if (c == KeyEvent.VK_RIGHT)
      scrollImage(1, 0);
    else if (c == KeyEvent.VK_UP)
      scrollImage(0, -1);
    else if (c == KeyEvent.VK_DOWN)
      scrollImage(0, 1);
  }
  
  public void keyReleased(KeyEvent e) {
  }
  
  public void drawPixel(Graphics g, Color color, int x0, int y0) {
    int u0 = (x0 + (y0 + 1)) / 2 - iu;
    int v0 = (sy - (y0 + 1) + x0) / 2 - iv;
    
    int u1 = u0 + 1;
    int v1 = v0 + 1;
    
    int du0 = (int) (u0 * scale);
    int du1 = (int) (u1 * scale);
    
    int dv0 = (int) (v0 * scale);
    int dv1 = (int) (v1 * scale);
    
    g.setColor(color);
    g.drawRect(du0 + 0, dv0 + 0, du1 - du0 - 0, dv1 - dv0 - 0);
    g.drawRect(du0 + 1, dv0 + 1, du1 - du0 - 2, dv1 - dv0 - 2);
    g.drawRect(du0 + 2, dv0 + 2, du1 - du0 - 4, dv1 - dv0 - 4);
  }
  
  public void paintComponent(Graphics g) {
    g.drawImage(scaledImage, 0, 0, scale * (wu / scale + 1), scale * (wv / scale + 1), iu, iv, iu
        + wu / scale + 1, iv + wv / scale + 1, null);
    
    for (Pair<Integer, Integer> via : selectedVias) {
      int x = via.getA();
      int y = via.getB();
      
      int u = (x + (y + 1)) / 2;
      int v = (sy - (y + 1) + x) / 2;
      
      if (u >= iu && v >= iv && u <= iu + wu / scale && v <= iv + wv / scale) {
        drawPixel(g, Color.BLUE, x, y);
      }
    }
  }
  
  public void mouseClicked(MouseEvent e) {
    int u = iu + e.getX() / scale;
    int v = iv + e.getY() / scale;
    
    int x = u + v - sy / 2;
    int y = u - v + sy / 2 - 1;
    
    boolean repaint = false;
    
    if (e.getButton() == 1) {
      selectedVias.add(new Pair<Integer, Integer>(x, y));
      repaint = true;
    } else if (e.getButton() == 3) {
      Pair<Integer, Integer> min = null;
      int minDist = 200;
      for (Pair<Integer, Integer> via : selectedVias) {
        int dx = x - via.getA();
        int dy = y - via.getB();
        int dd = dx * dx + dy * dy;
        if (dd < minDist) {
          min = via;
          minDist = dd;
        }
      }
      
      if (min != null) {
        selectedVias.remove(min);
        repaint = true;
      }
    }
    
    if (repaint) {
      invalidate();
      repaint();
    }
  }
  
  public void mousePressed(MouseEvent e) {
  }
  
  public void mouseReleased(MouseEvent e) {
  }
  
  public void mouseEntered(MouseEvent e) {
  }
  
  public void mouseExited(MouseEvent e) {
  }
  
  public void windowOpened(WindowEvent e) {
  }
  
  public void windowClosing(WindowEvent e) {
    frame.dispose();
  }
  
  public void windowClosed(WindowEvent e) {
    synchronized (closed) {
      if (closed[0])
        return;
      
      closed[0] = true;
      closed.notify();
    }
  }
  
  public void windowIconified(WindowEvent e) {
  }
  
  public void windowDeiconified(WindowEvent e) {
  }
  
  public void windowActivated(WindowEvent e) {
  }
  
  public void windowDeactivated(WindowEvent e) {
  }
  
  public static void main(String[] args) throws IOException, InterruptedException {
    new ViaFilterNegatives();
  }
  
}
