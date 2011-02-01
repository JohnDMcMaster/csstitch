package metal;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.lang.reflect.Array;

import old.storage.Point;

public class MetalTools {
  
  public static float[][] getImageValues(int stitch, int image) throws IOException {
    Point[][] array = Selector.select(stitch, image);
    
    int sx = array[0].length;
    int sy = array.length;
    
    float[][] combined = new float[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        combined[y][x] = array[y][x] == null ? 0 : array[y][x].val;
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if (combined[y][x] == 0)
          combined[y][x] =
              (combined[y - 1][x - 1] + combined[y - 1][x + 1] + combined[y + 1][x - 1] + combined[y + 1][x + 1]) / 4;
    
    return combined;
  }
  
  public static float[][] select(float[][] image, int component) {
    float[][] result = new float[image.length / 2][image[0].length / 2];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x)
        result[y][x] = image[2 * y + (component / 2)][2 * x + (component % 2)];
    return result;
  }
  
  public static double[][] select(double[][] image, int component) {
    double[][] result = new double[image.length / 2][image[0].length / 2];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x)
        result[y][x] = image[2 * y + (component / 2)][2 * x + (component % 2)];
    return result;
  }
  
  public static BufferedImage selectGreen(BufferedImage image) {
    WritableRaster imageRaster = image.getRaster();
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    BufferedImage result = new BufferedImage((sx + sy) / 2, (sx + sy) / 2, image.getType());
    WritableRaster raster = result.getRaster();
    Object pixel = imageRaster.getDataElements(0, 0, null);
    for (int y = 0; y != sy; ++y)
      for (int x = (y + 1) % 2; x < sx; x += 2)
        raster.setDataElements((x + y - 1) / 2, (x - y + sy - 1) / 2,
            imageRaster.getDataElements(x, y, pixel));
    
    return result;
  }
  
  public static <T> T[][] selectGreen(T[][] image) {
    int sx = image[0].length;
    int sy = image.length;
    
    @SuppressWarnings("unchecked")
    T[][] result = (T[][]) Array.newInstance(image[0][0].getClass(), (sx + sy) / 2, (sx + sy) / 2);
    for (int y = 0; y != sy; ++y)
      for (int x = (y + 1) % 2; x < sx; x += 2)
        result[(x - y + sy - 1) / 2][(x + y - 1) / 2] = image[y][x];
    
    return result;
  }
  
  public static BufferedImage scale(BufferedImage image, int factor) {
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    BufferedImage result = new BufferedImage(factor * sx, factor * sy, image.getType());
    WritableRaster resultRaster = result.getRaster();
    
    int[] pixel = new int[3];
    
    WritableRaster raster = image.getRaster();
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        for (int i = 0; i != factor; ++i)
          for (int j = 0; j != factor; ++j)
            resultRaster.setPixel(factor * x + i, factor * y + j, pixel);
      }
    
    return result;
  }
  
  public static float[][] fromImage(BufferedImage image, float scale) {
    float[][] result = new float[image.getHeight()][image.getWidth()];
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result.length; ++x) {
        raster.getPixel(x, y, pixel);
        result[y][x] = (pixel[0] + 0.5f) / scale;
      }
    return result;
  }
  
  public static double[][] fromImage(BufferedImage image, double scale) {
    double[][] result = new double[image.getHeight()][image.getWidth()];
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result.length; ++x) {
        raster.getPixel(x, y, pixel);
        result[y][x] = (pixel[0] + 0.5) / scale;
      }
    return result;
  }
  
  public static int[][] fromImage(BufferedImage image) {
    int[][] result = new int[image.getHeight()][image.getWidth()];
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result.length; ++x) {
        raster.getPixel(x, y, pixel);
        result[y][x] = pixel[0];
      }
    return result;
  }
  
  public static BufferedImage toImage(float[][] image, float scale) {
    BufferedImage result =
        new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x) {
        float val = image[y][x] * scale;
        pixel[0] = val >= 0x0100 ? 0xff : (int) val;
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(double[][] image, double scale) {
    BufferedImage result =
        new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x) {
        double val = image[y][x] * scale;
        pixel[0] = val >= 0x0100 ? 0xff : (int) val;
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(float[][][] images, float[] scales) {
    BufferedImage result =
        new BufferedImage(images[0][0].length, images[0].length, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[3];
    for (int y = 0; y != images[0].length; ++y)
      for (int x = 0; x != images[0][0].length; ++x) {
        for (int i = 0; i != 3; ++i) {
          double val = images[i][y][x] * scales[i];
          pixel[i] = val >= 0x0100 ? 0xff : (int) val;
        }
        
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(double[][][] images, double[] scales) {
    BufferedImage result =
        new BufferedImage(images[0][0].length, images[0].length, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[3];
    for (int y = 0; y != images[0].length; ++y)
      for (int x = 0; x != images[0][0].length; ++x) {
        for (int i = 0; i != 3; ++i) {
          double val = images[i][y][x] * scales[i];
          pixel[i] = val >= 0x0100 ? 0xff : (int) val;
        }
        
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(int[][] image, double scale) {
    BufferedImage result =
        new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x) {
        double val = image[y][x] * scale;
        pixel[0] = val >= 0x0100 ? 0xff : (int) val;
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(short[][] image, double scale) {
    BufferedImage result =
        new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x) {
        double val = image[y][x] * scale;
        pixel[0] = val >= 0x0100 ? 0xff : (int) val;
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage toImage(boolean[][] image) {
    BufferedImage result =
        new BufferedImage(image[0].length, image.length, BufferedImage.TYPE_BYTE_BINARY);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[1];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x) {
        pixel[0] = image[y][x] ? 0xff : 0x00;
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static boolean[][] binarize(float[][] image, float threshold, boolean value) {
    int sx = image[0].length;
    int sy = image.length;
    
    boolean[][] result = new boolean[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = (image[y][x] >= threshold) == value;
    
    return result;
  }
  
  public static boolean[][] binarize(double[][] image, double threshold, boolean value) {
    int sx = image[0].length;
    int sy = image.length;
    
    boolean[][] result = new boolean[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = (image[y][x] >= threshold) == value;
    
    return result;
  }
  
  public static boolean[][] union(boolean[][] image0, boolean[][] image1) {
    boolean[][] image = new boolean[image0.length][image0[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        image[y][x] = image0[y][x] || image1[y][x];
    return image;
  }
  
  public static boolean[][] intersection(boolean[][] image0, boolean[][] image1) {
    boolean[][] image = new boolean[image0.length][image0[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        image[y][x] = image0[y][x] && image1[y][x];
    return image;
  }
  
  public static int[] computeBoundaries(int[][][] shapes) {
    int[] result =
        new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
    for (int[][] shape : shapes)
      for (int[] point : shape) {
        result[0] = Math.min(result[0], point[0]);
        result[1] = Math.min(result[1], point[1]);
        result[2] = Math.max(result[2], point[0]);
        result[3] = Math.max(result[3], point[1]);
      }
    
    ++result[2];
    ++result[3];
    return result;
  }
  
  public static boolean[][] render(int[][] positions, int[] boundaries) {
    boolean[][] result = new boolean[boundaries[3] - boundaries[1]][boundaries[2] - boundaries[0]];
    for (int a = 0; a != positions.length; ++a)
      result[positions[a][1] - boundaries[1]][positions[a][0] - boundaries[0]] = true;
    
    return result;
  }
  
  public static float getNormSq(float x, float y) {
    return x * x + y * y;
  }
  
  public static float getNormSq(float[] v) {
    return getNormSq(v[0], v[1]);
  }
  
  public static double getAngle(float[] v0, float[] v1) {
    return Math.acos((v0[0] * v1[0] + v0[1] * v1[1]) / Math.sqrt(getNormSq(v0) * getNormSq(v1)));
  }
  
}
