package test;


import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.Tools;

public class ColorScaling {

  public static BufferedImage getComponent(BufferedImage source, int dx, int dy) {
    int sx = source.getWidth();
    int sy = source.getHeight();
    WritableRaster raster = source.getRaster();

    BufferedImage result = new BufferedImage(sx / 2, sy / 2, BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster resultRaster = result.getRaster();

    int[] pixel = new int[1];
    for (int y = 0; y != sy / 2; ++y) {
      for (int x = 0; x != sx / 2; ++x) {
        raster.getPixel(2 * x + dx, 2 * y + dy, pixel);
        resultRaster.setPixel(x, y, pixel);
      }
    }

    return result;
  }

  public static BufferedImage rescale(BufferedImage source, double threshold) {
    WritableRaster raster = source.getRaster();
    int[] values = new int[1 << 16];

    int sx = source.getWidth();
    int sy = source.getHeight();

    int[] pixel = {0};
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        ++values[pixel[0]];
      }

    int limit = 0;
    int sum = 0;
    while (sum < threshold * sx * sy)
      sum += values[limit++];

    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster resultRaster = result.getRaster();
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        pixel[0] = pixel[0] > limit ? 255 : (255 * pixel[0]) / limit;
        resultRaster.setPixel(x, y, pixel);
      }

    return result;
  }
  
  public static double getDistance(double[][] source, double[][] dest, double factor, double cx, double cy) {
    int sy = source.length;
    int sx = source[0].length;
    
    int bad = 0;
    
    double sum = 0;
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        double zx = factor * (x - cx) + cx;
        double zy = factor * (y - cy) + cx;
        
        if (zx < 0 || zx > sx - 1 || zy < 0 || zy > sy - 1) {
          ++bad;
          continue;
        }
        
        int ax = (int) Math.floor(zx);
        double dax = Math.abs(zx - ax);
        
        int bx = (int) Math.ceil(zx);
        double dbx = Math.abs(zx - bx);
        
        int ay = (int) Math.floor(zy);
        double day = Math.abs(zy - ay);
        
        int by = (int) Math.ceil(zy);
        double dby = Math.abs(zy - by);
        
        if (ax == bx) {
          dax = 1;
          dbx = 0;
        }
        
        if (ay == by) {
          day = 1;
          dby = 0;
        }
        
        double val = 0;
        val += dbx * dby * dest[ay][ax];
        val += dax * dby * dest[ay][bx];
        val += dbx * day * dest[by][ax];
        val += dax * day * dest[by][bx];
        
        double diff = source[y][x] - val;
        
        /*int dx = (int) Math.round(zx);
        int dy = (int) Math.round(zy);
        double diff = dest[dy][dx] - source[y][x];*/
        sum += diff * diff;
      }
    
    return sum / (sx * sy - bad);
  }

  public static void main(String[] args) throws IOException {
    BufferedImage image = ImageIO.read(new File(args[0]));
    double[][][] components = new double[4][][];
    
    for (int dy = 0; dy != 2; ++dy)
      for (int dx = 0; dx != 2; ++dx) {
        int i = 2 * dy + dx;
        components[i] = Tools.getMatrixFromImage(getComponent(image, dx, dy));
        Tools.scaleMatrix(components[i], 128 / Tools.findMean(components[i]));
        ImageIO.write(Tools.getGreyscaleImageFromMatrix(components[i]), "PNG", new File(args[1] + "-" + i + ".png"));
      }
  }
}
