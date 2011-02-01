package devignetter;

import java.io.IOException;

import data.DataTools;

public class DevigApprox {

  public static final int SX = 3250;
  public static final int SY = 2450;

  public static final double SLOPE = 64;

  public static final int NUM_POINTS_X = (int) (SX / SLOPE) + 1;
  public static final int NUM_POINTS_Y = (int) (SY / SLOPE) + 1;

  public static double[][][][] correlations;
  public static double[][] values = new double[NUM_POINTS_Y][NUM_POINTS_X];
  public static double[][] newValues = new double[NUM_POINTS_Y][NUM_POINTS_X];

  public static double getNorm(double[][] a) {
    double s = 0;
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x)
        s += a[y][x] * a[y][x];
    return Math.sqrt(s);
  }

  public static void setNorm(double[][] a, double t) {
    double factor = Math.sqrt(t) / getNorm(a);
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x)
        a[y][x] *= factor;
  }

  public static double getDistance(double[][] a, double[][] b) {
    double s = 0;
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x) {
        double c = a[y][x] - b[y][x];
        s += c * c;
      }
    return Math.sqrt(s);
  }

  public static void computeNewValues() {
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x) {
        newValues[y][x] = 0;
        double weight = 0;

        for (double[] entry : correlations[y][x]) {
          double x1 = entry[0];
          double y1 = entry[1];

          weight += entry[3];
          newValues[y][x] += entry[2] * values[(int) Math.round(y1)][(int) Math.round(x1)];
        }
        
        newValues[y][x] /= weight;
      }
  }

  public static void main(String[] args) throws IOException {
    correlations = DataTools.readDoubleArrayArrayMatrix(DataTools
        .openReadingDir("stitching/devig-points.dat"));

    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x)
        if (correlations[y][x].length == 0)
          System.out.printf("(%d, %d)\n", x, y);

    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x)
        values[y][x] = 1;

    for (int step = 0;; ++step) {
      computeNewValues();
      setNorm(newValues, NUM_POINTS_X * NUM_POINTS_Y);

      if (step % 100 == 0) {
        double dist = getDistance(values, newValues);
        System.out.println(dist);

        if (dist < 1E-8)
          break;
      }

      double[][] temp = values;
      values = newValues;
      newValues = temp;
    }

    for (int y = 0; y != NUM_POINTS_Y; ++y) {
      for (int x = 0; x != NUM_POINTS_X; ++x)
        System.out.printf("%.1f ", values[y][x]);
      System.out.println();
    }
    System.out.println();
  }

}
