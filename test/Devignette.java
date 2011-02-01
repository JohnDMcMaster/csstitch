package test;

import general.collections.Pair;
import general.Statistics;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class Devignette {

  public static void main(String[] args) throws IOException {
    double factor = 0.9757262;

    BufferedImage source = ImageIO.read(new File(DataTools.DIR + "raw.png"));
    WritableRaster sourceRaster = source.getRaster();

    final int sx = source.getWidth();
    final int sy = source.getHeight();

    BufferedImage image = Tools.getGreenComponent(source, 1);
    final WritableRaster raster = image.getRaster();

    double[][] matrix = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR
        + "light-dist-20.dat"));

    final double[][] rotMatrix = new double[image.getHeight()][image.getWidth()];
    for (int u = 0; u != rotMatrix.length; ++u)
      for (int v = 0; v != rotMatrix[0].length; ++v) {
        rotMatrix[v][u] = raster.getPixel(u, v, (int[]) null)[0] + 0.001 * Math.random();
      }

    double a = 0, b = 0;
    double mi = 1E10, ma = 0;

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 1) {
          int u = (x + (y + 1)) / 2;
          int v = (sy - (y + 1) + x) / 2;

          double val = rotMatrix[v][u];
          if (x % 2 == 1)
            val *= factor;

          val /= matrix[y][x];

          if (x % 2 == 0)
            a += val;
          else
            b += val;

          rotMatrix[v][u] = val;

          if (x != 3089 || y != 1432) {
          if (val < mi)
            mi = val;
          if (val > ma) {
            ma = val;
            System.out.println(ma + ": " + u + ", " + v);
          }}
        }

    final double min = mi;
    final double max = ma;

    System.out.println(a / b);
    System.out.println(min);
    System.out.println(max);

    TreeMap<Pair<Double, Double>, Pair<Double, Double>> points = new TreeMap<Pair<Double, Double>, Pair<Double, Double>>();

    Rectangle[] rectangles = DataTools.readRectangles(DataTools.openReading(DataTools.DIR
        + "rectangles.dat.save"));
    for (Rectangle r : rectangles) {
      int nu = Math.max(1, r.width / 5);
      int nv = Math.max(1, r.height / 5);

      for (int vv = 0; vv != nv; ++vv)
        for (int uu = 0; uu != nu; ++uu) {
          int au = r.x + (uu * r.width) / nu;
          int av = r.y + (vv * r.height) / nv;
          int bu = r.x + ((uu + 1) * r.width) / nu;
          int bv = r.y + ((vv + 1) * r.height) / nv;

          TreeSet<Double> set = new TreeSet<Double>();
          for (int v = av; v != bv; ++v)
            for (int u = au; u != bu; ++u)
              if (!set.add(rotMatrix[v][u]))
                throw new RuntimeException("double key");

          if (set.size() < 12)
            continue;

          for (int i = 0; i != 4; ++i) {
            set.remove(set.first());
            set.remove(set.last());
          }

          double mean = 0;
          for (double d : set)
            mean += d;
          mean /= set.size();

          double zu = 0.5 * (au + bu + 1);
          double zv = 0.5 * (av + bv + 1);

          points.put(new Pair<Double, Double>(zu, zv), new Pair<Double, Double>(
              (double) set.size(), mean));
        }
    }

    analyze(points, sx, sy);

    int maxx = 0;
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 1) {
          int u = (x + (y + 1)) / 2;
          int v = (sy - (y + 1) + x) / 2;

          int val = (int) (65535 * (rotMatrix[v][u] - min) / (max - min));
          if (val > maxx)
            maxx = val;

          raster.setPixel(u, v, new int[] {val});
        }
    System.out.println(maxx);

    ImageIO.write(image, "PNG", new File(DataTools.DIR + "raw-devig.png"));

    Tools.display(new PointChooser(image, new PointChooser.Handler() {

      public void draw(Graphics g, double zoom) {
        g.setColor(Color.RED);
        for (int y = 0; y != sy; ++y)
          for (int x = 0; x != sx; ++x)
            if ((x + y) % 2 == 1) {
              int u = (x + (y + 1)) / 2;
              int v = (sy - (y + 1) + x) / 2;

              if ((int) (65535 * (rotMatrix[v][u] - min) / (max - min)) > 30000) {
                g.drawLine((int) (u * zoom), (int) (v * zoom), (int) ((u + 1) * zoom),
                    (int) (v * zoom));
              }
            }
      }

      public boolean click(int button, int x, int y) {
        return false;
      }
    }));
  }

  static void analyze(TreeMap<Pair<Double, Double>, Pair<Double, Double>> sampleMap, int sx, int sy)
      throws IOException {
    final double[][] samples = new double[sampleMap.size()][4];
    int i = 0;
    for (Pair<Double, Double> point : sampleMap.keySet()) {
      samples[i][0] = point.getA();
      samples[i][1] = point.getB();
      samples[i][2] = sampleMap.get(point).getA();
      samples[i][3] = sampleMap.get(point).getB();
      ++i;
    }

    TreeSet<Double> set = new TreeSet<Double>();
    for (i = 0; i != samples.length; ++i)
      set.add(samples[i][3]);

    PrintStream out = new PrintStream(DataTools.DIR + "set.txt");
    for (double d : set)
      out.println(d);
    out.close();

    double[] factors = new double[samples.length];
    for (i = 0; i != factors.length; ++i)
      factors[i] = 1;

    System.out.println(evaluateFactors(samples, factors) / samples.length);
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
