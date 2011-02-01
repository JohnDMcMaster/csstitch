package metal2;

import general.Streams;
import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.TreeSet;

import operations.image.ImageOpsDouble;

import distributed.Bootstrap;
import distributed.server.Servers;

import metal.MetalTools;
import metal.Test4;
import metal2.Segmenter.ComponentInfo;

import realmetal.Images;

public class Test0 {
  
  public static interface Comp<T> extends Comparator<T>, Serializable {
  }
  
  public static final Comparator<short[]> POINT_COMP = new Comp<short[]>() {
    private static final long serialVersionUID = 1L;
    
    public int compare(short[] a, short[] b) {
      int d = a[0] - b[0];
      if (d != 0)
        return d;
      
      return a[1] - b[1];
    }
  };
  
  public static short[][] dilate(short[][] points) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    
    for (short[] point : points) {
      minX = Math.min(minX, point[0]);
      minY = Math.min(minY, point[1]);
      
      maxX = Math.max(maxX, point[0]);
      maxY = Math.max(maxY, point[1]);
    }
    
    --minX;
    --minY;
    
    ArrayList<short[]> result = new ArrayList<short[]>();
    
    boolean[][] mask = new boolean[maxY - minY + 2][maxX - minX + 2];
    for (short[] point : points)
      mask[point[1] - minY][point[0] - minX] = true;
    
