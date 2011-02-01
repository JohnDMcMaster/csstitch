package devignetter;


import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import stitcher.Stitcher4;

import data.DataTools;
import data.Tools;

public class DevigMedian {

  final static int SU = 5;
  final static int SV = 7;

  final static int SX = 3250;
  final static int SY = 2450;

  static int LX = 0;
  static int LY = 0;

  final static double RADIUS = 32;

  final static double BOUND_LOW = 0.8;
  final static double BOUND_HIGH = 0.9;

  static double SAMPLE_RADIUS = 12;

  final static double SAMPLE_BOUND_LOW = 0.4;
  final static double SAMPLE_BOUND_HIGH = 0.6;

  public static double[][] coefs;

  public static void readParameters(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");

    int su = scanner.nextInt();
    int sv = scanner.nextInt();
    int numImages = su * sv;
    System.out.println(su + " x " + sv);

    for (int i = 0; i != numImages; ++i) {
      scanner.nextDouble();
      scanner.nextDouble();
    }

    int numCoefs = scanner.nextInt();
    coefs = new double[4][numCoefs];
    for (int i = 0; i != 4; ++i) {
      for (int j = 0; j != numCoefs; ++j) {
        coefs[i][j] = scanner.nextDouble();
        System.out.print(coefs[i][j] + ", ");
      }
      System.out.println();
    }
    System.out.println();
  }

  public static int[][][] prepareInOut(double radius) {
    ArrayList<int[]> in = new ArrayList<int[]>();
    ArrayList<int[]> out = new ArrayList<int[]>();

    for (int y = (int) Math.floor(-radius); y <= RADIUS; ++y)
      for (int x = (int) (Math.floor(-radius)); x <= RADIUS + 1; ++x) {
        boolean a = x * x + y * y <= radius * radius;
        boolean b = (x - 1) * (x - 1) + y * y <= radius * radius;
        if (!a && b)
          in.add(new int[] {x, y});
        if (a && !b)
          out.add(new int[] {x, y});
      }

    return new int[][][] {in.toArray(new int[][] {}), out.toArray(new int[][] {})};
  }

  public static double[][] applyMedianMeanFilter(int[][] image, int[][] mask, double radius,
      double boundLow, double boundHigh) {
    int sx = image[0].length;
    int sy = image.length;

    double[][] result = new double[sy][sx];

    int[][][] inOut = prepareInOut(radius);

    for (int y = 0; y != sy; ++y) {
      System.out.println(y);

      int numPoints = 0;
      TreeMap<Integer, Integer> histogram = new TreeMap<Integer, Integer>();
      for (int yy = (int) Math.floor(y - radius); yy <= y + radius; ++yy)
        for (int xx = (int) Math.floor(-radius); xx <= radius; ++xx)
          if (xx * xx + (yy - y) * (yy - y) <= radius * radius)
            if (xx >= 0 && yy >= 0 && xx < sx && yy < sy && mask[yy][xx] == 0) {
              int val = image[yy][xx];
              if (!histogram.containsKey(val))
                histogram.put(val, 1);
              else
                histogram.put(val, histogram.get(val) + 1);
              ++numPoints;
            }

      for (int x = 0; x != sx; ++x) {
        for (int i = 0; i != 2; ++i)
          for (int j = 0; j != inOut[i].length; ++j) {
            int xx = x + inOut[i][j][0];
            int yy = y + inOut[i][j][1];
            if (xx >= 0 && yy >= 0 && xx < sx && yy < sy && mask[yy][xx] == 0) {
              int val = image[yy][xx];
              if (i == 0) {
                Integer num = histogram.get(val);
                if (num == null)
                  histogram.put(val, 1);
                else
                  histogram.put(val, num + 1);
                ++numPoints;
              } else {
                int num = histogram.get(val);
                if (num == 1)
                  histogram.remove(val);
                else
                  histogram.put(val, num - 1);
                --numPoints;
              }
            }
          }

        if (mask[y][x] == -1)
          continue;

        int sum = 0;
        int weight = 0;
        int counter = 0;
        for (int value : histogram.keySet())
          for (int i = histogram.get(value); i != 0; --i) {
            if (counter >= boundLow * numPoints && counter < boundHigh * numPoints) {
              sum += value;
              ++weight;
            }
            ++counter;
          }

        if (weight != 0)
          result[y][x] = (double) sum / weight;
      }
    }

    return result;
  }

