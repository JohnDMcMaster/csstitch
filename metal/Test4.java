package metal;

import general.Streams;
import general.collections.Pair;
import general.execution.Bash;
import general.execution.SSHAddress;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.TreeSet;

import cache.Cache;

import data.DataTools;
import data.Tools;
import distributed.Bootstrap;
import distributed.server.Servers;

import operations.image.ImageOpsDouble;
import realmetal.CircleShapeGenerator;
import realmetal.Decomp;
import realmetal.Decomposer;
import realmetal.EdgeTracer;
import realmetal.Images;
import realmetal.PolyDirt;
import realmetal.Vias;

public class Test4 {
  
  public static void showImages(Collection<BufferedImage> images) throws IOException {
    System.err.println("writing...");
    
    String[] titles = new String[images.size()];
    for (int i = 0; i != images.size(); ++i)
      titles[i] = "image " + i;
    
    Viewer.viewRemotely(new SSHAddress("noname-home"), images.toArray(new BufferedImage[] {}),
        titles);
  }
  
  public static TreeMap<Pair<Float, Float>, Double> getThresholds(int stitch, int image,
      boolean good) throws IOException {
    TreeMap<Pair<Float, Float>, Double> result =
        Cache.cache("vias-selected-thresholds/%3$b/%1$d/%2$d", stitch, image, good);
    if (result != null)
      return result;
    
    final double[][] img = EdgeTracer.getImage(stitch, image);
    
    final Object[] circleData = CircleShapeGenerator.computeCircleData(3.f, 6.f, 8);
    final int[][][] shapes = (int[][][]) circleData[2];
    final float[][] centers = (float[][]) circleData[3];
    
    final TreeSet<Pair<Float, Float>> vias = Vias.getUnmappedSelectedVias(stitch, image, good);
    
    final double realDist = 5;
    
    result = new TreeMap<Pair<Float, Float>, Double>();
    
    int counter = 0;
    for (Pair<Float, Float> via : vias) {
      //System.err.println(counter++);
      
      double min = Double.POSITIVE_INFINITY;
      
      for (int i = 0; i != shapes.length; ++i) {
        float cx = via.getA() - centers[i][0];
        float cy = via.getB() - centers[i][1];
        for (int yy = (int) (cy - realDist); yy <= cy + realDist; ++yy)
          for (int xx = (int) (cx - realDist); xx <= cx + realDist; ++xx) {
            if ((xx + yy) % 2 == 0 && MetalTools.getNormSq(xx - cx, yy - cy) < realDist * realDist) {
              double max = Float.NEGATIVE_INFINITY;
              for (int j = 0; j != shapes[i].length; ++j)
                max = Math.max(max, img[yy + shapes[i][j][1]][xx + shapes[i][j][0]]);
              min = Math.min(min, max);
            }
          }
      }
      
      result.put(via, min);
    }
    
    return result;
  }
  
  public static TreeSet<Pair<Double, Pair<Float, Float>>> sort(
      TreeMap<Pair<Float, Float>, Double> thresholds) {
    TreeSet<Pair<Double, Pair<Float, Float>>> sorted =
        new TreeSet<Pair<Double, Pair<Float, Float>>>();
    for (Pair<Float, Float> via : thresholds.keySet())
      sorted.add(new Pair<Double, Pair<Float, Float>>(thresholds.get(via), via));
    return sorted;
  }
  
  public static void main(String[] args) throws IOException {
    int sx = 1625;
    int sy = 1225;
    
    DataInputStream in =
        new DataInputStream(new BufferedInputStream(new FileInputStream(DataTools.DIR + "0.dat")));
    
    double[][] img = new double[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        img[y][x] = in.readShort() / 1024.;
    
    Tools.displayImages(new BufferedImage[] {MetalTools.toImage(img, 128)});
    
    System.exit(0);
    
    Bootstrap.bootstrap(Servers.CIP_91);
    
    final int stitch = 0;
    final int image = 23;
    final int minSegmentLength = 8;
    final double threshold = 0.86;
    
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    
    double[][] d =
        EdgeTracer.combineComponents(Images.getImageComponent(stitch, image, 1),
            Images.getImageComponent(stitch, image, 2));
    images.add(MetalTools.selectGreen(MetalTools.toImage(d, 128)));
    Tools.writePNG(images.get(images.size() - 1), "out.png");
    images.add(MetalTools.selectGreen(MetalTools.toImage(MetalTools.binarize(d, threshold, true))));
    
    //images.add(MetalTools.scale(MetalTools.toImage(Images.getImageComponent(stitch, u, v, 0), 128),
    //    2));
    //images.add(MetalTools.scale(MetalTools.toImage(Images.getImageComponent(stitch, u, v, 3), 128),
    //    2));
    
    //images.add(Images.getGreenImage(stitch, u, v));
    
    /*
    double[][] filtered2 = Decomp.computeFiltered(stitch, u, v, 2, minSegmentLength);
    double[][] filtered1 = Decomp.computeFiltered(stitch, u, v, 1, minSegmentLength);
    images.add(MetalTools.toImage(Images.interpolate(filtered1, filtered2), 128));
    
    double[][][] comp1 = Decomp.computeComponents(stitch, u, v, 1, minSegmentLength);
    double[][][] comp2 = Decomp.computeComponents(stitch, u, v, 2, minSegmentLength);
    images.add(MetalTools.toImage(
        ImageOpsDouble.exp(ImageOpsDouble.add(Images.interpolate(comp1[0], comp2[0]),
            Images.interpolate(comp1[1], comp2[1]))), 128));
    */

    showImages(images);
  }
}
