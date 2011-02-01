package confocal;

import general.Streams;
import general.execution.Bash;

import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import cache.Cache;

import data.Tools;

import metal.MetalTools;

import tools.LinearEquation;
import tools.SumOfSquares;

public class Test {
  
  public static double[][] loadDouble(String in) throws IOException {
    short[][] a = TiffReader.readLeicaTiff(in);
    double[][] r = new double[a.length][a[0].length];
    for (int y = 0; y != r.length; ++y)
      for (int x = 0; x != r[0].length; ++x)
        r[y][x] = a[y][x];
    return r;
  }
  
  public static void convertStack(String in, String out, int sz) throws IOException {
    short[][][] stack = new short[sz][][];
    for (int z = 0; z != sz; ++z)
      stack[z] = TiffReader.readLeicaTiff(String.format(in, z));
    Streams.writeObject(out, stack);
  }
  
  public static BufferedImage[] viewStack(short[][][] stack) {
    BufferedImage[] images = new BufferedImage[stack.length];
    for (int z = 0; z != stack.length; ++z)
      images[z] = MetalTools.toImage(stack[z], 0.25);
    
    return images;
  }
  
  public static BufferedImage[] viewStack(float[][][] stack, float limit) {
    BufferedImage[] images = new BufferedImage[stack.length];
    for (int z = 0; z != stack.length; ++z)
      images[z] = MetalTools.toImage(stack[z], 256 / limit);
    
    return images;
  }
  
  public static float getMax(float[][][] stack) {
    float max = 0;
    for (float[][] plane : stack)
      for (float[] row : plane)
        for (float val : row)
          if (val > max)
            max = val;
    
    return max;
  }
  
  public static BufferedImage[] viewStack(float[][][] stack) {
    return viewStack(stack, getMax(stack));
  }
  
  public static BufferedImage[] viewStacksColor(float[][][][] stacks, float[] limits) {
    BufferedImage[] images = new BufferedImage[stacks[0].length];
    for (int z = 0; z != stacks[0].length; ++z) {
      float[][][] planes = new float[stacks.length][][];
      for (int i = 0; i != stacks.length; ++i)
        planes[i] = stacks[i][z];
      
      float[] scales = new float[stacks.length];
      for (int i = 0; i != stacks.length; ++i)
        scales[i] = 256 / limits[i];
      
      images[z] = MetalTools.toImage(planes, scales);
    }
    
    return images;
  }
  
  public static BufferedImage[] viewStacksColor(float[][][]... stacks) {
    float[] limits = new float[stacks.length];
    for (int i = 0; i != stacks.length; ++i)
      limits[i] = getMax(stacks[i]);
    
    return viewStacksColor(stacks, limits);
  }
  
  public static void saveImages(String filename, BufferedImage[] images) throws IOException {
    new File(filename.substring(0, filename.lastIndexOf('/'))).mkdirs();
    for (int i = 0; i != images.length; ++i)
      ImageIO.write(images[i], "png", new File(String.format(filename, i)));
  }
  
  public static short[][][] loadStack(String name) throws IOException {
    ArrayList<short[][]> stack = new ArrayList<short[][]>();
    for (int i = 0;; ++i) {
      String s = String.format(name, i);
      if (!new File(s).exists())
        break;
      
      stack.add(TiffReader.readLeicaTiff(s));
    }
    
    return stack.toArray(new short[][][] {});
  }
  
  public static double[] estimateSkew(short[][][] stack, int threshold) {
    int sx = stack[0][0].length;
    int sy = stack[0].length;
    int sz = stack.length;
    
    double[][] a = new double[3][3];
    double[] c = new double[3];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int maxZ = 0;
        for (int z = 0; z != sz; ++z)
          if (stack[z][y][x] > stack[maxZ][y][x])
            maxZ = z;
        
        if (stack[maxZ][y][x] < threshold)
          continue;
        
        int xx = 2 * x - (sx - 1);
        int yy = 2 * y - (sy - 1);
        
        a[0][0] += xx * xx;
        a[0][1] += xx * yy;
        a[0][2] += xx;
        
        a[1][0] += yy * xx;
        a[1][1] += yy * yy;
        a[1][2] += yy;
        
        a[2][0] += xx;
        a[2][1] += yy;
        a[2][2] += 1;
        
        c[0] += xx * maxZ;
        c[1] += yy * maxZ;
        c[2] += maxZ;
      }
    
    return LinearEquation.solveLinearEquation(a, c);
  }
  
  public static float[][][] levelStack(short[][][] s, float[] skew) throws IOException {
    int sx = s[0][0].length;
    int sy = s[0].length;
    int sz = s.length;
    
    float[][][] result = new float[sz][sy][sx];
    for (int z = 0; z != sz; ++z)
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x) {
          float newZ = z + skew[0] * (2 * x - (sx - 1)) + skew[1] * (2 * y - (sy - 1));
          if (newZ >= 0 && newZ < sz - 1) {
            int u = (int) newZ;
            int v = u + 1;
            float du = newZ - u;
            float dv = 1 - du;
            result[z][y][x] = dv * (s[u][y][x] + 0.5f) + du * (s[v][y][x] + 0.5f);
          }
        }
    
    return result;
  }
  
  public static float[][][][] loadLevelledStacks(String name, int threshold) throws IOException {
    float[][][][] result = Cache.cache("levelled-stack/%s/%d", name, threshold);
    if (result != null)
      return result;
    
    int[] channels = {0, 1, 2};
    
    short[][][][] stacks = new short[channels.length][][][];
    double[][] skews = new double[channels.length][];
    for (int i = 0; i != channels.length; ++i) {
      stacks[i] = loadStack(String.format(name, i));
      skews[i] = estimateSkew(stacks[i], threshold);
      
      System.err.println("skew: ");
      for (int j = 0; j != skews[i].length; ++j)
        System.err.println(skews[i][j] + ", ");
      System.err.println();
    }
    
    float[] skew = new float[skews[0].length];
    for (int j = 0; j != skew.length; ++j) {
      for (int i = 0; i != channels.length; ++i)
        skew[j] += skews[i][j];
      
      skew[j] /= skew.length;
    }
    
    result = new float[channels.length][][][];
    for (int i = 0; i != channels.length; ++i)
      result[i] = levelStack(stacks[i], skew);
    
    return result;
  }
  
  public static void produceMovie(String dir, BufferedImage[] images) throws IOException {
    for (int i = 0; i != images.length; ++i) {
      ImageIO.write(images[i], "png", new File(dir + "/image-" + i + ".png"));
      Bash.command("convert -quality 100 image-" + i + ".png image-" + i + ".jpg", dir).executeChecked();
      new File(dir + "/image-" + i + ".png").delete();
    }
  }
  
  public static float[][][] divide(float[][][] a, float[][][] b) {
    int sx = a[0][0].length;
    int sy = a[0].length;
    int sz = a.length;
    
    float[][][] r = new float[sz][sy][sx];
    
    for (int z = 0; z != sz; ++z)
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          if (b[z][y][x] != 0)
            r[z][y][x] = a[z][y][x] / b[z][y][x];
    
    return r;
  }
  
  public static void main(String[] args) throws IOException {
    float[][][][] stacks =
        loadLevelledStacks("/home/noname/di/confocal2/111022_3rd Chip_20x_zoom1_z%%03d_ch%02d.tif", 512);
    BufferedImage[] images = viewStacksColor(stacks);
    Tools.displayImages(images);
    produceMovie("/home/noname/di/confocal2/movie2", images);
  }
}