  public static void process(int u, int v) throws IOException {
    int[][] image = Tools.readColorImage(DataTools.DIR + "comp" + LX + LY + "/" + u + "-" + v
        + ".PNG");
    Rectangle[] rectangles = DataTools.readRectangles(DataTools.openReadingDir("obstacles/" + u
        + "-" + v + ".dat"));

    int[][] mask = new int[SY / 2][SX / 2];
    for (int y = 0; y != SY / 2; ++y)
      for (int x = 0; x != SX / 2; ++x)
        if (image[y][x] < 180)
          mask[y][x] = 1;

    for (Rectangle r : rectangles)
      for (int y = r.y; y != r.y + r.height && y != SY; ++y)
        for (int x = r.x; x != r.x + r.width && x != SX; ++x)
          mask[y / 2][x / 2] = -1;

    double[][] result = applyMedianMeanFilter(image, mask, RADIUS, BOUND_LOW, BOUND_HIGH);
    BufferedImage resultImage = Tools.getGreyscaleImageFromMatrix(result);
    Tools.writePNG(resultImage, DataTools.DIR + "median/" + u + "-" + v + ".PNG");
  }

  public static void sum() throws IOException {
    int[][] sum = new int[SY / 2][SX / 2];
    int[][] weights = new int[SY / 2][SX / 2];

    int[] pixel = new int[1];

    for (int v = 0; v != SV; ++v)
      for (int u = 0; u != SU; ++u) {
        System.out.println(u + "-" + v);
        WritableRaster raster = ImageIO.read(
            new File(DataTools.DIR + "median/" + u + "-" + v + ".PNG")).getRaster();
        for (int y = 0; y != SY / 2; ++y)
          for (int x = 0; x != SX / 2; ++x) {
            raster.getPixel(x, y, pixel);
            if (pixel[0] != 0) {
              sum[y][x] += pixel[0];
              ++weights[y][x];
            }
          }
      }

    BufferedImage result = new BufferedImage(SX / 2, SY / 2, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = result.getRaster();
    for (int y = 0; y != SY / 2; ++y)
      for (int x = 0; x != SX / 2; ++x) {
        int val = sum[y][x] / weights[y][x];
        pixel[0] = val;
        //pixel[0] = (val % 2) * 255;
        raster.setPixel(x, y, pixel);
      }
    Tools.writePNG(result, DataTools.DIR + "median/sum.PNG");
  }

  public static void normalize(String filename) throws IOException {
    double[][] matrix = DataTools.readMatrixDouble(DataTools.openReading(filename));
    Tools.scaleMatrix(matrix, 1 / Tools.findMean(matrix));
    DataOutputStream out = DataTools.openWriting(filename);
    DataTools.writeMatrixDouble(out, matrix);
    out.close();
  }
  
  public static void normalize() throws IOException {
    normalize(DataTools.DIR + "light-dist-disk-0-" + 0 + 0 + ".dat");
    normalize(DataTools.DIR + "light-dist-disk-0-" + 1 + 0 + ".dat");
    normalize(DataTools.DIR + "light-dist-disk-0-" + 0 + 1 + ".dat");
    normalize(DataTools.DIR + "light-dist-disk-0-" + 1 + 1 + ".dat");
  }

  public static void sampled() throws IOException {
    double[][] matrix = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR
        + "light-dist-disk-0-" + LX + LY + ".dat"));

    BufferedImage image = new BufferedImage(SX / 2, SY / 2, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image.getRaster();

    Tools.scaleMatrix(matrix, 192 / Tools.findMean(matrix));

    int[] pixel = new int[1];

    for (int y = 0; y != SY / 2; ++y)
      for (int x = 0; x != SX / 2; ++x) {
        pixel[0] = ((int) matrix[y][x] % 2) * 255;
        raster.setPixel(x, y, pixel);
      }

    Tools.writePNG(image, DataTools.DIR + "median/light-dist-disk-0-" + LX + LY + "-diff.png");
  }

