package operations.image;

import java.io.IOException;

import metal.MetalTools;

import data.Tools;

public class ImageOpsFloat {
  
  public static float[][] section(float[][] image, int x0, int y0, int x1, int y1) {
    float[][] result = new float[y1 - y0][x1 - x0];
    for (int y = Math.max(y0, 0); y != Math.min(image.length, y1); ++y)
      for (int x = Math.max(x0, 0); x != Math.min(image[0].length, x1); ++x)
        result[y - y0][x - x0] = image[y][x];

    return result;
  }
  
  public static float[][] fromDouble(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (float) image[y][x];
    return result;
  }
  
  public static float[][] zero(float[][] image) {
    return new float[image.length][image[0].length];
  }
  
  public static float[][] constant(float[][] image, float val) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = val;
    return result;
  }
  
  public static float[][] copy(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = image[y][x];
    return result;
  }
  
  public static float[][] log(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (float) Math.log(image[y][x]);
    return result;
  }
  
  public static float[][] exp(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (float) Math.exp(image[y][x]);
    return result;
  }
  
  public static float[][] add(float[][]... images) {
    float[][] result = new float[images[0].length][images[0][0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x)
        for (float[][] image : images)
          result[y][x] += image[y][x];
    return result;
  }
  
  public static float[][] add(float[][] image0, float value) {
    float[][] result = new float[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] + value;
    return result;
  }
  
  public static float[][] mul(float value, float[][]... images) {
    float[][] result = new float[images[0].length][images[0][0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        result[y][x] = value;
        for (float[][] image : images)
          result[y][x] *= image[y][x];
      }
    return result;
  }
  
  public static float[][] mul(float[][]... images) {
    return mul(1, images);
  }
  
  public static float[][] subtract(float[][] image0, float[][] image1) {
    float[][] result = new float[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] - image1[y][x];
    return result;
  }
  
  public static float[][] subtract(float[][] image0, float value) {
    float[][] result = new float[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] - value;
    return result;
  }
  
  public static float[][] div(float[][] image0, float[][] image1) {
    float[][] result = new float[image0.length][image0[0].length];
    for (int y = 0; y != image0.length; ++y)
      for (int x = 0; x != image0[0].length; ++x)
        result[y][x] = image0[y][x] / image1[y][x];
    return result;
  }
  
  public static float[][] negate(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = -image[y][x];
    return result;
  }
  
  public static float[][] abs(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = Math.abs(image[y][x]);
    return result;
  }
  
  public static float[][] sqrt(float[][] image) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != image.length; ++y)
      for (int x = 0; x != image[0].length; ++x)
        result[y][x] = (float) Math.sqrt(image[y][x]);
    return result;
  }
  
  public static float sum(float[][] image) {
    float sum = 0;
    for (float[] row : image) {
      float rowSum = 0;
      for (float val : row)
        rowSum += val;
      sum += rowSum;
    }
    return sum;
  }
  
  public static float mean(float[][] image) {
    return sum(image) / (image.length * image[0].length);
  }
  
  public static float min(float[][] image) {
    float min = Float.POSITIVE_INFINITY;
    for (float[] row : image)
      for (float val : row)
        min = Math.min(min, val);
    return min;
  }
  
  public static float max(float[][] image) {
    float max = Float.NEGATIVE_INFINITY;
    for (float[] row : image)
      for (float val : row)
        max = Math.max(max, val);
    return max;
  }
  
  public static float norm(float[][] image) {
    float sum = 0;
    for (float[] row : image) {
      float rowSum = 0;
      for (float val : row)
        rowSum += val * val;
      sum += rowSum;
    }
    return (float) Math.sqrt(sum);
  }
  
  public static float distance(float[][] a, float[][] b) {
    return norm(subtract(a, b));
  }
  
  public static float[][] convolve(float[][] image, float[][] filter, int dx, int dy) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        float sum = 0;
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
  
  public static float[][] convolveX(float[][] image, float[] filterX, int dx) {
    float[][] result = new float[image.length][image[0].length];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        float sum = 0;
        for (int xx = 0; xx != filterX.length; ++xx) {
          int xxx = x + xx + dx;
          if (xxx >= 0 && xxx < image[0].length)
            sum += filterX[xx] * image[y][xxx];
        }
        result[y][x] = sum;
      }
    return result;
  }
  
  public static float[][] convolveY(float[][] image, float[] filterY, int dy) {
    float[][] result = new float[image.length][image[0].length];
    for (int x = 0; x != result[0].length; ++x)
      for (int y = 0; y != result.length; ++y) {
        float sum = 0;
        for (int yy = 0; yy != filterY.length; ++yy) {
          int yyy = y + yy + dy;
          if (yyy >= 0 && yyy < image.length)
            sum += filterY[yy] * image[yyy][x];
        }
        result[y][x] = sum;
      }
    return result;
  }
  
  public static float[][]
      convolve(float[][] image, float[] filterX, float[] filterY, int dx, int dy) {
    image = convolveX(image, filterX, dx);
    image = convolveY(image, filterY, dy);
    return image;
  }
  
  public static float[][] convolve(float[][] image, float[] filter, int d) {
    return convolve(image, filter, filter, d, d);
  }
  
  public static int getLimit(float[][] image, int dir) {
    return dir == 0 ? image.length : image[0].length;
  }
  
  public static float[] getLine(float[][] image, int index, int dir) {
    float[] line = new float[getLimit(image, 1 - dir)];
    for (int i = 0; i != line.length; ++i)
      line[i] = image[dir == 0 ? index : i][dir == 0 ? i : index];
    return line;
  }
  
  public static void setLine(float[][] image, int index, int dir, float[] line) {
    for (int i = 0; i != line.length; ++i)
      image[dir == 0 ? index : i][dir == 0 ? i : index] = line[i];
  }
  
  public static void write(float[][] image, String name) throws IOException {
    Tools.writePNG(MetalTools.toImage(image, 128), name);
  }
  
  public static void writeExp(float[][] image, String name) throws IOException {
    Tools.writePNG(MetalTools.toImage(exp(image), 128), name);
  }
  
}
