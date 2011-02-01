package segment;

import general.collections.Pair;

import interpolation.Bicubic;
import interpolation.Bilinear;
import interpolation.NearestNeighbour;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.DataOutput;
import java.io.IOException;
import java.util.TreeMap;

import map.Map;
import map.properties.ImageSize;
import map.properties.StitchStackProperties;

import cache.Cache;
import realmetal.BinaryImage;
import realmetal.Images;

public class Sharpness {
  
  public interface ContinuousImage {
    public double getBoundary();
    
    public double getValue(double x, double y);
  }
  
  public interface Interpolator {
    public ContinuousImage getContinuousImage(double[][] image);
  }
  
  public static Interpolator NEAREST_NEIGHBOUR = new Interpolator() {
    public ContinuousImage getContinuousImage(double[][] image) {
      return new NearestNeighbour(image);
    }
  };
  
  public static Interpolator BILINEAR = new Interpolator() {
    public ContinuousImage getContinuousImage(double[][] image) {
      return new Bilinear(image);
    }
  };
  
  public static Interpolator BICUBIC_HOMOGENEOUS = new Interpolator() {
    public ContinuousImage getContinuousImage(double[][] image) {
      return new Bicubic(image, -0.5);
    }
  };
  
  public static Interpolator BICUBIC_STANDARD = new Interpolator() {
    public ContinuousImage getContinuousImage(double[][] image) {
      return new Bicubic(image, -0.75);
    }
  };
  
  public static Interpolator BICUBIC_SHARPEN = new Interpolator() {
    public ContinuousImage getContinuousImage(double[][] image) {
      return new Bicubic(image, -1);
    }
  };
  
