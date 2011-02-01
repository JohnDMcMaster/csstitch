package realmetal;

import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import cache.Cache;

import operations.image.ImageOpsDouble;

import distributed.Bootstrap;
import distributed.server.Servers;

import metal.MetalTools;
import metal.Test4;

public class EdgeTracer {
  
  public static double[][] combineComponents(double[][] a, double[][] b) {
    double[][] result = new double[2 * a.length][2 * a[0].length];
    for (int y = 0; y != a.length; ++y)
      for (int x = 0; x != a[0].length; ++x) {
        result[2 * y + 0][2 * x + 1] = a[y][x];
        result[2 * y + 1][2 * x + 0] = b[y][x];
      }
    
    return result;
  }
  
  public static double[][] getImage(int stitch, int image) throws IOException {
    double[][] i1 = Images.getImageComponent(stitch, image, 1);
    double[][] i2 = Images.getImageComponent(stitch, image, 2);
    return combineComponents(i1, i2);
  }
  
  public static double[][] computeComponentRemainder(int stitch, int image, int component,
      int minSegmentLength) throws IOException {
    double[][] img = Images.getImageComponent(stitch, image, component);
    double[][][] components = Decomp.computeComponents(stitch, image, component, minSegmentLength);
    
    return ImageOpsDouble.subtract(ImageOpsDouble.add(components[0], components[1]),
        ImageOpsDouble.log(img));
  }
  
  public static double[][] computeRemainder(int stitch, int image, int minSegmentLength)
      throws IOException {
    double[][] r1 = computeComponentRemainder(stitch, image, 1, minSegmentLength);
    double[][] r2 = computeComponentRemainder(stitch, image, 2, minSegmentLength);
    return combineComponents(r1, r2);
  }
  
  public static BufferedImage plotImage(int stitch, int image) throws IOException {
    return MetalTools.selectGreen(MetalTools.toImage(getImage(stitch, image), 128));
  }
  
  public static BufferedImage plotRemainder(int stitch, int image, int minSegmentLength)
      throws IOException {
    return MetalTools.selectGreen(MetalTools.toImage(
        ImageOpsDouble.subtract(
            ImageOpsDouble.exp(computeRemainder(stitch, image, minSegmentLength)), 1), 256));
  }
  
  public static TreeSet<Pair<Double, Pair<Integer, Integer>>> organizeLine(double[] line,
      int minLength) {
    TreeSet<Pair<Double, Integer>> sorted = new TreeSet<Pair<Double, Integer>>();
    for (int i = 0; i != line.length; ++i)
      sorted.add(new Pair<Double, Integer>(line[i], i));
    
    TreeMap<Pair<Integer, Integer>, Double> edgeValues =
        new TreeMap<Pair<Integer, Integer>, Double>();
    
    TreeSet<Integer> segmentation = new TreeSet<Integer>();
    for (Pair<Double, Integer> entry : sorted) {
      int i = entry.getB();
      
      double value = line[i];
      int lower = i;
      if (segmentation.contains(i)) {
        lower = segmentation.lower(i);
        value = Math.max(value, edgeValues.get(new Pair<Integer, Integer>(lower, i)));
        segmentation.remove(i);
      } else
        segmentation.add(i);
      
      int higher = i + 1;
      if (segmentation.contains(i + 1)) {
        higher = segmentation.higher(i + 1);
        value = Math.max(value, edgeValues.get(new Pair<Integer, Integer>(i + 1, higher)));
        segmentation.remove(i + 1);
      } else
        segmentation.add(i + 1);
      
      edgeValues.put(new Pair<Integer, Integer>(lower, higher), value);
    }
    
    TreeSet<Pair<Double, Pair<Integer, Integer>>> result =
        new TreeSet<Pair<Double, Pair<Integer, Integer>>>();
    for (Pair<Integer, Integer> edge : edgeValues.keySet())
      if (edge.getB() - edge.getA() >= minLength)
        result.add(new Pair<Double, Pair<Integer, Integer>>(edgeValues.get(edge), edge));
    return result;
  }
  
  public static Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[]
      organizeComponent(int image, int component, int minLength) throws IOException {
    Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] result =
        Cache.cache("decomp-edges/%d/%d/%d", image, component, minLength);
    if (result != null)
      return result;
    
    double[][][] components = Decomp.computeComponents(0, image, component, minLength);
    
    TreeSet<Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>> edges =
        new TreeSet<Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>>();
    for (int i = 0; i != 2; ++i) {
      for (int j = 0; j != ImageOpsDouble.getLimit(components[i], i); ++j) {
        System.err.println(j);
        double[] line = ImageOpsDouble.getLine(components[i], j, i);
        for (Pair<Double, Pair<Integer, Integer>> edge : organizeLine(line, minLength))
          edges.add(new Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>(edge
              .getA(), new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(
              new Pair<Integer, Integer>(i, j), edge.getB())));
      }
    }
    
    return edges.toArray(new Pair[] {});
  }
  
  @SuppressWarnings("unchecked")
  public static Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[]
      organizeComponents(int image, int minLength) throws IOException {
    Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] result =
        Cache.cache("decomp-green-edges/%d/%d",image, minLength);
    if (result != null)
      return result;
    
    TreeSet<Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>> edges =
        new TreeSet<Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>>();
    for (int i = 0; i != 2; ++i)
      for (Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> entry : organizeComponent(
          image, i + 1, minLength)) {
        Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> edge = entry.getB();
        Pair<Integer, Integer> edgeInfo = edge.getA();
        Pair<Integer, Integer> edgePosition = edge.getB();
        
        int dir = edgeInfo.getA();
        edgeInfo = new Pair<Integer, Integer>(dir, 2 * edgeInfo.getB() + (dir + i) % 2);
        
        int offset = (dir + i + 1) % 2;
        edgePosition =
            new Pair<Integer, Integer>(2 * edgePosition.getA() + offset,
                2 * (edgePosition.getB() - 1) + 1);
        
        edges.add(new Pair<Double, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>(entry
            .getA(), new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(edgeInfo,
            edgePosition)));
      }
    
    return edges.toArray(new Pair[] {});
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    
    int stitch = 0;
    int image = 23;
    
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    
    System.out.println(organizeComponents(image, 10).length);
    System.exit(0);
    
    Test4.showImages(images);
  }
  
}
