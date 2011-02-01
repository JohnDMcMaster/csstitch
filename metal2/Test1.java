package metal2;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import metal.MetalTools;
import metal.Test4;

import realmetal.Decomp;
import realmetal.Images;

import distributed.Bootstrap;
import distributed.server.Servers;

public class Test1 {
  
  public static double[][][] getGreenDecomp(int stitch, int image, int minSegmentLength)
      throws IOException {
    double[][][] comp1 = Decomp.computeComponents(stitch, image, 1, minSegmentLength);
    double[][][] comp2 = Decomp.computeComponents(stitch, image, 2, minSegmentLength);
    
    int sx = 2 * comp1[0][0].length;
    int sy = 2 * comp1[0].length;
    
    double[][][] comp = new double[2][sy][sx];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if (x % 2 == 1 && y % 2 == 0) {
          comp[0][y][x] = comp1[0][y / 2][x / 2];
          comp[1][y][x] = comp1[1][y / 2][x / 2];
        }
        
        if (x % 2 == 0 && y % 2 == 1) {
          comp[0][y][x] = comp2[0][y / 2][x / 2];
          comp[1][y][x] = comp2[1][y / 2][x / 2];
        }
        
        if (x % 2 == 0 && y % 2 == 0) {
          if (x == 0)
            comp[0][y][x] = comp1[0][y / 2][x / 2];
          else
            comp[0][y][x] = (comp1[0][y / 2][x / 2 - 1] + comp1[0][y / 2][x / 2]) / 2;
          
          if (y == 0)
            comp[1][y][x] = comp2[1][y / 2][x / 2];
          else
            comp[1][y][x] = (comp2[1][y / 2 - 1][x / 2] + comp2[1][y / 2][x / 2]) / 2;
        }
        
        if (x % 2 == 1 && y % 2 == 1) {
          if (x == sx - 1)
            comp[0][y][x] = comp2[0][y / 2][x / 2];
          else
            comp[0][y][x] = (comp2[0][y / 2][x / 2] + comp2[0][y / 2][x / 2 + 1]) / 2;
          
          if (y == sy - 1)
            comp[1][y][x] = comp1[1][y / 2][x / 2];
          else
            comp[1][y][x] = (comp1[1][y / 2][x / 2] + comp1[1][y / 2 + 1][x / 2]) / 2;
        }
      }
    
    return comp;
  }
  
  public static boolean[][][] markLocalMinimums(double[][][] comp) {
    int sx = comp[0][0].length;
    int sy = comp[0].length;
    
    boolean[][][] result = new boolean[2][sy][sx];
    
    for (int x = 0; x != sx; ++x)
      for (int y = 1; y != sy - 1; ++y)
        if (!(comp[0][y - 1][x] < comp[0][y][x]) && comp[0][y][x] < comp[0][y + 1][x])
          result[0][y][x] = true;
    
    for (int y = 0; y != sy; ++y)
      for (int x = 1; x != sx - 1; ++x)
        if (!(comp[1][y][x - 1] < comp[1][y][x]) && comp[1][y][x] < comp[1][y][x + 1])
          result[1][y][x] = true;
    
    return result;
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    
    final int stitch = 0;
    final int image = 23;
    final int minSegmentLength = 8;
    final double threshold = -0.14;
    
    double[][] img1 = Images.getImageComponent(stitch, image, 1);
    double[][] img2 = Images.getImageComponent(stitch, image, 2);
    double[][] img = Images.interpolate(img1, img2);
    
    double[][][] comp = getGreenDecomp(stitch, image, minSegmentLength);
    boolean[][][] max = markLocalMinimums(comp);
    
    boolean[][][] bin = new boolean[2][][];
    bin[0] = MetalTools.binarize(comp[0], threshold, false);
    bin[1] = MetalTools.binarize(comp[1], threshold, false);
    
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    images.add(Images.getGreenImage(0, 23));
    //images.add(MetalTools.toImage(MetalTools.binarize(image, 1.1, false)));
    //images.add(MetalTools.toImage(MetalTools.intersection(max[0], bin[0])));
    //images.add(MetalTools.toImage(MetalTools.intersection(max[1], bin[1])));
    images.add(MetalTools.toImage(MetalTools.union(MetalTools.intersection(max[0], bin[0]),
        MetalTools.intersection(max[1], bin[1]))));
    
    Test4.showImages(images);
  }
}