    final int[][] neighbours = new int[][] { {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    
    outer: for (short[] point : points) {
      int x = point[0] - minX;
      int y = point[1] - minY;
      
      for (int k = 0; k != 4; ++k) {
        int xx = x + neighbours[k][0];
        int yy = y + neighbours[k][1];
        
        if (!(xx >= 0 && yy >= 0 && xx < mask[0].length && yy < mask.length && mask[yy][xx]))
          continue outer;
      }
      
      result.add(point);
    }
    
    return result.toArray(new short[][] {});
  }
  
  public static short[][] getSpine(short[][] points) {
    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    
    for (short[] point : points) {
      minX = Math.min(minX, point[0]);
      minY = Math.min(minY, point[1]);
      
      maxX = Math.max(maxX, point[0]);
      maxY = Math.max(maxY, point[1]);
    }
    
    --minX;
    --minY;
    
    boolean[][] mask = new boolean[maxY - minY + 2][maxX - minX + 2];
    for (short[] point : points)
      mask[point[1] - minY][point[0] - minX] = true;
    
    int[][] distance = new int[maxY - minY + 2][maxX - minX + 2];
    
    ArrayList<short[]> pointArray = new ArrayList<short[]>();
    pointArray.addAll(Arrays.asList(points));
    
    final int[][] neighbours = new int[][] { {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    
    for (int i = 1; !pointArray.isEmpty(); ++i) {
      short[][] ps = pointArray.toArray(new short[][] {});
      pointArray.clear();
      
      boolean[][] maskCopy = new boolean[maxY - minY + 2][maxX - minX + 2];
      for (int y = 0; y != maskCopy.length; ++y)
        for (int x = 0; x != maskCopy[0].length; ++x)
          maskCopy[y][x] = mask[y][x];
      
      loop: for (short[] point : ps) {
        int x = point[0] - minX;
        int y = point[1] - minY;
        
        for (int k = 0; k != 4; ++k) {
          int xx = x + neighbours[k][0];
          int yy = y + neighbours[k][1];
          
          if (!maskCopy[yy][xx]) {
            mask[y][x] = false;
            distance[y][x] = i;
            continue loop;
          }
        }
        
        pointArray.add(point);
      }
    }
    
    pointArray.clear();
    
    loop: for (short[] point : points) {
      int x = point[0] - minX;
      int y = point[1] - minY;
      
      for (int k = 0; k != 4; ++k) {
        int xx = x + neighbours[k][0];
        int yy = y + neighbours[k][1];
        
        if (distance[yy][xx] > distance[y][x])
          continue loop;
      }
      
      pointArray.add(point);
    }
    
    return pointArray.toArray(new short[][] {});
  }
  
  public static void print(Collection<int[]> points) {
    TreeSet<Pair<Integer, Integer>> set = new TreeSet<Pair<Integer, Integer>>();
    for (int[] point : points)
      set.add(new Pair<Integer, Integer>(point[0], point[1]));
    System.err.println(set);
  }
  
  public static double mean(double[][] image, short[][] points) {
    double sum = 0;
    for (short[] point : points)
      sum += image[point[1]][point[0]];
    return sum / points.length;
  }
  
  public static int getPoints(ComponentInfo info, short[][] output, int offset) {
    for (ComponentInfo sub : info.sub)
      offset = getPoints(sub, output, offset);
    
    for (short[] point : info.component) {
      output[offset][0] = point[0];
      output[offset][1] = point[1];
      ++offset;
    }
    return offset;
  }
  
  public static short[][] getPoints(ComponentInfo info) {
    short[][] result = new short[info.size][2];
    if (getPoints(info, result, 0) != result.length)
      throw new RuntimeException("component size mismatches actual size");
    
    return result;
  }
  
  public static int getNumComponents(ComponentInfo info) {
    int num = 1;
    for (ComponentInfo sub : info.sub)
      num += getNumComponents(sub);
    return num;
  }
  
  public static int getDepth(ComponentInfo info, int[] stages, int[] depths) {
    int depth = 0;
    for (ComponentInfo sub : info.sub)
      depth = Math.max(depth, getDepth(sub, stages, depths) + 1);
    for (int i = stages.length - 1; i != -1; --i) {
      if (info.size > stages[i])
        break;
      
      depths[i] = Math.max(depths[i], depth);
    }
    
    return depth;
  }
  
  public static void collectNodes(ArrayList<ComponentInfo> components, ComponentInfo component,
      int maxComponentSize, double minThreshold) {
    if (component.size <= maxComponentSize && component.threshold >= minThreshold)
      components.add(component);
    else
      for (ComponentInfo sub : component.sub)
        collectNodes(components, sub, maxComponentSize, minThreshold);
  }
  
  public static ComponentInfo[] splitTopLevels(ComponentInfo topLevel, int maxComponentSize, double minThreshold) {
    ArrayList<ComponentInfo> components = new ArrayList<ComponentInfo>();
    collectNodes(components, topLevel, maxComponentSize, minThreshold);
    return components.toArray(new ComponentInfo[] {});
  }
  
  public static BufferedImage getColorClone(BufferedImage image) {
    BufferedImage output =
        new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y != output.getHeight(); ++y)
      for (int x = 0; x != output.getWidth(); ++x)
        output.setRGB(x, y, image.getRGB(x, y) & 0x00ffffff);
    return output;
  }
  
  public static BufferedImage markComponents(BufferedImage image, ComponentInfo[] infos) {
    BufferedImage result = getColorClone(image);
    for (ComponentInfo component : infos) {
      int color = (int) (Math.random() * (1 << 24));
      for (short[] point : getPoints(component))
        result.setRGB(point[0], point[1], color);
    }
    return result;
  }
  
  public static boolean computeGreenStuff(ArrayList<ComponentInfo> result,
      TreeMap<short[], Double> ratios, ComponentInfo info, double minRatio) {
    if (info.size < 10)
      return false;
    
    ArrayList<ComponentInfo> list = new ArrayList<ComponentInfo>();
    boolean allGreen = true;
    for (ComponentInfo sub : info.sub)
      if (sub.size >= 10)
        if (computeGreenStuff(result, ratios, sub, minRatio))
          list.add(sub);
        else
          allGreen = false;
    
    if (allGreen && ratios.get(info.component.get(0)) >= minRatio)
      return true;
    
    result.addAll(list);
    return false;
  }
  
  public static ComponentInfo[] computeGreenStuff(TreeMap<short[], Double> ratios,
      ComponentInfo[] infos, double minRatio) {
    ArrayList<ComponentInfo> result = new ArrayList<ComponentInfo>();
    int counter = 0;
    for (ComponentInfo info : infos) {
      System.err.println(++counter);
      if (computeGreenStuff(result, ratios, info, minRatio))
        result.add(info);
    }
    return result.toArray(new ComponentInfo[] {});
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    
    ComponentInfo info = Streams.readObject("segment-result-4");
    
    BufferedImage image = Images.getGreenImage(0, 23);
    
    //images.add(image);
    
    ComponentInfo[] infos = splitTopLevels(info, 2000, 0.85);
    
    //images.add(markComponents(image, infos));
    
    double[][] image0 = ImageOpsDouble.fromFloat(MetalTools.getImageValues(0, 23));
    double[][] image1 = Images.getImageComponent(0, 23, 0);
    
    //ComponentInfo[] greenComps = computeGreenStuff(ratios, infos, 1.05);
    //images.add(markComponents(image, greenComps));
    
    //images.add(MetalTools.scale(MetalTools.toImage(image1, 128), 2));
    
    Test4.showImages(images);
  }
}
