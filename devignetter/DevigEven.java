package devignetter;

import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import stitcher.StitchInfo;
import tools.LinearEquation;

import data.DataTools;
import distributed.Bootstrap;

public class DevigEven {

  public static final int STITCH = 0;

  public static final int LENGTH = 20;
  public static final int CHANNEL = 2;

  public static final double BOUND_LOW = 0.4;
  public static final double BOUND_HIGH = 0.6;

  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    System.out.println(STITCH);

    WritableRaster image = ImageIO.read(
        new File(DataTools.DIR + "stitching" + StitchInfo.SUFFICES[STITCH]
            + "/modern-filtering-10m7m5m3mlpn.png")).getRaster();
    Rectangle[] rectangles = DataTools.readRectangles(DataTools.openReading(DataTools.DIR
        + "even-lighting-rects" + StitchInfo.SUFFICES[STITCH] + ".dat"));

    double[][] matrix = new double[3][3];
    double[] constants = new double[3];

    int[] pixel = new int[3];
    double[] values = new double[4];

    for (Rectangle r : rectangles) {
      System.out.println(r);

      r.x *= 4;
      r.y *= 4;

      r.width *= 4;
      r.height *= 4;

      int nx = r.width / LENGTH;
      int ny = r.height / LENGTH;

      for (int a = 0; a != nx; ++a)
        for (int b = 0; b != ny; ++b) {
          int x0 = r.x + (a * r.width) / LENGTH;
          int y0 = r.y + (b * r.height) / LENGTH;

          int x1 = r.x + ((a + 1) * r.width) / LENGTH;
          int y1 = r.y + ((b + 1) * r.height) / LENGTH;

          TreeMap<Integer, Integer> histogram = new TreeMap<Integer, Integer>();
          for (int y = y0; y != y1; ++y)
            for (int x = x0; x != x1; ++x) {
              int val = image.getPixel(x, y, pixel)[CHANNEL];
              Integer num = histogram.get(val);
              if (num == null)
                histogram.put(val, 1);
              else
                histogram.put(val, num + 1);
            }

          int numPoints = (x1 - x0) * (y1 - y0);
          int sum = 0;
          int weight = 0;
          int counter = 0;
          for (int val : histogram.keySet())
            for (int i = histogram.get(val); i != 0; --i) {
              if (counter >= BOUND_LOW * numPoints && counter < BOUND_HIGH * numPoints) {
                sum += val;
                ++weight;
              }
              ++counter;
            }

          double val = Math.log((double) sum / weight);
          double xx = 0.5 * (x1 + x0 - 1);
          double yy = 0.5 * (y1 + y0 - 1);

          System.out.println("(" + xx + ", " + yy + "): " + val);

          values[0] = xx;
          values[1] = yy;
          values[2] = 1;
          values[3] = -val;

          for (int i = 0; i != 3; ++i) {
            for (int j = 0; j != 3; ++j)
              matrix[i][j] += values[i] * values[j];
            constants[i] -= values[i] * values[3];
          }
        }
    }

    double[] params = LinearEquation.solveLinearEquation(matrix, constants);
    for (int i = 0; i != 3; ++i)
      System.out.println(params[i]);
  }

}
