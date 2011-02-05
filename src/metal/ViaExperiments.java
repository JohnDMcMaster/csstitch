package metal;

import general.Streams;
import general.Statistics;
import general.collections.Pair;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import realmetal.CircleShapeGenerator;
import realmetal.Vias;

import data.Tools;
import distributed.Bootstrap;
import distributed.server.Servers;

public class ViaExperiments {
  
  private static final int SX = 3250;
  private static final int SY = 2450;
  
  private static final float REAL_DIST = 3.0f;
  
  public static Object[] getCircles() throws IOException {
    if (new File("circles").exists())
      return Streams.readObject("circles");
    
    Object[] data = CircleShapeGenerator.computeCircleData(3.0f, 6.0f, 7);
    Streams.writeObject("circles", data);
    return data;
  }
  
  public static TreeMap<Pair<Float, Float>, Float> computeThresholds(float[][] image,
      TreeSet<Pair<Float, Float>> vias, Object[] circles) throws IOException {
    int[][][] shapes = (int[][][]) circles[2];
    float[][] centers = (float[][]) circles[3];
    
    TreeMap<Pair<Float, Float>, Float> thresholds = new TreeMap<Pair<Float, Float>, Float>();
    int counter = 0;
    
    for (Pair<Float, Float> via : vias) {
      System.err.println(counter++);
      
      float min = Float.POSITIVE_INFINITY;
      for (int i = 0; i != shapes.length; ++i) {
        float cx = via.getA() - centers[i][0];
        float cy = via.getB() - centers[i][1];
        for (int yy = (int) (cy - REAL_DIST); yy <= cy + REAL_DIST; ++yy)
          for (int xx = (int) (cx - REAL_DIST) + (yy + (int) (cx - REAL_DIST)) % 2; xx <= cx
              + REAL_DIST; xx += 2)
            if (MetalTools.getNormSq(xx - cx, yy - cy) < REAL_DIST * REAL_DIST) {
              float max = Float.NEGATIVE_INFINITY;
              for (int j = 0; j != shapes[i].length; ++j)
                max = Math.max(max, image[yy + shapes[i][j][1]][xx + shapes[i][j][0]]);
              min = Math.min(min, max);
            }
      }
      thresholds.put(via, min);
    }
    
    return thresholds;
  }
  
  public static void drawScaledOval(Graphics g, Color c, float x, float y, float r, int t) {
    float uu = (x + y - 1) / 2f;
    float vv = (x - y + SY - 1) / 2f;
    
    g.setColor(c);
    for (int i = 0; i != t; ++i) {
      float meanRad = r / (float) Math.sqrt(2) + i;
      
      int x0 = (int) Math.round(uu - meanRad);
      int y0 = (int) Math.round(vv - meanRad);
      
      int x1 = (int) Math.round(uu + meanRad);
      int y1 = (int) Math.round(vv + meanRad);
      
      g.drawOval(x0, y0, x1 - x0, y1 - y0);
    }
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_90);
    
    int image = 23;
    
    float[][] img = MetalTools.getImageValues(0, image);
    
    TreeSet<Pair<Float, Float>> vias = Vias.getUnmappedSelectedVias(0, image, false);
    System.out.println(vias.size());
    
    Object[] circles = getCircles();
    int[][][] shapes = (int[][][]) circles[2];
    System.out.println(shapes.length);
    
    TreeMap<Pair<Float, Float>, Float> thresholds = computeThresholds(img, vias, circles);
    
    BufferedImage im = MetalTools.selectGreen(MetalTools.toImage(img, 128));
    Graphics g = im.getGraphics();
    for (Pair<Float, Float> via : vias)
      drawScaledOval(g, thresholds.get(via) > 0.80f ? Color.BLACK : Color.WHITE, via.getA(),
          via.getB(), 10, 4);
    Tools.writePNG(im, "image.png");
    System.out.println("image written");
    
    Statistics.printMap(Statistics.getHistogram(thresholds.values()));
  }
}
