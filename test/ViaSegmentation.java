package test;

import general.Streams;
import general.collections.Pair;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class ViaSegmentation {
  
  private static final int[][] NEIGHBOURS = { {-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
  private static final int[][] DIAG_NEIGHBOURS = { {1, 1}, {2, 0}, {1, -1}, {0, -2}, {-1, -1},
      {-2, 0}, {-1, 1}, {0, 2}};
  
  public static double[][] image;
  public static int sx, sy;
  
  public static BufferedImage scaledImage;
  
  public static Pair<Integer, Integer>[][] POINTS;
  
  public static class Entry implements Comparable<Entry> {
    int x, y;
    double val;
    
    public Entry(int x, int y, double val) {
      this.x = x;
      this.y = y;
      this.val = val;
    }
    
    public int compareTo(Entry e) {
      double dv = val - e.val;
      if (dv > 0)
        return 1;
      
      if (dv < 0)
        return -1;
      
      int dy = y - e.y;
      if (dy != 0)
        return dy;
      
      return x - e.x;
    }
    
    public String toString() {
      return "(" + x + ", " + y + ")";
    }
  };
  
  public static class Via implements Serializable {
    private static final long serialVersionUID = 1L;

    int ax0, ay0;
    TreeSet<Pair<Integer, Integer>> component;
    
    public Via(int ax0, int ay0, TreeSet<Pair<Integer, Integer>> component) {
      this.ax0 = ax0;
      this.ay0 = ay0;
      this.component = component;
    }
  }
  
  public static class ViaViewer implements PointChooser.Handler {
    private TreeSet<Pair<Integer, Integer>> vias;
    
    public ViaViewer(TreeSet<Pair<Integer, Integer>> vias) {
      this.vias = vias;
    }
    
    public void draw(Graphics g, double zoom) {
      for (Pair<Integer, Integer> via : vias)
        drawPixel(g, zoom, Color.BLUE, via.getA(), via.getB());
    }
    
    public boolean click(int button, int x, int y) {
      return false;
    }
  }
  
  public static class ProgressViewer implements PointChooser.Handler {
    private int[][] state;
    private int ax0, ay0, ax1, ay1, cx, cy;
    private Object signal;
    
    public ProgressViewer(int[][] state, int ax0, int ay0, int cx, int cy, Object signal) {
      this.state = state;
      this.ax0 = ax0;
      this.ay0 = ay0;
      this.cx = cx;
      this.cy = cy;
      this.signal = signal;
      
      ax1 = ax0 + state[0].length;
      ay1 = ay0 + state.length;
    }
    
    public void draw(Graphics g, double zoom) {
      drawRotatedRect(g, zoom, Color.GREEN, ax0, ay0, ax1, ay1);
      
      for (int y = ay0; y != ay1; ++y)
        for (int x = ax0 + ((y + ax0 + 1) % 2); x < ax1; x += 2) {
          int p = x - ax0;
          int q = y - ay0;
          
          Color color = null;
          //if (x == cx && y == cy)
          //  color = Color.BLACK;
          if (state[q][p] == -1)
            color = Color.RED;
          else if (state[q][p] > 0)
            color = Color.BLUE;
          
          if (color != null)
            drawPixel(g, zoom, color, x, y);
        }
    }
    
    public boolean click(int button, int x, int y) {
      if (button == 1) {
        synchronized (signal) {
          signal.notify();
        }
        return false;
      }
      
      if (button == 3) {
        for (int i = 0; i != 10; ++i) {
          synchronized (signal) {
            signal.notify();
          }
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      
      return false;
    }
  }
  
  public static void drawPixel(Graphics g, double zoom, Color color, int x0, int y0) {
    int u0 = (x0 + (y0 + 1)) / 2;
    int v0 = (sy - (y0 + 1) + x0) / 2;
    
    int u1 = u0 + 1;
    int v1 = v0 + 1;
    
    int du0 = (int) (u0 * zoom);
    int du1 = (int) (u1 * zoom);
    
    int dv0 = (int) (v0 * zoom);
    int dv1 = (int) (v1 * zoom);
    
    g.setColor(color);
    g.drawRect(du0 + 0, dv0 + 0, du1 - du0 - 0, dv1 - dv0 - 0);
    g.drawRect(du0 + 1, dv0 + 1, du1 - du0 - 2, dv1 - dv0 - 2);
    g.drawRect(du0 + 2, dv0 + 2, du1 - du0 - 4, dv1 - dv0 - 4);
  }
  
  public static void drawRotatedRect(Graphics g, double zoom, Color color, int x0, int y0, int x1,
      int y1) {
    double xx0 = x0 + 0.5;
    double yy0 = y0 - 0.5;
    
    double xx1 = x1 + 0.5;
    double yy1 = y1 - 0.5;
    
    double[] xs = {xx0, xx0, xx1, xx1};
    double[] ys = {yy0, yy1, yy1, yy0};
    
    int[] dus = new int[4];
    int[] dvs = new int[4];
    
    for (int i = 0; i != 4; ++i) {
      double u = (xs[i] + (ys[i] + 1)) / 2;
      double v = (sy - (ys[i] + 1) + xs[i]) / 2;
      
      dus[i] = (int) (u * zoom);
      dvs[i] = (int) (v * zoom);
    }
    
    g.setColor(color);
    for (int i = 0; i != 4; ++i) {
      g.drawPolygon(dus, dvs, 4);
      
      ++dus[0];
      ++dvs[1];
      --dus[2];
      --dvs[3];
    }
  }
  
  public static void initPoints() {
    final int k = 32;
    POINTS = new Pair[k][k];
    for (int y = 0; y != k; ++y)
      for (int x = 0; x != k; ++x)
        POINTS[y][x] = new Pair<Integer, Integer>(x, y);
  }
  
  public static Pair<Integer, Integer> createPoint(int x, int y) {
    if (x >= 0 && y > 0 && x < 32 && y < 32)
      return POINTS[y][x];
    return new Pair<Integer, Integer>(x, y);
  }
  
  public static void prepareImage(String filename) throws IOException {
    // "/media/book/decapsulation/micro-backup/grey-data/P6114227.PNG"
    // DataTools.DIR + "corner.png"
    double[][] matrix =
        DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat"));
    image = Tools.getMatrixFromImage(ImageIO.read(new File(DataTools.DIR + filename)));
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
    Tools.scaleMatrix(scaledMatrix, 64);
    scaledImage = Tools.getGreyscaleImageFromMatrix(scaledMatrix);
    //Tools.display(new PointChooser(scaledImage));
  }
  
  public static TreeSet<Pair<Integer, Integer>> computeComponent(int[][] state,
      Pair<Integer, Integer> point) {
    int sp = state[0].length;
    int sq = state.length;
    
    int s = state[point.getB()][point.getA()];
    
    TreeSet<Pair<Integer, Integer>> result = new TreeSet<Pair<Integer, Integer>>();
    TreeSet<Pair<Integer, Integer>> workingSet = new TreeSet<Pair<Integer, Integer>>();
    workingSet.add(point);
    
    while (!workingSet.isEmpty()) {
      point = workingSet.pollFirst();
      if (result.add(point)) {
        int p = point.getA();
        int q = point.getB();
        for (int i = 0; i != 8; ++i) {
          int pp = p + DIAG_NEIGHBOURS[i][0];
          int qq = q + DIAG_NEIGHBOURS[i][1];
          if (pp >= 0 && qq >= 0 && pp < sq && qq < sq && state[qq][pp] == s)
            workingSet.add(createPoint(pp, qq));
        }
      }
    }
    
    return result;
  }
  
  public static boolean isInfiniteComponent(int[][] state, Pair<Integer, Integer> point) {
    int sp = state[0].length;
    int sq = state.length;
    
    int s = state[point.getB()][point.getA()];
    
    TreeSet<Pair<Integer, Integer>> checked = new TreeSet<Pair<Integer, Integer>>();
    TreeSet<Pair<Integer, Integer>> workingSet = new TreeSet<Pair<Integer, Integer>>();
    workingSet.add(point);
    while (!workingSet.isEmpty()) {
      point = workingSet.pollFirst();
      if (checked.add(point)) {
        int p = point.getA();
        int q = point.getB();
        if (p <= 1 || q <= 1 || p >= sp - 2 || q >= sq - 2)
          return true;
        
        for (int i = 0; i != 8; ++i) {
          int pp = p + DIAG_NEIGHBOURS[i][0];
          int qq = q + DIAG_NEIGHBOURS[i][1];
          if (state[qq][pp] == s)
            workingSet.add(createPoint(pp, qq));
        }
      }
    }
    
    return false;
  }
  
  public static void fillComponent(int[][] state, TreeSet<Pair<Integer, Integer>> component,
      int value) {
    for (Pair<Integer, Integer> point : component)
      state[point.getB()][point.getA()] = value;
  }
  
  public static double computeDiameter(TreeSet<Pair<Integer, Integer>> component) {
    TreeSet<Pair<Integer, Integer>> nonCorners = new TreeSet<Pair<Integer, Integer>>();
    outer: for (Pair<Integer, Integer> point : component) {
      middle: for (int i = 0; i != 4; ++i) {
        for (int j = 0; j != 2; ++j) {
          int p = point.getA() + DIAG_NEIGHBOURS[i + 4 * j][0];
          int q = point.getB() + DIAG_NEIGHBOURS[i + 4 * j][1];
          if (!component.contains(createPoint(p, q)))
            continue middle;
        }
        
        nonCorners.add(point);
        continue outer;
      }
    }
    
    TreeSet<Pair<Integer, Integer>> corners = new TreeSet<Pair<Integer, Integer>>(component);
    corners.removeAll(nonCorners);
    Pair<Integer, Integer>[] cornerArray = corners.toArray(new Pair[] {});
    
    int max = 0;
    for (int i = 0; i != cornerArray.length; ++i)
      for (int j = i + 1; j != cornerArray.length; ++j) {
        int dx = cornerArray[i].getA() - cornerArray[i].getA();
        int dy = cornerArray[i].getB() - cornerArray[j].getB();
        int dd = dx * dx + dy * dy;
        if (dd > max)
          max = dd;
      }
    
    return Math.sqrt(max);
  }
  
  public static Pair<Integer, Integer> computeMidpoint(TreeSet<Pair<Integer, Integer>> component) {
    int x = 0, y = 0;
    for (Pair<Integer, Integer> p : component) {
      x += p.getA();
      y += p.getB();
    }
    
    x = (x + component.size() / 2) / component.size();
    y = (y + component.size() / 2) / component.size();
    return createPoint(x, y);
  }
  
  public static void viewResults() throws IOException {
    TreeSet<Pair<Integer, Integer>> vias = Streams.readObject(DataTools.DIR + "via-candidates");
    Tools.display(new PointChooser(scaledImage, new ViaViewer(vias)));
  }
  
  public static void main(String[] args) throws IOException, InterruptedException {
    prepareImage("P6114227.PNG");
    initPoints();
    
    //viewResults();
    
    ArrayList<Via> vias = new ArrayList<ViaSegmentation.Via>();
    TreeSet<Pair<Integer, Integer>> viaPoints = new TreeSet<Pair<Integer, Integer>>();
    
    long a = System.currentTimeMillis();
    
    for (int by0 = 0; by0 < sy; by0 += 10) {
      System.out.println(by0);
      
      for (int bx0 = 0; bx0 < sx; bx0 += 10) {
        int bx1 = Math.min(sx, bx0 + 8);
        int by1 = Math.min(sy, by0 + 8);
        
        double min = 1024;
        int cx = 0, cy = 0;
        for (int y = by0; y != by1; ++y)
          for (int x = bx0 + (y + bx0 + 1) % 2; x < bx1; x += 2)
            if (image[y][x] < min) {
              min = image[y][x];
              cx = x;
              cy = y;
            }
        
        int ax0 = Math.max(0, cx - 20);
        int ay0 = Math.max(0, cy - 20);
        int ax1 = Math.min(sx, cx + 20);
        int ay1 = Math.min(sy, cy + 20);
        
        int sp = ax1 - ax0;
        int sq = ay1 - ay0;
        
        int[][] state = new int[sq][sp];
        int numPixels = sp * sq / 2;
        int numBoundaryPixels = 0;
        
        /*Object signal = 1;
        PointChooser chooser = new PointChooser(scaledImage, new ProgressViewer(state, ax0, ay0,
            cx, cy, signal));
        JViewport viewport = chooser.getViewport();

        final JFrame frame = new JFrame();
        frame.getContentPane().add(chooser);
        frame.pack();
        frame.setVisible(true);*/

        int compCounter = 1;
        TreeSet<Entry> workingSet = new TreeSet<Entry>();
        workingSet.add(new Entry(cx - ax0, cy - ay0, image[cy][cx]));
        
        while (!workingSet.isEmpty()) {
          Entry entry = workingSet.pollFirst();
          if (image[ay0 + entry.y][ax0 + entry.x] > 750)
            break;
          
          int oldComponent = state[entry.y][entry.x];
          
          /*System.out.print("waiting for signal: ... ");
          synchronized (signal) {
            signal.wait();
          }
          System.out.println("got signal");*/

          state[entry.y][entry.x] = -1;
          if (3 * ++numBoundaryPixels >= numPixels)
            break;
          
          //viewport.invalidate();
          //viewport.repaint();
          
          for (int i = 0; i != 4; ++i) {
            int pp = entry.x + NEIGHBOURS[i][0];
            int qq = entry.y + NEIGHBOURS[i][1];
            if (pp >= 0 && qq >= 0 && pp < sp && qq < sq && state[qq][pp] != -1)
              workingSet.add(new Entry(pp, qq, image[ay0 + qq][ax0 + pp]));
          }
          
          TreeSet<Pair<Integer, Integer>> compInitializers = new TreeSet<Pair<Integer, Integer>>();
          boolean component = false;
          boolean boundary = false;
          boolean outOfWindow = false;
          boolean outOfWindowComponent = false;
          
          for (int i = 0; i != 16; ++i) {
            int pp = entry.x + DIAG_NEIGHBOURS[i % 8][0];
            int qq = entry.y + DIAG_NEIGHBOURS[i % 8][1];
            if (pp >= 0 && qq >= 0 && pp < sp && qq < sq) {
              int s = state[qq][pp];
              if (s != -1 && boundary) {
                if (!component)
                  component = true;
                else
                  compInitializers.add(createPoint(pp, qq));
                
                boundary = false;
              } else if (s == -1 && i % 2 == 0) {
                if (outOfWindow && boundary)
                  outOfWindowComponent = true;
                
                boundary = true;
                outOfWindow = false;
              }
            } else
              outOfWindow = true;
          }
          
          Pair<Integer, Integer> infiniteComponent = null;
          if (!outOfWindowComponent)
            for (Pair<Integer, Integer> compInit : compInitializers)
              if (isInfiniteComponent(state, compInit))
                infiniteComponent = compInit;
          
          boolean skipOne = true;
          for (Pair<Integer, Integer> compInit : compInitializers)
            if (!compInit.equals(infiniteComponent)) {
              TreeSet<Pair<Integer, Integer>> compSet = computeComponent(state, compInit);
              
              if (skipOne && state[compInit.getB()][compInit.getA()] != 0)
                skipOne = false;
              else
                fillComponent(state, compSet, compCounter++);
              
              if (compSet.size() < 8 || compSet.size() > 26)
                continue;
              
              if (computeDiameter(compSet) >= 7)
                continue;
              
              Pair<Integer, Integer> v = computeMidpoint(compSet);
              viaPoints.add(new Pair<Integer, Integer>(ax0 + v.getA(), ay0 + v.getB()));
              vias.add(new Via(ax0, ay0, compSet));
            }
        }
        
        //frame.dispose();
      }
    }
    
    long b = System.currentTimeMillis();
    System.out.println(b - a);
    
    Streams.writeObject(DataTools.DIR + "via-points", viaPoints);
    Streams.writeObject(DataTools.DIR + "via-components", vias);
    
    Tools.display(new PointChooser(scaledImage, new ViaViewer(viaPoints)));
  }
  
}
