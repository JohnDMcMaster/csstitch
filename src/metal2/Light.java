package metal2;

import general.Streams;
import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import metal.MetalTools;
import metal.Test4;

import operations.image.ImageOpsDouble;

import data.DataTools;
import data.Tools;
import distributed.Bootstrap;
import distributed.server.CipServer;
import distributed.server.Servers;

public class Light {
  
  private static final double RADIUS = 200;
  private static final int SCALE = 1024;
  
  public static void show(double[][] image) throws IOException {
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    for (int i = 0; i != 100; ++i)
      images.add(MetalTools.toImage(MetalTools.binarize(image, 0.5 + 0.01 * i, true)));
    Test4.showImages(images);
  }
  
  public static void filter(double[][] factors) {
    int sx = factors[0].length;
    int sy = factors.length;
    
    for (int s = 100; s != sx / 2; ++s)
      for (int y = 0; y != sy; ++y) {
        if (factors[y][sx / 2 - 1 - s] > factors[y][sx / 2 - 1 - (s - 1)])
          factors[y][sx / 2 - 1 - s] = factors[y][sx / 2 - 1 - (s - 1)];
        
        if (factors[y][sx / 2 + s] > factors[y][sx / 2 + (s - 1)])
          factors[y][sx / 2 + s] = factors[y][sx / 2 + (s - 1)];
      }
    
    for (int s = 100; s != sy / 2; ++s)
      for (int x = 0; x != sx; ++x) {
        if (factors[sy / 2 - 1 - s][x] > factors[sy / 2 - 1 - (s - 1)][x])
          factors[sy / 2 - 1 - s][x] = factors[sx / 2 - 1 - (s - 1)][x];
        
        if (factors[sy / 2 + s][x] > factors[sy / 2 + (s - 1)][x])
          factors[sy / 2 + s][x] = factors[sy / 2 + (s - 1)][x];
      }
  }
  
  public static double[][] getImage(int u, int v, int component) throws IOException {
    return Tools.getMatrixFromImage("data/P" + (6114199 + 5 * v + u) + "/" + component + ".png");
  }
  
  public static int[][][] produceChangeSets(double radius) {
    @SuppressWarnings("unchecked")
    TreeSet<Pair<Integer, Integer>>[] set = new TreeSet[2];
    for (int i = 0; i != 2; ++i)
      set[i] = new TreeSet<Pair<Integer, Integer>>();
    
    for (int y = -(int) (radius + 2); y <= radius + 2; ++y)
      for (int x = -(int) (radius + 2); x <= radius + 2; ++x) {
        boolean a = y * y + x * x < radius * radius;
        boolean b = y * y + (x - 1) * (x - 1) < radius * radius;
        if (a && !b)
          set[0].add(new Pair<Integer, Integer>(x, y));
        if (b && !a)
          set[1].add(new Pair<Integer, Integer>(x, y));
      }
    
    int[][][] result = new int[2][][];
    for (int i = 0; i != 2; ++i) {
      result[i] = new int[set[i].size()][2];
      Iterator<Pair<Integer, Integer>> it = set[i].iterator();
      for (int j = 0; j != result[i].length; ++j) {
        Pair<Integer, Integer> pair = it.next();
        result[i][j][0] = pair.getA();
        result[i][j][1] = pair.getB();
      }
    }
    
    return result;
  }
  
  public static double evaluate(int[] histogram) {
    int a = 0;
    for (int i = 0; i != histogram.length; ++i)
      if (histogram[i] > histogram[a])
        a = i;
    
    int threshold = (3 * histogram[a]) / 4;
    
    int sum = 0;
    int weight = 0;
    for (int i = 0; i != histogram.length; ++i)
      if (histogram[i] >= threshold) {
        sum += i * histogram[i];
        weight += histogram[i];
      }
    
    return (double) sum / weight + 0.5;
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    //Bootstrap.bootstrap(new CipServer("cip78"));
    System.err.println(Servers.HOSTNAME + ": " + SCALE + ", " + RADIUS);
    
    /*{
      String path = "light/3-4/1/25";
      double[][] image = MetalTools.select(getImage(3, 4), 1);
      double[][] factors = Streams.readObject(path);
      int sx = 1625;
      int sy = 1225;
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          image[y][x] /= factors[y][x];
      ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
      images.add(MetalTools.toImage(Images.getImageComponent(0, 3, 4, 1), 64));
      images.add(MetalTools.toImage(image, 128));
      Test4.showImages(images);
      System.exit(0);
    }*/

    final int u = 3;
    final int v = 3;
    final int component = 1;
    
    double[][] image = getImage(u, v, component);
    
    int sx = image[0].length;
    int sy = image.length;
    
    int[][][] changeSets = produceChangeSets(RADIUS);
    
    double[][] factors = new double[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        factors[y][x] = 1024;
    
    double[][] result = new double[sy][sx];
    
    for (int k = 0;; ++k) {
      int[] hist = new int[8 * SCALE];
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          ++hist[(int) (SCALE * (image[y][x] / factors[y][x]))];
      
      double normalizer = evaluate(hist) / SCALE;
      System.err.println("normalizer: " + normalizer);
      
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          factors[y][x] *= normalizer;
      
      double[][] matrix = ImageOpsDouble.copy(image);
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          matrix[y][x] = image[y][x] / factors[y][x];
      
      result = ImageOpsDouble.copy(matrix);
      
      double change = 0;
      
      int numViolations = 0;
      
      int[] histogramStarter = new int[4 * SCALE];
      for (int yy = 0; yy <= RADIUS + 2; ++yy)
        for (int xx = 0; xx <= RADIUS + 2; ++xx) {
          if (xx * xx + yy * yy < RADIUS * RADIUS) {
            int value = (int) (SCALE * matrix[yy][xx]);
            if (value < histogramStarter.length)
              ++histogramStarter[value];
            else
              ++numViolations;
          }
        }
      
      for (int y = 0; y != sy; ++y) {
        if (y % 100 == 0)
          System.err.println(y);
        
        int[] histogram = histogramStarter.clone();
        
        for (int x = 0; x != sx; ++x) {
          double res = evaluate(histogram) / SCALE;
          factors[y][x] *= res;
          
          change += Math.abs(res - 1);
          
          for (int i = 0; i != 2; ++i)
            for (int[] point : changeSets[i]) {
              int xx = x + point[0];
              int yy = y + point[1];
              if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
                int value = (int) (SCALE * matrix[yy][xx]);
                if (value < histogram.length)
                  histogram[value] += 2 * i - 1;
                else
                  ++numViolations;
              }
            }
        }
        
        for (int i = 0; i != 2; ++i)
          for (int[] point : changeSets[i]) {
            int xx = point[1];
            int yy = y + point[0];
            if (xx >= 0 && yy >= 0 && sx < sx && yy < sy) {
              int value = (int) (SCALE * matrix[yy][xx]);
              if (value < histogramStarter.length)
                histogramStarter[value] += 2 * i - 1;
              else
                ++numViolations;
            }
          }
      }
      
      System.err.println(numViolations);
      
      if (change < 12000)
        show(result);
      
      filter(factors);
      
      String path = "light/" + u + "-" + v + "/" + component + "/" + k;
      Tools.ensurePath(path);
      Streams.writeObject(path, factors);
      
      System.err.println(change);
    }
  }
}
