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
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class ViaGaussianFitter {
  
  public static final double RADIUS = 4.5;
  
  public static double[][] image;
  public static int sx, sy;
  
  public static BufferedImage scaledImage;
  
  public static class ViaViewer implements PointChooser.Handler {
    private TreeMap<Double, Pair<Double, Double>> viasByQuality;
    private TreeSet<Pair<Double, Double>> exclude = new TreeSet<Pair<Double, Double>>();
    private double threshold = 800;
    private double scale = 20;
    
    public ViaViewer(TreeMap<Pair<Double, Double>, Double> vias) {
      viasByQuality = new TreeMap<Double, Pair<Double, Double>>();
      for (Pair<Double, Double> via : vias.keySet())
        viasByQuality.put(vias.get(via) + 0.001 * Math.random(), via);
    }
    
    public TreeSet<Pair<Double, Double>> getVisibleVias() {
      TreeSet<Pair<Double, Double>> result =
          new TreeSet<Pair<Double, Double>>(viasByQuality.headMap(threshold).values());
      result.removeAll(exclude);
      return result;
    }
    
    public void draw(Graphics g, double zoom) {
      for (Double qual : viasByQuality.headMap(threshold).keySet()) {
        Pair<Double, Double> via = viasByQuality.get(qual);
        if (!exclude.contains(via))
          drawCircle(g, zoom, Color.BLUE, via.getA(), via.getB(), RADIUS);
      }
    }
    
    public boolean click(int button, int u, int v) {
      if (button == 2) {
        /*scale *= 2;
        if (scale == 64)
          scale = 1;

        System.out.println("scale " + scale);
        return false;*/

        int x = u + v - sy / 2;
        int y = u - v + sy / 2 - 1;
        
        int minDistSq = 400;
        Pair<Double, Double> minVia = null;
        
        for (Pair<Double, Double> via : viasByQuality.values())
          if (!exclude.contains(via)) {
            int dx = x - (int) Math.round(via.getA());
            int dy = y - (int) Math.round(via.getB());
            int dd = dx * dx + dy * dy;
            if (dd < minDistSq) {
              minDistSq = dd;
              minVia = via;
            }
          }
        
        if (minVia != null)
          exclude.add(minVia);
        
        return true;
      }
      
      if (button == 1 || button == 3) {
        threshold += (2 - button) * scale;
        System.out.println("threshold " + threshold);
        return true;
      }
      
      return false;
    }
  }
  
  public static void drawCircle(Graphics g, double zoom, Color color, double x, double y, double r) {
    x += 0.5;
    y -= 0.5;
    
    int du = (int) Math.round((x + (y + 1)) / 2 * zoom);
    int dv = (int) Math.round((sy - (y + 1) + x) / 2 * zoom);
    int dr = (int) Math.round(r * zoom / Math.sqrt(2));
    
    g.setColor(color);
    for (int i = 0; i != 3; ++i)
      g.drawOval(du - dr + i, dv - dr + i, 2 * (dr - i), 2 * (dr - i));
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
  
  public static double computeGaussian(double t, double x, double y) {
    double s = Math.ceil(3 * Math.sqrt(2 * t));
    double invT = 1 / t;
    
    int x0 = Math.max(0, (int) Math.floor(x - s));
    int y0 = Math.max(0, (int) Math.floor(y - s));
    int x1 = Math.min(sx - 1, (int) Math.ceil(x + s));
    int y1 = Math.min(sy - 1, (int) Math.ceil(y + s));
    
    double sum = 0;
    double weight = 0;
    for (int yy = y0; yy <= y1; ++yy)
      for (int xx = x0 + ((yy + x0 + 1) % 2); xx <= x1; xx += 2) {
        double dx = xx - x;
        double dy = yy - y;
        double w = Math.exp(-invT * (dx * dx + dy * dy));
        
        weight += w;
        sum += w * image[yy][xx];
      }
    
    return sum / weight;
  }
  
  public static void writeDoublePairToDoubleMap(TreeMap<Pair<Double, Double>, Double> map,
      String filename) throws IOException {
    DataOutputStream out = DataTools.openWriting(filename);
    out.writeInt(map.size());
    for (Pair<Double, Double> pair : map.keySet()) {
      out.writeDouble(pair.getA());
      out.writeDouble(pair.getB());
      out.writeDouble(map.get(pair));
    }
    out.close();
  }
  
  public static TreeMap<Pair<Double, Double>, Double> readDoublePairToDoubleMap(String filename)
      throws IOException {
    DataInputStream in = DataTools.openReading(filename);
    TreeMap<Pair<Double, Double>, Double> map = new TreeMap<Pair<Double, Double>, Double>();
    for (int i = in.readInt(); i != 0; --i) {
      double x = in.readDouble();
      double y = in.readDouble();
      double val = in.readDouble();
      map.put(new Pair<Double, Double>(x, y), val);
    }
    in.close();
    return map;
  }
  
  public static void writeDoublePairSet(TreeSet<Pair<Double, Double>> set, String filename)
      throws IOException {
    DataOutputStream out = DataTools.openWriting(filename);
    out.writeInt(set.size());
    for (Pair<Double, Double> point : set) {
      out.writeDouble(point.getA());
      out.writeDouble(point.getB());
    }
    out.close();
  }
  
  public static TreeSet<Pair<Double, Double>> readDoublePairSet(String filename) throws IOException {
    TreeSet<Pair<Double, Double>> set = new TreeSet<Pair<Double, Double>>();
    DataInputStream in = DataTools.openReading(filename);
    for (int i = in.readInt(); i != 0; --i) {
      double x = in.readDouble();
      double y = in.readDouble();
      set.add(new Pair<Double, Double>(x, y));
    }
    in.close();
    return set;
  }
  
  public static double optimizeRadius(Pair<Double, Double>[] vias) {
    final double radiusStart = 4.0;
    final double radiusEnd = 8.0;
    final int radiusSteps = 41;
    
    final double t = 1;
    final int divisions = 36;
    
    double[][] results = new double[vias.length][radiusSteps];
    double[] sums = new double[radiusSteps + 1];
    
    for (int j = 0; j != radiusSteps + 1; ++j) {
      double radius = radiusStart + (radiusEnd - radiusStart) * j / (radiusSteps - 1);
      double[][] circlePoints = new double[divisions][2];
      for (int i = 0; i != divisions; ++i) {
        circlePoints[i][0] = radius * Math.sin(2 * Math.PI * i / divisions);
        circlePoints[i][1] = radius * Math.cos(2 * Math.PI * i / divisions);
      }
      
      for (int i = 0; i != vias.length; ++i) {
        double min = 1000;
        double minCx = 0, minCy = 0;
        for (double dx = -2; dx <= 2; dx += 0.1)
          for (double dy = -2; dy <= 2; dy += 0.1) {
            double cx = vias[i].getA() + dx;
            double cy = vias[i].getB() + dy;
            
            double max = 0;
            for (int k = 0; k != divisions; ++k) {
              double val = computeGaussian(t, cx + circlePoints[k][0], cy + circlePoints[k][1]);
              if (val > max)
                max = val;
            }
            
            if (max < min) {
              min = max;
              minCx = cx;
              minCy = cy;
            }
          }
        
        results[i][j] = min;
        sums[j] += results[i][j];
      }
      
      System.out.printf("radius %1.1f: %4.3f\n", radius, sums[j]);
    }
    
    int[] counts = new int[radiusSteps];
    for (int i = 0; i != vias.length; ++i) {
      double min = 1000;
      int ind = 0;
      for (int j = 0; j != radiusSteps; ++j) {
        if (results[i][j] < min) {
          min = results[i][j];
          ind = j;
        }
      }
      ++counts[ind];
    }
    
    int countsTotal = 0;
    for (int j = 0; j != radiusSteps; ++j)
      countsTotal += j * counts[j];
    
    double radius =
        radiusStart + (radiusEnd - radiusStart) / (radiusSteps - 1) * countsTotal / vias.length;
    System.out.println("mean best radius: " + radius);
    return radius;
  }
  
  public static void main(String[] args) throws IOException {
    prepareImage(args[0]);
    
    //TreeSet<Pair<Double, Double>> visibleVias = readDoublePairSet(DataTools.DIR + "guaranteed-vias");
    
    //TreeMap<Pair<Double, Double>, Double> viaResults = readDoublePairToDoubleMap(DataTools.DIR
    //    + args[1]);
    /*for (Iterator<Pair<Double, Double>> i = viaResults.keySet().iterator(); i.hasNext(); ) {
      Pair<Double, Double> via = i.next();
      double x = via.getFirst();
      double y = via.getSecond();
      if (!(x >= 1000 && x <= 1500 && y >= 1000 && y <= 1500) || !visibleVias.contains(via))
        i.remove();
    }*/
    //System.out.println(viaResults.size());
    
    //ViaViewer viewer = new ViaViewer(viaResults);
    //Tools.display(new PointChooser(scaledImage, viewer));
    
    //visibleVias = viewer.getVisibleVias();
    //System.out.println(visibleVias.size());
    
    //writeDoublePairSet(visibleVias, DataTools.DIR + "guaranteed-vias");
    
    //optimizeRadius(visibleVias.toArray(new Pair[] {}));
    //System.exit(0);
    
    final double t = 1;
    final int divisions = 36;
    
    double[][] circlePoints = new double[divisions][2];
    for (int i = 0; i != divisions; ++i) {
      circlePoints[i][0] = RADIUS * Math.sin(2 * Math.PI * i / divisions);
      circlePoints[i][1] = RADIUS * Math.cos(2 * Math.PI * i / divisions);
    }
    
    TreeSet<Pair<Integer, Integer>> vias = Streams.readObject(DataTools.DIR + args[1]);
    /*for (Iterator<Pair<Integer, Integer>> i = vias.iterator(); i.hasNext();) {
      Pair<Integer, Integer> via = i.next();
      int x = via.getFirst();
      int y = via.getSecond();
      if (x >= 1600 && x <= 1800 && y >= 1200 && y <= 1400)
        ;
      else
        i.remove();
    }*/
    System.out.println(vias.size());
    
    TreeMap<Pair<Double, Double>, Double> realVias = new TreeMap<Pair<Double, Double>, Double>();
    
    int count = 0;
    for (Pair<Integer, Integer> via : vias) {
      System.out.println(count++);
      
      double min = 1000;
      double minCx = 0, minCy = 0;
      for (double dx = -3; dx <= 3; dx += 0.2)
        for (double dy = -3; dy <= 3; dy += 0.2) {
          double cx = via.getA() + dx;
          double cy = via.getB() + dy;
          
          double max = 0;
          for (int i = 0; i != divisions; ++i) {
            double val = computeGaussian(t, cx + circlePoints[i][0], cy + circlePoints[i][1]);
            if (val > max)
              max = val;
          }
          
          if (max < min) {
            min = max;
            minCx = cx;
            minCy = cy;
          }
        }
      
      System.out.println(min);
      //if (min < 800)
      realVias.put(new Pair<Double, Double>(minCx, minCy), min);
    }
    
    writeDoublePairToDoubleMap(realVias, DataTools.DIR + args[2]);
    
    Tools.display(new PointChooser(scaledImage, new ViaViewer(realVias)));
  }
  
}
