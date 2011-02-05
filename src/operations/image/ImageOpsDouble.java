package operations.image;

import java.io.IOException;

import data.Tools;

import metal.MetalTools;

public class ImageOpsDouble {
  
  public static double[][] section(double[][] image, int x0, int y0, int x1, int y1) {
    double[][] result = new double[y1 - y0][x1 - x0];
    for (int y = Math.max(y0, 0); y != Math.min(image.length, y1); ++y)
      for (int x = Math.max(x0, 0); x != Math.min(image[0].length, x1); ++x)
        result[y - y0][x - x0] = image[y][x];

    return result;
  }
  
  public static double[][] fromFloat(float[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = image[y][x];
    return result;
  }
  
  public static double[][] zero(double[][] image) {
    return new double[image.length][image[0].length];
  }
  
  public static double[][] constant(double[][] image, double val) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = val;
    return result;
  }
  
  public static double[][] copy(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = image[y][x];
    return result;
  }
  
  public static double[][] log(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (double) Math.log(image[y][x]);
    return result;
  }
  
  public static double[][] exp(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (double) Math.exp(image[y][x]);
    return result;
  }
  
  public static double[][] add(double[][]... images) {
    double[][] result = new double[images[0].length][images[0][0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x)
        for (double[][] image : images)
          result[y][x] += image[y][x];
    return result;
  }
  
  public static double[][] add(double[][] image0, double value) {
    double[][] result = new double[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] + value;
    return result;
  }
  
  public static double[][] mul(double value, double[][]... images) {
    double[][] result = new double[images[0].length][images[0][0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        result[y][x] = value;
        for (double[][] image : images)
          result[y][x] *= image[y][x];
      }
    return result;
  }
  
  public static double[][] mul(double[][]... images) {
    return mul(1, images);
  }
  
  public static double[][] subtract(double[][] image0, double[][] image1) {
    double[][] result = new double[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] - image1[y][x];
    return result;
  }
  
  public static double[][] subtract(double[][] image0, double value) {
    double[][] result = new double[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] - value;
    return result;
  }
  
  public static double[][] div(double[][] image0, double[][] image1) {
    double[][] result = new double[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] / image1[y][x];
    return result;
  }
  
  public static double[][] negate(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = -image[y][x];
    return result;
  }
  
  public static double[][] abs(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = Math.abs(image[y][x]);
    return result;
  }
  
  public static double[][] sqrt(double[][] image) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = Math.sqrt(image[y][x]);
    return result;
  }
  
  public static double sum(double[][] image) {
    double sum = 0;
    for (double[] row : image) {
      double rowSum = 0;
      for (double val : row)
        rowSum += val;
      sum += rowSum;
    }
    return sum;
  }
  
  public static double mean(double[][] image) {
    return sum(image) / (image.length * image[0].length);
  }
  
  public static double min(double[][] image) {
    double min = Float.POSITIVE_INFINITY;
    for (double[] row : image)
      for (double val : row)
        min = Math.min(min, val);
    return min;
  }
  
  public static double max(double[][] image) {
    double max = Float.NEGATIVE_INFINITY;
    for (double[] row : image)
      for (double val : row)
        max = Math.max(max, val);
    return max;
  }
  
  public static double norm(double[][] image) {
    double sum = 0;
    for (double[] row : image) {
      double rowSum = 0;
      for (double val : row)
        rowSum += val * val;
      sum += rowSum;
    }
    return Math.sqrt(sum);
  }
  
  public static double distance(double[][] a, double[][] b) {
    return norm(subtract(a, b));
  }
  
  public static double[][] convolve(double[][] image, double[][] filter, int dx, int dy) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        double sum = 0;
        for (int yy = 0; yy != filter.length; ++yy)
          for (int xx = 0; xx != filter[0].length; ++x) {
            int xxx = x + xx + dx;
            int yyy = y + yy + dy;
            if (xxx >= 0 && yyy >= 0 && xxx < image[0].length && yyy < image.length)
              sum += filter[yy][xx] * image[yyy][xxx];
          }
        result[y][x] = sum;
      }
    return result;
  }
  
  public static double[][] convolveX(double[][] image, double[] filterX, int dx) {
    double[][] result = new double[image.length][image[0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        double sum = 0;
        for (int xx = 0; xx != filterX.length; ++xx) {
          int xxx = x + xx + dx;
          if (xxx >= 0 && xxx < image[0].length)
            sum += filterX[xx] * image[y][xxx];
        }
        result[y][x] = sum;
      }
    return result;
  }
  
  public static double[][] convolveY(double[][] image, double[] filterY, int dy) {
    double[][] result = new double[image.length][image[0].length];
    for (int x = 0; x != result[0].length; ++x)
      for (int y = 0; y != result.length; ++y) {
        double sum = 0;
        for (int yy = 0; yy != filterY.length; ++yy) {
          int yyy = y + yy + dy;
          if (yyy >= 0 && yyy < image.length)
            sum += filterY[yy] * image[yyy][x];
        }
        result[y][x] = sum;
      }
    return result;
  }
  
  public static double[][] convolve(double[][] image, double[] filterX, double[] filterY, int dx,
      int dy) {
    image = convolveX(image, filterX, dx);
    image = convolveY(image, filterY, dy);
    return image;
  }
  
  public static double[][] convolve(double[][] image, double[] filter, int d) {
    return convolve(image, filter, filter, d, d);
  }
  
  public static int getLimit(double[][] image, int dir) {
    return dir == 0 ? image.length : image[0].length;
  }
  
  public static double[] getLine(double[][] image, int index, int dir) {
    double[] line = new double[getLimit(image, 1 - dir)];
    for (int i = 0; i != line.length; ++i)
      line[i] = image[dir == 0 ? index : i][dir == 0 ? i : index];
    return line;
  }
  
  public static void setLine(double[][] image, int index, int dir, double[] line) {
    for (int i = 0; i != line.length; ++i)
      image[dir == 0 ? index : i][dir == 0 ? i : index] = line[i];
  }
  
  public static void write(double[][] image, String name) throws IOException {
    Tools.writePNG(MetalTools.toImage(image, 128), name);
  }
  
  public static void writeExp(double[][] image, String name) throws IOException {
    Tools.writePNG(MetalTools.toImage(exp(image), 128), name);
  }
  
}
