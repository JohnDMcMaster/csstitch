package test;

import general.Streams;
import general.collections.Pair;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.DataOutputStream;
import java.io.File;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

/**
int u = (x + (y + 1)) / 2;
int v = (sy - (y + 1) + x) / 2;

inverse transform:
2 * u = x + y + 1
2 * v = sy - 1 - y + x

x = u + v - sy / 2
y = u - v + sy / 2 - 1
*/

public class Devignetting {

  public static void main(String[] args) throws Exception {
    double factor = 0.9757262;

    BufferedImage source = ImageIO.read(new File(args[0] + ".png"));
    WritableRaster sourceRaster = source.getRaster();

    System.out.println("image read");

    int sx = source.getWidth();
    int sy = source.getHeight();

    double[][] pixels = new double[sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 1) {
          double val = sourceRaster.getPixel(x, y, (double[]) null)[1];
          if (x % 2 == 1)
            val *= factor;
          pixels[y][x] = val + 0.001 * Math.random();
        }

    System.out.println("pixels read");

    Rectangle[] rectangles = DataTools.readRectangles(DataTools.DIR + "rectangles2.dat.save");
    for (Rectangle r : rectangles) {
      for (int v = r.y; v != r.y + r.height; ++v)
        for (int u = r.x; u != r.x + r.width; ++u) {
          int x = u + v - sy / 2;
          int y = u - v + sy / 2 - 1;
          if (x >= 0 && y >= 0 && x < sx && y < sy)
            pixels[y][x] = -1;
        }
    }

    System.out.println("bad pixels set");

    //final double t = 8;
    //final int k = (int) (5 * Math.sqrt(2 * t)) + 1;
    final int k = 20;

    double[][] filter = new double[2 * k + 1][2 * k + 1];
    for (int yy = -k; yy <= k; ++yy)
      for (int xx = -k; xx <= k; ++xx)
        if (xx * xx + yy * yy <= k * k)
          filter[yy + k][xx + k] = 1;// / (2 * Math.PI * t) * Math.exp(-(xx * xx + yy * yy) / (2 * t));

    System.out.println("filter setup");

    double[][] result = new double[sy][sx];

    System.out.println("output matrix prepared");

    int minSize = 10000;
    for (int y = 0; y != sy; ++y) {
      if (y % 100 == 0)
        System.out.println(y);
      for (int x = 0; x != sx; ++x) {
        TreeMap<Double, Double> map = new TreeMap<Double, Double>();
        double weight = 0;
        double value = 0;

        for (int yy = -k; yy <= k; ++yy)
          for (int xx = -k; xx <= k; ++xx) {
            int zx = x + xx;
            int zy = y + yy;
            if (zx >= 0 && zy >= 0 && zx < sx && zy < sy && pixels[zy][zx] > 0
                && filter[yy + k][xx + k] > 0) {
              if (map.put(pixels[zy][zx], filter[yy + k][xx + k]) != null)
                throw new RuntimeException("double key");

              weight += filter[yy + k][xx + k];
              value += filter[yy + k][xx + k] * pixels[zy][zx];
            }
          }

        double temp = value / weight;
        int totalSize = map.size();
        if (totalSize < minSize) {
          minSize = totalSize;
          System.out.println("temporary min size: " + minSize);
        }

        while (3 * map.size() > 2 * totalSize) {
          double a = map.firstKey();
          double b = map.lastKey();
          double c = Math.abs(temp - a) > Math.abs(temp - b) ? a : b;
          double w = map.get(c);
          weight -= w;
          value -= c;
          map.remove(c);
        }

        result[y][x] = value / weight;
      }
    }

    System.out.println("matrix filtered");

    Streams.writeObject(DataTools.DIR + args[0], result);
  }

  static double[] analyze(TreeMap<Pair<Double, Double>, Pair<Double, Double>> sampleMap, int sx,
      int sy) {
    final double[][] samples = new double[sampleMap.size()][4];
    int i = 0;
    for (Pair<Double, Double> point : sampleMap.keySet()) {
      samples[i][0] = point.getA();
      samples[i][1] = point.getB();
      samples[i][2] = sampleMap.get(point).getA();
      samples[i][3] = sampleMap.get(point).getB();
      ++i;
    }

    final double cx = (sx + sy) / 4.0 + 0.01;
    final double cy = (sx + sy - 2) / 4.0 + 0.1;

    final double[] distances = new double[samples.length];
    for (i = 0; i != samples.length; ++i) {
      double xx = samples[i][0] - cx;
      double yy = samples[i][1] - cy;
      distances[i] = Math.sqrt(xx * xx + yy * yy) / 1000;
    }

    double minA = 0;
    double minB = 0;
    double minC = 0;
    double minVal = 1000;
    double minMean = 0;

    double[] factors = new double[samples.length];
    for (double a = 0.1540; a <= 0.1550; a += 0.0001) {
      System.out.println(a);
      for (double b = 0.14530; b <= 0.14550; b += 0.00001) {
        for (double c = -0.07700; c <= -0.07680; c += 0.00001) {
          for (i = 0; i != samples.length; ++i) {
            double d = distances[i];
            factors[i] = 1 + d * (a + d * (b + d * c));
          }
          double val = evaluateFactors(samples, factors);
          if (val < minVal) {
            minA = a;
            minB = b;
            minC = c;
            minVal = val;
            minMean = getMean(samples, factors);
          }
        }
      }
    }

    System.out.println(minA + ", " + minB + ", " + minC + ", " + minVal);

    final double polyA = minA;
    final double polyB = minB;
    final double polyC = minC;
    final double polyMean = minMean;

    Tools.display(new PointChooser(new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_BGR),
        new PointChooser.Handler() {
          public void draw(Graphics g, double zoom) {
            g.setColor(Color.GREEN);
            for (int i = 0; i != samples.length; ++i) {
              int x = (int) (distances[i] * 1000 * zoom);
              int y = (int) (samples[i][3] * zoom);
              g.drawLine(x - 3, y, x + 3, y);
              g.drawLine(x, y - 3, x, y + 3);
            }

            g.setColor(Color.RED);
            for (int x = 0; x != 3000; ++x) {
              double d = x / 1000.0;
              double y = polyMean / (1 + d * (polyA + d * (polyB + d * polyC)));
              g.drawLine((int) (zoom * x), (int) (zoom * y), (int) (zoom * (x + 1)),
                  (int) (zoom * y));
            }
          }

          public boolean click(int button, int x, int y) {
            return false;
          }
        }));

    return new double[] {polyA, polyB, polyC};
  }

  static double getMean(double[][] samples, double[] factors) {
    double a = 0, b = 0;
    for (int i = 0; i != samples.length; ++i) {
      double t = samples[i][3] * factors[i];
      a += samples[i][2] * t;
      b += samples[i][2] * t * t;
    }

    return b / a;
  }

  static double evaluateFactors(double[][] samples, double[] factors) {
    double invMean = 1 / getMean(samples, factors);

    double sum = 0;
    for (int i = 0; i != samples.length; ++i) {
      double t = invMean * samples[i][3] * factors[i] - 1;
      sum += samples[i][2] * t * t;
    }

    return sum;
  }
}
