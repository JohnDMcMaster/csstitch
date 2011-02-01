package realmetal;

import general.Streams;
import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import operations.image.ImageOpsDouble;

import segment.Sharpness;
import stitcher.StitchInfo;

import cache.Cache;
import data.DataTools;
import data.Tools;

import map.Map;
import map.properties.StitchStackProperties;
import metal.MetalTools;

public class Images {
  
  private static void readSecondOrderLighting(String filename, double[] secondOrderLighting,
      double[] directionalLighting) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");
    
    for (int image = 0; image != secondOrderLighting.length; ++image)
      secondOrderLighting[image] = Math.exp(scanner.nextDouble());
    
    for (int i = 0; i != 2; ++i)
      directionalLighting[i] = scanner.nextDouble();
    
    in.close();
  }
  
  private static void initImageComponents(final int stitch, final int channel) throws IOException {
    StitchStackProperties stack = getStitchStackProperties();
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    
    double[][] lighting = Streams.readObject(DataTools.DIR + "light-dist-20");
    double[] secondOrderLighting = new double[maps.length];
    double[] directionalLighting = new double[2];
    readSecondOrderLighting(DataTools.DIR + "devig-second-order-n" + StitchInfo.SUFFICES[stitch]
        + ".txt", secondOrderLighting, directionalLighting);
    
    int sx = stack.getImageSetProperties(0).getSize().getSx();
    int sy = stack.getImageSetProperties(0).getSize().getSy();
    
    double[] coords = new double[2];
    double sum = 0;
    
    double[][][] components = new double[maps.length][][];
    for (int image = 0; image != components.length; ++image) {
      components[image] = getRawComponent(stitch, image, channel);
      
      for (int b = 0; b != sy / 2; ++b)
        for (int a = 0; a != sx / 2; ++a) {
          int x = 2 * a + channel % 2;
          int y = 2 * b + channel / 2;
          
          coords[0] = x;
          coords[1] = y;
          
          maps[image].map(coords, coords);
          
          double directionalLight = 0;
          for (int i = 0; i != 2; ++i)
            directionalLight += directionalLighting[i] * coords[i];
          double light =
              lighting[y][x] * secondOrderLighting[image] * Math.exp(0.5 * directionalLight);
          
          components[image][b][a] /= light;
        }
      
      fixDeadPixels(components[image], getDeadPixelsInComponent(channel));
      
      sum += ImageOpsDouble.mean(components[image]);
    }
    
    sum /= components.length;
    
    for (int image = 0; image != components.length; ++image) {
      components[image] = ImageOpsDouble.mul(1 / sum, components[image]);
      String path =
          String
              .format("%s/images/%d/%d/%d", Cache.CACHE.getAbsolutePath(), stitch, image, channel);
      new File(path).getParentFile().mkdirs();
      Streams.writeObject(path, components[image]);
    }
  }
  
  public static double[][] getImageComponent(final int stitch, final int image, final int channel)
      throws IOException {
    String path =
        String.format("%s/images/%d/%d/%d", Cache.CACHE.getAbsolutePath(), stitch, image, channel);
    if (!new File(path).exists())
      initImageComponents(stitch, channel);
    
    return Streams.readObject(path);
  }
  
  public static double[][] interpolate(double[][] i1, double[][] i2) {
    int sx = 2 * i1[0].length;
    int sy = 2 * i1.length;
    
    double[][] i = new double[sy][sx];
    for (int y = 0; y != sy / 2; ++y)
      for (int x = 0; x != sx / 2; ++x) {
        i[2 * y + 0][2 * x + 1] = i1[y][x];
        i[2 * y + 1][2 * x + 0] = i2[y][x];
      }
    
    double[][] image = new double[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if ((x + y) % 2 == 1)
          image[y][x] = i[y][x];
        else {
          if (x == 0 && y == 0)
            image[y][x] = (i[y][x + 1] + i[y + 1][x]) / 2;
          else if (x == sx - 1 && y == sy - 1)
            image[y][x] = (i[y][x - 1] + i[y - 1][x]) / 2;
          else if (x == 0 || x == sx - 1)
            image[y][x] = (i[y - 1][x] + i[y + 1][x]) / 2;
          else if (y == 0 || y == sy - 1)
            image[y][x] = (i[y][x - 1] + i[y][x + 1]) / 2;
          else {
            double x0 = i[y][x - 1];
            double x1 = i[y][x + 1];
            
            double y0 = i[y - 1][x];
            double y1 = i[y + 1][x];
            
            if (x0 < x1) {
              double t = x0;
              x0 = x1;
              x1 = t;
            }
            
            if (y0 < y1) {
              double t = y0;
              y0 = y1;
              y1 = t;
            }
            
            if (x0 < y0) {
              double t = x0;
              x0 = y0;
              y0 = t;
              t = x1;
              x1 = y1;
              y1 = t;
            }
            
            if (y1 > x1)
              image[y][x] = (y0 + y1) / 2;
            else if (y0 > x1)
              image[y][x] = (x1 + y0) / 2;
            else {
              double sum = 0;
              int weight = 0;
              
              for (int b = -2; b <= 2; ++b)
                for (int a = -2; a <= 2; ++a)
                  if (Math.abs(a) + Math.abs(b) == 3) {
                    int xx = x + a;
                    int yy = y + b;
                    if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
                      sum += i[yy][xx];
                      ++weight;
                    }
                  }
              
              sum /= weight;
              if (Math.abs(sum - x1) > Math.abs(sum - y0))
                image[y][x] = (x0 + x1) / 2;
              else
                image[y][x] = (y0 + y1) / 2;
            }
          }
        }
      }
    
    return image;
  }
  
  public static double[][] getInterpolatedImage(final int stitch, final int image)
      throws IOException {
    double[][] result = Cache.cache("images-interpolated/%d/%d", stitch, image);
    if (result != null)
      return result;
    
    double[][] c1 = getImageComponent(stitch, image, 1);
    double[][] c2 = getImageComponent(stitch, image, 2);
    return interpolate(c1, c2);
  }
  
  public static BufferedImage getGreenImage(final int stitch, final int image) throws IOException {
    File file =
        new File(Cache.CACHE.getCanonicalPath() + "/images-green/" + stitch + "/" + image + ".png");
    if (file.exists())
      return ImageIO.read(file);
    
    BufferedImage result = MetalTools.toImage(getInterpolatedImage(stitch, image), 128);
    Tools.ensurePath(file.getAbsolutePath());
    ImageIO.write(result, "png", file);
    return result;
  }
  
  private static final int[] STITCH_NUMBERS = new int[] {6114199, 6214326, 5193940};
  
  public static StitchStackProperties getStitchStackProperties() throws IOException {
    int numStitches = 3;
    
    String[] filenames = new String[3];
    for (int i = 0; i != numStitches; ++i) {
      filenames[i] =
          DataTools.DIR + "stitching" + StitchInfo.SUFFICES[i]
              + "/modern-filtering-10m7m5m3mlpn.txt";
    }
    
    String transformsFilename = DataTools.DIR + "affine-transforms";
    
    return new StitchStackProperties(filenames, transformsFilename);
  }
  
  public static double[][] getRawComponent(final int stitch, final int image, final int channel)
      throws IOException {
    if (!(image >= 0 && image < StitchInfo.NUM_IMAGES[stitch]))
      throw new IllegalArgumentException("image number out of range: " + image);
    
    WritableRaster raster =
        ImageIO.read(
            new File(DataTools.DIR + "data/P" + (STITCH_NUMBERS[stitch] + image) + "/" + channel
                + ".png")).getRaster();
    
    int sx = raster.getWidth();
    int sy = raster.getHeight();
    
    double[][] result = new double[sy][sx];
    
    short[] data = (short[]) raster.getDataElements(0, 0, sx, sy, null);
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = (int) (char) data[sx * y + x];
    
    return result;
  }
  
  public static double[][][] getRawComponents(final int stitch, final int image) throws IOException {
    double[][][] result = new double[4][][];
    for (int channel = 0; channel != result.length; ++channel)
      result[channel] = getRawComponent(stitch, image, channel);
    
    return result;
  }
  
  public static double[][][][] getRawComponents(final int stitch) throws IOException {
    double[][][][] result = new double[StitchInfo.NUM_IMAGES[stitch]][][][];
    for (int image = 0; image != result.length; ++image)
      result[image] = getRawComponents(stitch, image);
    
    return result;
  }
  
  // TODO: implement neighbouring dead pixels
  public static void fixDeadPixels(double[][] component, int[][] deadPixels) {
    TreeSet<Pair<Integer, Integer>> set = new TreeSet<Pair<Integer, Integer>>();
    for (int[] deadPixel : deadPixels) {
      int x = deadPixel[0];
      int y = deadPixel[1];
      
      for (int yy = y - 1; yy <= y + 1; ++yy)
        for (int xx = x - 1; xx <= x + 1; ++xx)
          if (Math.abs(y - yy) + Math.abs(x - xx) <= 1)
            if (set.contains(new Pair<Integer, Integer>(xx, yy)))
              throw new RuntimeException("neighbouring dead pixels can currently not be handled");
      
      set.add(new Pair<Integer, Integer>(x, y));
    }
    
    int sx = component[0].length;
    int sy = component.length;
    
    for (int[] deadPixel : deadPixels) {
      int x = deadPixel[0];
      int y = deadPixel[1];
      
      if (x == 0) {
        if (y == 0)
          component[y][x] = (component[y][x + 1] + component[y + 1][x]) / 2;
        else if (y == sy - 1)
          component[y][x] = (component[y][x + 1] + component[y - 1][x]) / 2;
        else
          component[y][x] = (component[y - 1][x] + component[y + 1][x]) / 2;
      } else if (x == sx - 1) {
        if (y == 0)
          component[y][x] = (component[y][x - 1] + component[y + 1][x]) / 2;
        else if (y == sy - 1)
          component[y][x] = (component[y][x - 1] + component[y - 1][x]) / 2;
        else
          component[y][x] = (component[y - 1][x] + component[y + 1][x]) / 2;
      } else {
        double x0 = component[y][x - 1];
        double x1 = component[y][x + 1];
        
        double y0 = component[y - 1][x];
        double y1 = component[y + 1][x];
        
        component[y][x] = (x0 + y0 + x1 + y1) / 4;
      }
    }
  }
  
  public static int[][] getDeadPixelsInComponent(final int channel) {
    ArrayList<int[]> deadPixels = new ArrayList<int[]>();
    for (int[] deadPixel : Tools.DEAD_PIXELS)
      if (deadPixel[0] % 2 == channel % 2 && deadPixel[1] % 2 == channel / 2)
        deadPixels.add(new int[] {deadPixel[0] / 2, deadPixel[1] / 2});
    
    return deadPixels.toArray(new int[][] {});
  }
  
  public static double[][]
      getFixedRawComponent(final int stitch, final int image, final int channel) throws IOException {
    double[][] result = getRawComponent(stitch, image, channel);
    fixDeadPixels(result, getDeadPixelsInComponent(channel));
    return ImageOpsDouble.mul((result.length * result[0].length) / ImageOpsDouble.sum(result),
        result);
  }
  
  public static double[][][] getFixedRawComponents(final int stitch, final int image)
      throws IOException {
    double[][][] result = new double[4][][];
    for (int channel = 0; channel != result.length; ++channel)
      result[channel] = getFixedRawComponent(stitch, image, channel);
    
    return result;
  }
  
  public static double[][][][] getFixedRawComponents(final int stitch) throws IOException {
    double[][][][] result = new double[StitchInfo.NUM_IMAGES[stitch]][][][];
    for (int image = 0; image != result.length; ++image)
      result[image] = getFixedRawComponents(stitch, image);
    
    return result;
  }
  
  public static double[][] getColorComponent(final int stitch, final int image, final int color)
      throws IOException {
    int channel = color + color / 2;
    if (color == 0 || color == 2)
      return getFixedRawComponent(stitch, image, channel);
    
    double[][] c1 = getFixedRawComponent(stitch, image, 1);
    double[][] c2 = getFixedRawComponent(stitch, image, 2);
    return interpolate(c1, c2);
  }
  
  public static double[][]
      getColorComponentDevig(final int stitch, final int image, final int color) throws IOException {
    int channel = color + color / 2;
    if (color == 0 || color == 2)
      return getImageComponent(stitch, image, channel);
    
    double[][] c1 = getImageComponent(stitch, image, 1);
    double[][] c2 = getImageComponent(stitch, image, 2);
    return interpolate(c1, c2);
  }
  
  public static BufferedImage getStitchImage(final int stitch, final int scale, final int color)
      throws IOException {
    File file =
        new File(String.format("%s/stitch-image/%d/%d/%d.png", Cache.CACHE.getAbsolutePath(), stitch,
            scale, color));
    if (file.exists())
      return ImageIO.read(file);
    
    StitchStackProperties stack = getStitchStackProperties();
    
    BufferedImage[] images = new BufferedImage[stack.getImageSetProperties(stitch).getNumImages()];
    for (int image = 0; image != images.length; ++image)
      images[image] = MetalTools.toImage(getColorComponent(stitch, image, color), 128);
    
    BufferedImage result =
        Sharpness.render(scale, stack, stitch, color + color / 2, images, color == 1 ? 1 : 0.5,
            Sharpness.computeEdges(stitch, color, 0, true));
    file.getParentFile().mkdirs();
    ImageIO.write(result, "png", file);
    return result;
  }
  
  public static BufferedImage
      getStitchImageDevig(final int stitch, final int scale, final int color) throws IOException {
    File file =
        new File(String.format("%s/stitch-image-devig/%d/%d/%d.png", Cache.CACHE.getAbsolutePath(),
            stitch, scale, color));
    if (file.exists())
      return ImageIO.read(file);
    
    StitchStackProperties stack = getStitchStackProperties();
    
    BufferedImage[] images = new BufferedImage[stack.getImageSetProperties(stitch).getNumImages()];
    for (int image = 0; image != images.length; ++image)
      images[image] = MetalTools.toImage(getColorComponentDevig(stitch, image, color), 128);
    
    BufferedImage result =
        Sharpness.render(scale, stack, stitch, color + color / 2, images, color == 1 ? 1 : 0.5,
            Sharpness.computeEdges(stitch, color, 0, true));
    file.getParentFile().mkdirs();
    ImageIO.write(result, "png", file);
    return result;
  }
  
  public static BufferedImage combineColors(BufferedImage... images) {
    int sx = images[0].getWidth();
    int sy = images[1].getHeight();
    
    WritableRaster[] sourceRaster = new WritableRaster[3];
    for (int i = 0; i != 3; ++i)
      sourceRaster[i] = images[i].getRaster();
    
    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = result.getRaster();
    
    int[] pixel = new int[3];
    int[] p = new int[1];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        for (int i = 0; i != 3; ++i) {
          sourceRaster[i].getPixel(x, y, p);
          pixel[i] = p[0];
        }
        
        raster.setPixel(x, y, pixel);
      }
    
    return result;
  }
  
  public static BufferedImage getStitchImageDevigColorized(final int stitch, final int scale)
      throws IOException {
    File file =
        new File(String.format("%s/stitch-image-devig-color/%d/%d.png", Cache.CACHE.getAbsolutePath(),
            stitch, scale));
    if (file.exists())
      return ImageIO.read(file);
    
    BufferedImage[] images = new BufferedImage[3];
    for (int color = 0; color != images.length; ++color)
      images[color] = getStitchImageDevig(stitch, scale, color);
    
    BufferedImage result = combineColors(images);
    file.getParentFile().mkdirs();
    ImageIO.write(result, "png", file);
    return result;
  }
  
}