  public static int[][] lightDistSum() throws IOException {
    int[][] image = new int[SY][SX];
    int[] pixel = new int[3];

    for (int i = 0; i != 4; ++i) {
      WritableRaster raster = ImageIO.read(new File(DataTools.DIR + "dist" + i + ".png"))
          .getRaster();
      for (int y = 0; y != SY; ++y)
        for (int x = 0; x != SX; ++x) {
          raster.getPixel(x, y, pixel);
          image[y][x] += pixel[x % 2 + y % 2];
        }
    }

    return image;
  }

  public static int[][] experimentalSum() throws IOException {
    int[][] image = new int[SY][SX];
    for (int i = 0; i != 4; ++i) {
      WritableRaster raster = ImageIO.read(
          new File((DataTools.INTERACTIVE ? "/media/book/decapsulation/backup/" : "")
              + "grey-data/P" + (6214307 + i) + ".PNG")).getRaster();
      int[] pixel = new int[1];

      for (int y = 0; y != SY; ++y)
        for (int x = 0; x != SX; ++x) {
          raster.getPixel(x, y, pixel);
          image[y][x] += pixel[0];
        }
    }

    return image;
  }

  public static int[][] getSubMatrix(int[][] matrix) {
    int[][] submatrix = new int[SY / 2][SX / 2];
    for (int y = 0; y != SY / 2; ++y)
      for (int x = 0; x != SX / 2; ++x)
        submatrix[y][x] = matrix[2 * y + LY][2 * x + LX];

    return submatrix;
  }

  public static void viewLightSamples() throws IOException {
    int[][] image = getSubMatrix(experimentalSum());
    Tools.scaleMatrix(image, 192 / Tools.findMean(image));
    Tools.writePNG(Tools.getGreyscaleImageFromMatrix(image), DataTools.DIR + "light" + LX + LY
        + ".png");
  }

  public static void sample() throws IOException {
    int[][] image = getSubMatrix(experimentalSum());

    Rectangle[] rectangles = DataTools.readRectangles(DataTools.DIR + "rectangles2.dat.save");

    int[][] mask = new int[SY / 2][SX / 2];
    for (int y = 0; y != SY / 2; ++y) {
      System.out.println(y);

      for (int x = 0; x != SX / 2; ++x) {
        double xx = 2 * x + LX - 0.5 * (SX + 1);
        double yy = 2 * y + LY - 0.5 * (SY + 1);

        xx *= coefs[2 * LY + LX][0] / coefs[1][0];
        yy *= coefs[2 * LY + LX][0] / coefs[1][0];

        xx += 0.5 * (SX + 1);
        yy += 0.5 * (SY + 1);

        double u = (xx + (yy + 1)) / 2;
        double v = (SY - (yy + 1) + xx) / 2;

        for (Rectangle r : rectangles)
          if (u >= r.x - 0.5 && v >= r.y - 0.5 && u < r.x + r.width - 0.5
              && v < r.y + r.height - 0.5)
            mask[y][x] = 255;
      }
    }

    double[][] result = applyMedianMeanFilter(image, mask, SAMPLE_RADIUS, SAMPLE_BOUND_LOW,
        SAMPLE_BOUND_HIGH);

    DataOutputStream out = DataTools.openWriting(DataTools.DIR + "light-dist-disk-0-" + LX + LY
        + ".dat");
    DataTools.writeMatrixDouble(out, result);
    out.close();
  }

  public static void main(String[] args) throws IOException {
    if (!DataTools.INTERACTIVE) {
      LX = Integer.parseInt(args[0]);
      LY = Integer.parseInt(args[1]);
      SAMPLE_RADIUS = Double.parseDouble(args[2]);
    }

    //readParameters(DataTools.DIR + "stitching/10m5m3mlp.txt");
    //sample();
    //sampled();
    System.exit(0);

    int u0 = 0;
    int v0 = 0;

    int u1 = 5;
    int v1 = 7;

    if (!DataTools.INTERACTIVE) {
      u0 = Integer.parseInt(args[0]);
      v0 = Integer.parseInt(args[1]);

      u1 = Integer.parseInt(args[2]);
      v1 = Integer.parseInt(args[3]);
    }

    for (int v = v0; v != v1; ++v)
      for (int u = u0; u != u1; ++u) {
        System.out.println(u + "-" + v);
        process(u, v);
      }
  }

}
