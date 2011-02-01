package stitcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import data.DataTools;

public class Demosaicer2 {

  @SuppressWarnings("serial")
  public static class Panel extends JPanel implements KeyListener, MouseInputListener {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private float scale = 1;

    private int[] stitches;
    private BufferedImage[] images;
    private float[][] coords;

    private int cursor;

    private int[] selection = new int[2];

    public Panel(int[] stitches) throws IOException {
      this.stitches = stitches.clone();
      images = new BufferedImage[stitches.length];
      coords = new float[stitches.length][2];

      for (int i = 0; i != stitches.length; ++i) {
        System.err.println("reading stitch " + this.stitches[i] + "...");
        images[i] = ImageIO.read(new File(DataTools.DIR + "sharp-stitch-1.0"
            + StitchInfo.SUFFICES[stitches[i]] + ".png"));
      }

      Dimension dimension = new Dimension(WIDTH, HEIGHT);
      setMinimumSize(dimension);
      setMaximumSize(dimension);
      setSize(dimension);
      setPreferredSize(dimension);

      addMouseListener(this);

      final JFrame frame = new JFrame();
      frame.getContentPane().add(this);
      frame.pack();

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setFocusTraversalKeysEnabled(false);

      frame.addKeyListener(this);

      frame.setVisible(true);

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      while (true) {
        String line = in.readLine();
        if (line == null)
          break;

        Scanner scanner = new Scanner(line);
        scanner.useDelimiter("[ \t\n,;()]+");
        
        for (int i = 0; i != stitches.length; ++i) {
          coords[i][0] = selection[0] / scale - scanner.nextFloat();
          coords[i][1] = selection[1] / scale - scanner.nextFloat();
        }
        
        invalidate();
        repaint();
      }
    }

    public void paintComponent(Graphics g) {
      Rectangle r = g.getClipBounds();

      int dx0 = r.x;
      int dy0 = r.y;
      int dx1 = dx0 + r.width;
      int dy1 = dy0 + r.height;

      int sx0 = (int) (dx0 / scale - coords[cursor][0]);
      int sy0 = (int) (dy0 / scale - coords[cursor][1]);
      int sx1 = (int) (dx1 / scale - coords[cursor][0]) + 1;
      int sy1 = (int) (dy1 / scale - coords[cursor][1]) + 1;

      dx0 = (int) ((sx0 + coords[cursor][0]) * scale);
      dy0 = (int) ((sy0 + coords[cursor][1]) * scale);
      dx1 = (int) ((sx1 + coords[cursor][0]) * scale);
      dy1 = (int) ((sy1 + coords[cursor][1]) * scale);

      g.drawImage(images[cursor], dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);

      g.setColor(Color.RED);
      g.drawLine(selection[0] - 16, selection[1], selection[0] + 16, selection[1]);
      g.drawLine(selection[0], selection[1] - 16, selection[0], selection[1] + 16);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      int c = e.getKeyCode();

      if (c == KeyEvent.VK_TAB) {
        if (e.isShiftDown())
          --cursor;
        else
          ++cursor;
        cursor = (cursor + stitches.length) % stitches.length;

        invalidate();
        repaint();
      } else if (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_UP
          || c == KeyEvent.VK_DOWN) {

        float factor = 1 / scale;
        if (e.isControlDown())
          factor *= 100;
        if (e.isShiftDown())
          factor *= 10;

        if (c == KeyEvent.VK_LEFT)
          coords[cursor][0] += factor;
        else if (c == KeyEvent.VK_RIGHT)
          coords[cursor][0] -= factor;
        else if (c == KeyEvent.VK_UP)
          coords[cursor][1] += factor;
        else
          coords[cursor][1] -= factor;

        invalidate();
        repaint();
      } else if (c == KeyEvent.VK_PAGE_UP || c == KeyEvent.VK_PAGE_DOWN) {
        for (int i = 0; i != images.length; ++i) {
          coords[i][0] -= selection[0] / scale;
          coords[i][1] -= selection[1] / scale;
        }

        if (c == KeyEvent.VK_PAGE_UP)
          scale *= 2;
        else
          scale /= 2;

        for (int i = 0; i != images.length; ++i) {
          coords[i][0] += selection[0] / scale;
          coords[i][1] += selection[1] / scale;
        }

        invalidate();
        repaint();
      } else if (c == KeyEvent.VK_ENTER) {
        for (int i = 0; i != stitches.length; ++i) {
          System.out.print("(");
          System.out.print(selection[0] / scale - coords[i][0]);
          System.out.print(", ");
          System.out.print(selection[1] / scale - coords[i][1]);
          System.out.print("); ");
        }
        System.out.println();
      }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
      if (e.getButton() == 1) {
        selection[0] = e.getX();
        selection[1] = e.getY();

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

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

  }

  public static void main(String[] args) throws IOException {
    @SuppressWarnings("unused")
    Panel panel = new Panel(new int[] {0, 1, 2});
  }

}
