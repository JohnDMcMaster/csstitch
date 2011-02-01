package metal;

import general.Streams;
import general.collections.ComparableCollection;
import general.collections.Pair;
import general.execution.SSHAddress;


import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import realmetal.BinaryImage;
import realmetal.Vias;

import data.Tools;
import distributed.Bootstrap;
import distributed.server.AbstractServer;
import distributed.server.Server;
import distributed.server.Servers;

public class ViaCircles2 {
  
  private static final float REAL_DIST = 3f;
  
  private static final int image = 23;
  
  private static float[][] img;
  private static int sx, sy;
  
  private static TreeSet<Pair<Integer, Integer>> realVias;
  
  public static int getMinNumPositions(int[][][] shapes) {
    int minNumPositions = Integer.MAX_VALUE;
    for (int[][] shape : shapes)
      minNumPositions = Math.min(minNumPositions, shape.length);
    return minNumPositions;
  }
  
  public static int[][] getPositions(int[][][] shapes) {
    TreeSet<Pair<Integer, Integer>> positions = new TreeSet<Pair<Integer, Integer>>();
    for (int[][] shape : shapes)
      for (int[] point : shape)
        positions.add(new Pair<Integer, Integer>(point[0], point[1]));
    System.out.println("num positions: " + positions.size());
    
    int[][] pos = new int[positions.size()][2];
    int index = 0;
    for (Pair<Integer, Integer> point : positions) {
      pos[index][0] = point.getA();
      pos[index][1] = point.getB();
      ++index;
    }
    
    return pos;
  }
  
  public static BinaryImage[] getBinaryImages(int[] size, int[][][] shapes) {
    BinaryImage[] images = new BinaryImage[shapes.length];
    for (int i = 0; i != images.length; ++i)
      images[i] = new BinaryImage(MetalTools.render(shapes[i], new int[] {0, 0, size[0], size[1]}));
    return images;
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(new Server[] {new AbstractServer(new SSHAddress("localhost", 2222,
        "mdd63bj"), Servers.CIP_90.getDir())});
    System.err.println("started");
    
    img = MetalTools.getImageValues(0, image);
    sx = img[0].length;
    sy = img.length;
    
    realVias = Vias.clipSelectedVias(image, sx, sy, 16);
    
    Object[] data = Streams.readObject("circle-data");
    int[] size = (int[]) data[1];
    
    int[][][] shapes = (int[][][]) data[2];
    int minNumPositions = getMinNumPositions(shapes);
    int[][] positions = getPositions(shapes);
    
    float[][] centers = (float[][]) data[3];
    
    BinaryImage[] masks = getBinaryImages(size, shapes);
    
    BinaryImage mask = new BinaryImage(MetalTools.binarize(img, 0.8f, false));
    
    TreeSet<Pair<Integer, Integer>> viasHit = new TreeSet<Pair<Integer, Integer>>();
    int numViaHits = 0;
    int numTotalHits = 0;
    
    int[][] vias = new int[sy][sx];
    for (int y = 0; y != sy - size[1]; ++y) {
      System.err.println(y);
      for (int x = y % 2; x < sx - size[0]; x += 2) {
        int numPoints = 0;
        for (int[] pos : positions)
          if (mask.get(x + pos[0], y + pos[1]))
            ++numPoints;
        
        if (!(numPoints >= minNumPositions))
          continue;
        
        for (int i = 0; i != masks.length; ++i)
          if (mask.contains(masks[i], x, y)) {
            ++numTotalHits;
            float cx = x + (int) Math.round(centers[i][0]);
            float cy = y + (int) Math.round(centers[i][1]);
            
            for (Pair<Integer, Integer> via : realVias)
              if (MetalTools.getNormSq(via.getA() - cx, via.getB() - cy) < REAL_DIST * REAL_DIST) {
                viasHit.add(via);
                ++numViaHits;
              }
            //++vias[y + (int) Math.round(centers[i][1])][x + (int) Math.round(centers[i][0])];
          }
      }
    }
    
    System.out.println(viasHit.size() + " / " + realVias.size());
    System.out.println(numViaHits + " / " + numTotalHits);
    
    //BufferedImage result = MetalTools.toImage(vias, 64 * 255. / max);
    //Tools.writePNG(result, "result.png");
  }
  
}