  public static void getChannel(BufferedImage image, double[][] matrix, int channel) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[4];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        matrix[y][x] = raster.getPixel(x, y, pixel)[channel] + 0.5;
  }
  
  public static void setChannel(BufferedImage image, double[][] matrix, int channel) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[4];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int val = (int) matrix[y][x];
        if (!(val >= 0))
          val = 0;
        else if (!(val < 256))
          val = 255;
        
        raster.getPixel(x, y, pixel)[channel] = val;
        raster.setPixel(x, y, pixel);
      }
  }
  
  public static BufferedImage render(int outScale, StitchStackProperties stack, int[] channels,
      int stitch, BufferedImage[] images, Interpolator interpolator, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    BufferedImage result =
        new BufferedImage(outScale * (stack.getX1() - stack.getX0()), outScale
            * (stack.getY1() - stack.getY0()), images[0].getType());
    
    ImageSize size = stack.getImageSetProperties(stitch).getSize();
    int numImages = stack.getImageSetProperties(stitch).getNumImages();
    
    double[][][] matrices = new double[images.length][size.getSy()][size.getSx()];
    ContinuousImage[] contImages = new ContinuousImage[numImages];
    
    for (int c = 0; c != channels.length; ++c) {
      for (int i = 0; i != numImages; ++i) {
        getChannel(images[i], matrices[i], c);
        contImages[i] = interpolator.getContinuousImage(matrices[i]);
      }
      
      double[][] resultMatrix =
          render(outScale, stack, stitch, channels[c], contImages, inScale, edges);
      setChannel(result, resultMatrix, c);
    }
    
    return result;
  }
  
  public static double[][] render(int outScale, StitchStackProperties stack, int stitch,
      int channel, ContinuousImage[] images, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    double[][] result =
        new double[outScale * (stack.getY1() - stack.getY0())][outScale
            * (stack.getX1() - stack.getX0())];
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(outScale, edges);
    double[] in = new double[2];
    
    for (int y = 0; y != result.length; ++y) {
      System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(outScale * stack.getY0() + y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(Integer.MIN_VALUE, -1);
      }
      
      for (int x = 0; x != result[0].length; ++x) {
        int v = line.floorEntry(outScale * stack.getX0() + x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / outScale;
          in[1] = (y + 0.5) / outScale;
          
          maps[v].unmap(in, in);
          
          result[y][x] = images[v].getValue(in[0] * inScale, in[1] * inScale);
        }
      }
    }
    
    return result;
  }
  
  public static void render(int outScale, StitchStackProperties stack, int stitch, int channel,
      ContinuousImage[] images, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges, DataOutput out)
      throws IOException {
    int sx = outScale * (stack.getX1() - stack.getX0());
    int sy = outScale * (stack.getY1() - stack.getY0());
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(outScale, edges);
    double[] in = new double[2];
    
    for (int y = 0; y != sy; ++y) {
      System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(outScale * stack.getY0() + y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(Integer.MIN_VALUE, -1);
      }
      
      for (int x = 0; x != sx; ++x) {
        int v = line.floorEntry(outScale * stack.getX0() + x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / outScale;
          in[1] = (y + 0.5) / outScale;
          
          maps[v].unmap(in, in);
          
          out.writeDouble(images[v].getValue(in[0] * inScale, in[1] * inScale));
        } else
          out.writeDouble(-1);
      }
    }
  }
  
  public static double[][] render(int scale, StitchStackProperties stack, int stitch, int channel,
      double[][][] images, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    return render(scale, stack, stitch, channel, images, scale, edges);
  }
  
  public static double[][] render(int outScale, StitchStackProperties stack, int stitch,
      int channel, double[][][] images, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    double[][] result =
        new double[outScale * (stack.getY1() - stack.getY0())][outScale
            * (stack.getX1() - stack.getX0())];
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(outScale, edges);
    double[] in = new double[2];
    
    for (int y = 0; y != result.length; ++y) {
      System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != result[0].length; ++x) {
        int v = line.floorEntry(x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / outScale;
          in[1] = (y + 0.5) / outScale;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(inScale * in[0] - 0.5);
          int yy = (int) Math.round(inScale * in[1] - 0.5);
          
          result[y][x] = images[v][yy][xx];
        }
      }
    }
    
    return result;
  }
  
  public static BinaryImage render(int scale, StitchStackProperties stack, int stitch, int channel,
      BinaryImage[] images, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    return render(scale, stack, stitch, channel, images, scale, edges);
  }
  
  public static BinaryImage render(int outScale, StitchStackProperties stack, int stitch,
      int channel, BinaryImage[] images, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    BinaryImage result =
        new BinaryImage(outScale * (stack.getX1() - stack.getX0()), outScale
            * (stack.getY1() - stack.getY0()));
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(outScale, edges);
    double[] in = new double[2];
    
    for (int y = 0; y != result.getSy(); ++y) {
      System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != result.getSx(); ++x) {
        int v = line.floorEntry(x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / outScale;
          in[1] = (y + 0.5) / outScale;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(inScale * in[0] - 0.5);
          int yy = (int) Math.round(inScale * in[1] - 0.5);
          
          result.set(x, y, images[v].get(xx, yy));
        }
      }
    }
    
    return result;
  }
  
  public static BufferedImage render(int scale, StitchStackProperties stack, int stitch,
      int channel, BufferedImage[] images,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    return render(scale, stack, stitch, channel, images, scale, edges);
  }
  
  public static BufferedImage render(int outScale, StitchStackProperties stack, int stitch,
      int channel, BufferedImage[] images, double inScale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    BufferedImage result =
        new BufferedImage(outScale * (stack.getX1() - stack.getX0()), outScale
            * (stack.getY1() - stack.getY0()), images[0].getType());
    WritableRaster raster = result.getRaster();
    
    WritableRaster[] rasters = new WritableRaster[images.length];
    for (int image = 0; image != images.length; ++image)
      rasters[image] = images[image].getRaster();
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors =
        SharpnessEvaluator.computeLineColorMaps(outScale, edges);
    double[] in = new double[2];
    int[] pixel = new int[4];
    
    for (int y = 0; y != result.getHeight(); ++y) {
      System.err.println(y);
      
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != result.getWidth(); ++x) {
        int v = line.floorEntry(x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / outScale;
          in[1] = (y + 0.5) / outScale;
          
          maps[v].unmap(in, in);
          
          int xx = (int) Math.round(inScale * in[0] - 0.5);
          int yy = (int) Math.round(inScale * in[1] - 0.5);
          
          if (xx >= 0 && yy >= 0 && xx < rasters[v].getWidth() && yy < rasters[v].getHeight())
            raster.setPixel(x, y, rasters[v].getPixel(xx, yy, pixel));
        }
      }
    }
    
    return result;
  }
  
  public static double[][][] getSharpness(int stitch, int color) throws IOException {
    double[][][] result = Cache.cache("sharpness/%d/%d", stitch, color);
    if (result != null)
      return result;
    
    return SharpnessEvaluator.computeSharpnessRGGB(Images.getFixedRawComponents(stitch), color,
        SharpnessEvaluator.DEFAULT_SHARPNESS_FILTER_SIGMA);
  }
  
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] computeEdges(
      int stitch, int color, int boundary, boolean interactive) throws IOException {
    Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] result =
        Cache.cache("sharpness-edges/%d/%d/%d", stitch, color, boundary, interactive);
    if (result != null)
      return result;
    
    int[] b = new int[] {boundary + 1};
    
    result =
        SharpnessEvaluator.computeEdges(Images.getStitchStackProperties(), stitch, color + color
            / 2, getSharpness(stitch, color), b,
            SharpnessEvaluator.DEFAULT_SHARPNESS_MASK_SHRINK_GROW_DISTANCE,
            SharpnessEvaluator.DEFAULT_EDGE_SMOOTH_FILTER_SIGMA, false);
    
    if (!(b[0] >= boundary))
      throw new RuntimeException("unexpected boundary shrinkage");
    
    return result;
  }
  
}
