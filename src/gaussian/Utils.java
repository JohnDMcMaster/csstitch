package gaussian;

import general.Streams;
import general.collections.Pair;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import cache.Cache;

import data.DataTools;
import data.Tools;

import map.properties.StitchStackProperties;
import metal.MetalTools;
import operations.image.ImageOpsDouble;
import realmetal.BinaryImage;
import realmetal.Images;
import segment.Segment;
import segment.Sharpness;
import segment.SharpnessEvaluator;

public class Utils {
  
  public static double gaussian(double x) {
    return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI);
  }
  
  public static double gaussianDerivative(double x) {
    return -x * gaussian(x);
  }
  
  public static double gaussianSecondDerivative(double x) {
    return (x * x - 1) * gaussian(x);
  }
  
  public static double gaussianIntegral(double x) {
    if (x < -8.)
      return -.5;
    if (x > 8.)
      return .5;
    
    double xx = x * x;
    
    double sum = 0.0, term = x;
    for (int i = 3;; i += 2) {
      double next = sum + term;
      if (next == sum)
        break;
      
      sum = next;
      term = term * xx / i;
    }
    
    return sum * gaussian(x);
  }
  
  public static Function[] getGaussianSet(double sigma) {
    return new Function[] {new GaussianIntegral(sigma), new Gaussian(sigma),
        new GaussianDerivative(sigma), new GaussianSecondDerivative(sigma)};
  }
  
  public static int[] getWindow(Function f, double x) {
    double wx = f.getWindowSize();
    return new int[] {(int) (x - wx), (int) (x + wx + 1)};
  }
  
  public static double[] computeTable(Function f, double x, int[] offset) {
    int[] b = getWindow(f, x);
    offset[0] = b[0] - 1;
    
    double[] result = new double[b[1] - offset[0] + 2];
    for (int xx = 0; xx != result.length; ++xx)
      result[xx] = f.eval(x - xx - offset[0]);
    
    return result;
  }
  
  public static double[] computeDifferenceTable(Function f, double x, int[] offset) {
    double[] table = computeTable(f, x, offset);
    
    double[] result = new double[table.length - 1];
    for (int i = 0; i != result.length; ++i)
      result[i] = table[i] - table[i + 1];
    
    return result;
  }
  
  public static double[][] computeDifferenceTables(Function f, int scale, int[] offsets,
      int[] boundary) {
    double[][] tables = new double[scale][];
    for (int i = 0; i != scale; ++i) {
      int[] offset = new int[1];
      tables[i] = computeDifferenceTable(f, (i + 0.5) / scale, offset);
      offsets[i] = offset[0];
    }
    
    if (boundary != null)
      for (int i = 0; i != scale; ++i) {
        boundary[0] = Math.max(boundary[0], -offsets[i]);
        boundary[0] = Math.max(boundary[0], tables[i].length + offsets[i] - 1);
      }
    
    return tables;
  }
  
  public static double compute(double[][] image, Function fx, Function fy, double x, double y) {
    int[] offsetX = new int[1];
    int[] offsetY = new int[1];
    
    double[] gx = computeDifferenceTable(fx, x, offsetX);
    double[] gy = computeDifferenceTable(fy, y, offsetY);
    
    double sum = 0;
    for (int yy = 0; yy != gy.length; ++yy)
      for (int xx = 0; xx != gx.length; ++xx)
        sum += image[yy + offsetY[0]][xx + offsetX[0]] * gx[xx] * gy[yy];
    
    return sum;
  }
  
  public static double[] convolve(double[] line, int scale, Function f, int[] boundary) {
    int sx = line.length;
    
    int[] offsets = new int[scale];
    
    double[][] tables = computeDifferenceTables(f, scale, offsets, boundary);
    
    double[] result = new double[scale * sx];
    
    for (int i = 0; i != scale; ++i)
      for (int x = boundary[0]; x != sx - boundary[0]; ++x)
        for (int a = 0; a != tables[i].length; ++a)
          result[scale * x + i] += line[a + x + offsets[i]] * tables[i][a];
    
    return result;
  }
  
  public static double[][] convolve(double[][] image, int scale, Function fx, Function fy,
      int[] boundary) {
    int sx = image[0].length;
    int sy = image.length;
    
    int[] offsetsX = new int[scale];
    int[] offsetsY = new int[scale];
    
    double[][] tablesX = computeDifferenceTables(fx, scale, offsetsX, boundary);
    double[][] tablesY = computeDifferenceTables(fy, scale, offsetsY, boundary);
    
    double[][] temp = new double[sy][scale * sx];
    
    for (int y = 0; y != sy; ++y)
      for (int i = 0; i != scale; ++i)
        for (int x = boundary[0]; x != sx - boundary[0]; ++x)
          for (int a = 0; a != tablesX[i].length; ++a)
            temp[y][scale * x + i] += image[y][a + x + offsetsX[i]] * tablesX[i][a];
    
    double[][] result = new double[scale * sy][scale * sx];
    
    for (int x = scale * boundary[0]; x != scale * (sx - boundary[0]); ++x)
      for (int i = 0; i != scale; ++i)
        for (int y = boundary[0]; y != sy - boundary[0]; ++y)
          for (int a = 0; a != tablesY[i].length; ++a)
            result[scale * y + i][x] += temp[a + y + offsetsY[i]][x] * tablesY[i][a];
    
    System.err.println("convolution done");
    return result;
  }
  
  public static double getFactor(double a, double b) {
    if (a == 0)
      return 0;
    
    if (a < 0) {
      a = -a;
      b = -b;
    }
    
    if (b > 0)
      return 0;
    
    return a / (a + b);
  }
  
  public static boolean[][] computeZeroCrossings(double[][] values, double[][] signs) {
    int sx = values[0].length;
    int sy = values.length;
    
    double[][][] points = new double[2][sy][sx];
    for (int i = 0; i != 2; ++i)
      for (int y = 0; y != (i == 0 ? sy : sy - 1); ++y)
        for (int x = 0; x != (i == 0 ? sx - 1 : sx); ++x) {
          double factor = getFactor(values[y][x], i == 0 ? values[y][x + 1] : values[y + 1][x]);
          if (factor != 0) {
            double sign =
                factor * signs[y][x] + (1 - factor) * (i == 0 ? signs[y][x + 1] : signs[y + 1][x]);
            //if (sign > 0)
            points[i][y][x] = factor;
          }
        }
    
    int[] counters = new int[5];
    int k = 0;
    
    for (int y = 0; y != sy - 1; ++y)
      for (int x = 0; x != sx - 1; ++x) {
        int count = 0;
        
        if (points[0][y][x] != 0)
          ++count;
        if (points[1][y][x] != 0)
          ++count;
        if (points[0][y + 1][x] != 0)
          ++count;
        if (points[1][y][x + 1] != 0)
          ++count;
        
        ++counters[count];
        
        if (count % 2 == 1)
          k = Math.max(k, Math.min(Math.min(x, sx - x - 1), Math.min(y, sy - y - 1)));
      }
    
    System.err.println("dist: " + k);
    
    for (int i = 0; i != 5; ++i)
      System.err.println(counters[i]);
    
    boolean[][] result = new boolean[sy][sx];
    for (int y = 0; y != sy - 1; ++y)
      for (int x = 0; x != sx - 1; ++x) {
        int count = 0;
        
        if (points[0][y][x] != 0)
          ++count;
        if (points[1][y][x] != 0)
          ++count;
        if (points[0][y + 1][x] != 0)
          ++count;
        if (points[1][y][x + 1] != 0)
          ++count;
        
        if (count != 0)
          result[y][x] = true;
      }
    
    return result;
  }
  
  public static double[][][] computeLVs(double[][] image, double sigma, int scale, int[] boundary) {
    Function[] f = getGaussianSet(sigma);
    
    double[][][] result = new double[4][][];
    
    int[] b = new int[1];
    
    double[][] lx = convolve(image, scale, f[1], f[0], boundary);
    double[][] ly = convolve(image, scale, f[0], f[1], boundary);
    
    System.err.println(b[0]);
    
    {
      double[][] lxlxlxlxxx =
          ImageOpsDouble.mul(lx, lx, lx, convolve(image, scale, f[3], f[0], boundary));
      double[][] lylylylyyy =
          ImageOpsDouble.mul(ly, ly, ly, convolve(image, scale, f[0], f[3], boundary));
      
      double[][] lxlxxy = ImageOpsDouble.mul(lx, convolve(image, scale, f[2], f[1], boundary));
      double[][] lylxyy = ImageOpsDouble.mul(ly, convolve(image, scale, f[1], f[2], boundary));
      
      result[3] =
          ImageOpsDouble.add(lxlxlxlxxx, lylylylyyy, ImageOpsDouble.mul(3, lx, ly, lxlxxy, lylxyy));
    }
    
    {
      double[][] lxlxlxx = ImageOpsDouble.mul(lx, lx, convolve(image, scale, f[2], f[0], boundary));
      double[][] lylylyy = ImageOpsDouble.mul(ly, ly, convolve(image, scale, f[0], f[2], boundary));
      
      double[][] lxlylxy2 =
          ImageOpsDouble.mul(2, lx, ly, convolve(image, scale, f[1], f[1], boundary));
      
      result[2] = ImageOpsDouble.add(lxlxlxx, lylylyy, lxlylxy2);
    }
    
    result[1] = ImageOpsDouble.add(ImageOpsDouble.mul(lx, lx), ImageOpsDouble.mul(ly, ly));
    result[0] = convolve(image, scale, f[0], f[0], boundary);
    
    return result;
  }
  
  public static double[][][] computeLUVs(double[][] image, double sigma, int scale, int[] boundary) {
    Function[] f = getGaussianSet(sigma);
    
    double[][][] result = new double[4][][];
    
    double[][] lx = convolve(image, scale, f[1], f[0], boundary);
    double[][] ly = convolve(image, scale, f[0], f[1], boundary);
    
    double[][] lxx = convolve(image, scale, f[2], f[0], boundary);
    double[][] lyy = convolve(image, scale, f[0], f[2], boundary);
    
    double[][] lxy = convolve(image, scale, f[1], f[1], boundary);
    
    {
      double[][] a =
          ImageOpsDouble.add(ImageOpsDouble.mul(lx, lx, lyy), ImageOpsDouble.mul(ly, ly, lxx));
      double[][] b = ImageOpsDouble.mul(2, lx, ly, lxy);
      
      result[1] = ImageOpsDouble.add(a, b);
      result[2] = ImageOpsDouble.subtract(a, b);
    }
    
    {
      double[][] a = ImageOpsDouble.mul(lx, ly, ImageOpsDouble.subtract(lxx, lyy));
      double[][] b =
          ImageOpsDouble.mul(
              ImageOpsDouble.subtract(ImageOpsDouble.mul(lx, lx), ImageOpsDouble.mul(ly, ly)), lxy);
      
      result[0] = ImageOpsDouble.subtract(a, b);
    }
    
    return result;
  }
  
  public static double[][]
      computeLvLvLvv(double[][] image, double sigma, int scale, int[] boundary) {
    Function[] f = getGaussianSet(sigma);
    
    double[][] lx = convolve(image, scale, f[1], f[0], boundary);
    double[][] ly = convolve(image, scale, f[0], f[1], boundary);
    
    double[][] lxlxlxx = ImageOpsDouble.mul(lx, lx, convolve(image, scale, f[2], f[0], boundary));
    double[][] lylylyy = ImageOpsDouble.mul(ly, ly, convolve(image, scale, f[0], f[2], boundary));
    
    double[][] lxlylxy2 =
        ImageOpsDouble.mul(2, lx, ly, convolve(image, scale, f[1], f[1], boundary));
    
    double[][] lvlvlvv = ImageOpsDouble.add(lxlxlxx, lylylyy, lxlylxy2);
    //double[][] lvlv = ImageOpsDouble.add(ImageOpsDouble.mul(lx, lx), ImageOpsDouble.mul(ly, ly));
    
    return lvlvlvv;
  }
  
  public static boolean[][] getPositive(double[][] input) {
    int sx = input[0].length;
    int sy = input.length;
    
    boolean[][] result = new boolean[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = input[y][x] > 0;
    
    return result;
  }
  
  public static int getLvLvLvvBoundary(double sigma, int scale) throws IOException {
    Integer result = Cache.cache("gaussian-lvlvlvv-boundary/%f/%d", sigma, scale);
    if (result != null)
      return result;
    
    int[] boundary = new int[1];
    computeLvLvLvv(new double[256][256], sigma, scale, boundary);
    return boundary[0];
  }
  
  public static double[][] getLvLvLvvHelper(double[][] image, double sigma, int scale)
      throws IOException {
    final int k = getLvLvLvvBoundary(sigma, scale);
    final int maxLength = 4096 / scale;
    
    int sx = image[0].length;
    int sy = image.length;
    
    int nx = (sx - 2 * k) / (maxLength - 2 * k) + 1;
    int ny = (sy - 2 * k) / (maxLength - 2 * k) + 1;
    
    double[][] result = new double[scale * sy][scale * sx];
    int[] boundary = new int[] {k};
    
    for (int q = 0; q != ny; ++q)
      for (int p = 0; p != nx; ++p) {
        System.err.println("(" + p + ", " + q + ")");
        
        int x0 = k + (p * (sx - 2 * k)) / nx;
        int y0 = k + (q * (sy - 2 * k)) / ny;
        
        int x1 = k + ((p + 1) * (sx - 2 * k)) / nx;
        int y1 = k + ((q + 1) * (sy - 2 * k)) / ny;
        
        double[][] r =
            computeLvLvLvv(ImageOpsDouble.section(image, x0 - k, y0 - k, x1 + k, y1 + k), sigma,
                scale, boundary);
        
        for (int b = 0; b != scale * (y1 - y0); ++b)
          for (int a = 0; a != scale * (x1 - x0); ++a)
            result[scale * y0 + b][scale * x0 + a] = r[scale * k + b][scale * k + a];
      }
    
    return result;
  }
  
  public static BinaryImage binarize(double[][] image, double threshold, boolean value) {
    int sx = image[0].length;
    int sy = image.length;
    
    int a = 0;
    int b = 0;
    
    BinaryImage result = new BinaryImage(sx, sy);
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result.set(x, y, (image[y][x] >= threshold) == value);
    
    return result;
  }
  
  public static BinaryImage computeLogLvLvLvvAdjusted(double[][] image, double sigma, int scale,
      double threshold) throws IOException {
    return binarize(getLvLvLvvHelper(ImageOpsDouble.log(image), sigma, scale), threshold, true);
  }
  
  public static void main(String[] args) throws IOException {
    final int stitch = 0;
    
    double sigma = 1;
    int scale = 1;
    double threshold = 0.000002;
    
    int boundary = getLvLvLvvBoundary(sigma, scale);
    System.err.println("boundary: " + boundary);
    
    StitchStackProperties stack = Images.getStitchStackProperties();
    
    Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges =
        Sharpness.computeEdges(stitch, 1, boundary, true);
    
    BinaryImage[] images = new BinaryImage[stack.getImageSetProperties(stitch).getNumImages()];
    for (int image = 0; image != images.length; ++image) {
      System.err.println("image " + image + "...");
      
      double[][] c1 = Images.getFixedRawComponent(stitch, image, 1);
      double[][] c2 = Images.getFixedRawComponent(stitch, image, 2);
      
      images[image] =
          computeLogLvLvLvvAdjusted(Images.interpolate(c1, c2), sigma, scale, threshold);
    }
    
    BinaryImage result = Sharpness.render(scale, stack, stitch, 1, images, edges);
    
    Streams.writeObject(DataTools.DIR + "gaussian-edges-" + scale, result);
    
    double[][] source;
    
    {
      double[][][] imgs = new double[stack.getImageSetProperties(stitch).getNumImages()][][];
      for (int image = 0; image != images.length; ++image) {
        double[][] c1 = Images.getFixedRawComponent(stitch, image, 1);
        double[][] c2 = Images.getFixedRawComponent(stitch, image, 2);
        
        imgs[image] = Images.interpolate(c1, c2);
      }
      
      source = Sharpness.render(1, stack, stitch, 1, imgs, edges);
      
      Streams.writeObject(DataTools.DIR + "source-" + scale, source);
    }
    
    //BinaryImage result = Streams.readObject(DataTools.DIR + "gaussian-edges-" + scale);
    
    ArrayList<BufferedImage> imgs = new ArrayList<BufferedImage>();
    
    imgs.add(SharpnessEvaluator.render(stack.getX1() - stack.getX0(),
        stack.getY1() - stack.getY0(), scale, edges));
    imgs.add(MetalTools.toImage(source, 128));
    imgs.add(MetalTools.toImage(result.render()));
    
    Tools.displayImages(imgs.toArray(new BufferedImage[] {}));
  }
}
