package test;


import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class Deraw {

  final static int EDGE = 100;

  final static double[][] ABERRATION_COEFS = { {1 / 1.00070, 1 / 1.00055},
      {1 / 1.00000, 1 / 1.00000}, {1 / 0.99898, 1 / 0.99898}};

  final static int[][] DEAD_PIXELS = { {2782, 575}, {3089, 1432}};

  public static short[][][] deraw(double[][] image, double[] means) {
    int sx = image[0].length - 2 * EDGE;
    int sy = image.length - 2 * EDGE;

    short[][][] result = new short[sy][sx][3];

    int[][] points = new int[4][2];

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int rxc = x - sx / 2;
        int ryc = y + sy / 2;

        for (int i = 0; i != 3; ++i) {
          double rx = rxc * ABERRATION_COEFS[i][0] + sx / 2 + EDGE;
          double ry = ryc * ABERRATION_COEFS[i][1] + sy / 2 + EDGE;

          if (i == 0) {
            int x0 = 2 * (int) (Math.floor(rx / 2));
            int y0 = 2 * (int) (Math.floor(ry / 2));

            int x1 = 2 * (int) (Math.ceil(rx / 2));
            int y1 = 2 * (int) (Math.ceil(ry / 2));

            points[0][0] = x0;
            points[0][1] = y0;

            points[1][0] = x0;
            points[1][1] = y1;

            points[2][0] = x1;
            points[2][1] = y1;

            points[3][0] = x1;
            points[3][1] = y0;
          } else if (i == 2) {
            int x0 = 2 * (int) (Math.floor((rx - 1) / 2)) + 1;
            int y0 = 2 * (int) (Math.floor((ry - 1) / 2)) + 1;

            int x1 = 2 * (int) (Math.ceil((rx - 1) / 2)) + 1;
            int y1 = 2 * (int) (Math.ceil((ry - 1) / 2)) + 1;

            points[0][0] = x0;
            points[0][1] = y0;

            points[1][0] = x0;
            points[1][1] = y1;

            points[2][0] = x1;
            points[2][1] = y1;

            points[3][0] = x1;
            points[3][1] = y0;
          } else if (i == 1) {
            // in knowledge of ABERRITION_COEFS[1][i] == 1 ...
            int xm = (int) rx;
            int ym = (int) ry;

            boolean dead = false;
            for (int k = 0; k != DEAD_PIXELS.length; ++k)
              if (xm == DEAD_PIXELS[k][0] && xm == DEAD_PIXELS[k][1])
                dead = true;

            if (dead) {
              points[0][0] = xm - 1;
              points[0][1] = ym - 1;

              points[1][0] = xm - 1;
              points[1][1] = ym + 1;

              points[2][0] = xm + 1;
              points[2][1] = ym + 1;

              points[3][0] = xm + 1;
              points[3][1] = ym - 1;
            } else {
              int k = (xm + ym + 1) % 2;

              points[0][0] = xm - k;
              points[0][1] = ym;

              points[1][0] = xm;
              points[1][1] = ym - k;

              points[2][0] = xm + k;
              points[2][1] = ym;

              points[3][0] = xm;
              points[3][1] = ym + k;
            }
          }

          double totalWeight = 0;
          double value = 0;
          outer: for (int j = 0; j != 4; ++j) {
            for (int k = 0; k != DEAD_PIXELS.length; ++k)
              if (points[j][0] == DEAD_PIXELS[k][0] && points[j][1] == DEAD_PIXELS[k][1])
                continue outer;

            double dx = rx - points[j][0];
            double dy = ry - points[j][1];

            double weight = dx * dx + dy * dy;
            totalWeight += weight;
            value += weight * image[points[j][1]][points[j][0]];
          }
          value /= (totalWeight * means[i]);

          value *= 128;
          if (value < 256)
            result[y][x][i] = (short) (value);
          else
            result[y][x][i] = 255;
        }
      }

    return result;
  }

  public static BufferedImage derawToImage(double[][] image, double[] means) {
    int sx = image[0].length - 2 * EDGE;
    int sy = image.length - 2 * EDGE;

    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = result.getRaster();

    int[][] points = new int[4][2];
    int[] values = new int[3];

    for (int y = 0; y != sy; ++y) {
      for (int x = 0; x != sx; ++x) {
        int rxc = x - sx / 2;
        int ryc = y - sy / 2;

        for (int i = 0; i != 3; ++i) {
          double rx = rxc * ABERRATION_COEFS[i][0] + sx / 2 + EDGE;
          double ry = ryc * ABERRATION_COEFS[i][1] + sy / 2 + EDGE;

          if (i == 0) {
            int x0 = 2 * (int) (Math.floor(rx / 2));
            int y0 = 2 * (int) (Math.floor(ry / 2));

            int x1 = 2 * (int) (Math.ceil(rx / 2));
            int y1 = 2 * (int) (Math.ceil(ry / 2));

            points[0][0] = x0;
            points[0][1] = y0;

            points[1][0] = x0;
            points[1][1] = y1;

            points[2][0] = x1;
            points[2][1] = y1;

            points[3][0] = x1;
            points[3][1] = y0;
          } else if (i == 2) {
            int x0 = 2 * (int) (Math.floor((rx - 1) / 2)) + 1;
            int y0 = 2 * (int) (Math.floor((ry - 1) / 2)) + 1;

            int x1 = 2 * (int) (Math.ceil((rx - 1) / 2)) + 1;
            int y1 = 2 * (int) (Math.ceil((ry - 1) / 2)) + 1;

            points[0][0] = x0;
            points[0][1] = y0;

            points[1][0] = x0;
            points[1][1] = y1;

            points[2][0] = x1;
            points[2][1] = y1;

            points[3][0] = x1;
            points[3][1] = y0;
          } else if (i == 1) {
            // in the knowledge of ABERRITION_COEFS[1][i] == 1 ...
            int xm = (int) rx;
            int ym = (int) ry;

            int k = (xm + ym + 1) % 2;

            points[0][0] = xm - k;
            points[0][1] = ym;

            points[1][0] = xm;
            points[1][1] = ym - k;

            points[2][0] = xm + k;
            points[2][1] = ym;

            points[3][0] = xm;
            points[3][1] = ym + k;
          }

          double totalWeight = 0;
          double value = 0;
          for (int j = 0; j != 4; ++j) {
            double dx = rx - points[j][0];
            double dy = ry - points[j][1];

            double weight = dx * dx + dy * dy;
            if (j == 3 && totalWeight == 0 && weight == 0) // HACK for green pixels
              weight = 1;

            totalWeight += weight;
            value += weight * image[points[j][1]][points[j][0]];
          }
          value /= (totalWeight * means[i]);

          value *= 128;
          if (value < 256)
            values[i] = (short) (value);
          else
            values[i] = 255;
        }

        raster.setPixel(x, y, values);
      }
    }

    return result;
  }

  //675.26785732587
  //819.6843105031993
  //409.90028051563456
  public static double[] findNoMetalMeans() throws IOException {
    double[] means = new double[3];

    for (int y = 0; y != 7; ++y)
      for (int x = 0; x != 5; ++x) {
        System.out.println(x);

        double[][] image = Tools.evenLighting(DataTools.DIR + "jpegs/" + x + "-" + y + ".jpg",
            new double[] {128., 128., 128.});

        int sx = image[0].length;
        int sy = image.length;

        double[] interMeans = new double[3];
        for (int yy = 0; yy != sy; ++yy)
          for (int xx = 0; xx != sx; ++xx)
            interMeans[(xx % 2) + (yy % 2)] += image[yy][xx];

        for (int i = 0; i != 3; ++i)
          means[i] += interMeans[i] / ((sx / 2) * (sy / 2) * (1 + (i % 2)));
      }

    for (int i = 0; i != 3; ++i)
      means[i] /= 5 * 7;

    return means;
  }

  public static void derawNoMetal() throws IOException {
    //double[] means = findNoMetalMeans();
    double[] means = {591.4538795317337, 727.1132002547656, 359.0503858694775};

    for (int y = 0; y != 7; ++y)
      for (int x = 0; x != 5; ++x) {
        System.out.println(x);

        double[][] image = Tools.getMatrixFromImage(ImageIO.read(new File(DataTools.DIR + "images/"
            + x + "-" + y + ".png")));
        BufferedImage result = derawToImage(image, means);
        ImageIO.write(result, "PNG", new File(DataTools.DIR + "deraw/" + x + "-" + y + ".png"));
      }
  }

  public static void main(String[] args) throws IOException {
    long a = System.currentTimeMillis();
    double[] means = findNoMetalMeans();
    long b = System.currentTimeMillis();
    System.out.println(0.001 * (b - a));

    for (int i = 0; i != 3; ++i)
      System.out.println(means[i]);
  }

}
