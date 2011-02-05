package stitcher;

import general.collections.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import tools.LinearEquation;
import tools.SumOfSquares;

import data.DataTools;

public class Stitcher {

  public static double[][] readControlPoints(String filename) throws IOException {
    ArrayList<double[]> controlPoints = new ArrayList<double[]>();
    BufferedReader in = new BufferedReader(new FileReader(filename));
    String line;
    while ((line = in.readLine()) != null) {
      if (!line.startsWith("c"))
        continue;

      String[] entries = line.split(" ");
      double sx = Double.valueOf(entries[3].substring(1));
      double sy = Double.valueOf(entries[4].substring(1));
      double dx = Double.valueOf(entries[5].substring(1));
      double dy = Double.valueOf(entries[6].substring(1));

      controlPoints.add(new double[] {sx, sy, dx, dy});
    }

    return controlPoints.toArray(new double[][] {});
  }

  final static int sx = 3250;
  final static int sy = 2450;

  static int lx = 0;
  static int ly = 0;

  public static double[] computeRadialDistortion(double[][] controlPoints, int numCoefs,
      int precision) throws IOException {    
    int n = controlPoints.length;
    double[][] summandRoots = new double[2 * n][numCoefs + 3];

    for (int i = 0; i != n; ++i) {
      double x0 = 2 * controlPoints[i][0] + lx - 0.5 * (sx + 1);
      double y0 = 2 * controlPoints[i][1] + ly - 0.5 * (sy + 1);

      double x1 = 2 * controlPoints[i][2] + lx - 0.5 * (sx + 1);
      double y1 = 2 * controlPoints[i][3] + ly - 0.5 * (sy + 1);

      double r0sq = Math.sqrt(x0 * x0 + y0 * y0);
      double r1sq = Math.sqrt(x1 * x1 + y1 * y1);

      for (int j = 0; j != numCoefs; ++j) {
        summandRoots[2 * i + 0][j] = x1 * Math.pow(r1sq, j + 1) - x0 * Math.pow(r0sq, j + 1);
        summandRoots[2 * i + 1][j] = y1 * Math.pow(r1sq, j + 1) - y0 * Math.pow(r0sq, j + 1);
      }

      summandRoots[2 * i + 0][numCoefs + 2] = x1 - x0;
      summandRoots[2 * i + 1][numCoefs + 2] = y1 - y0;

      summandRoots[2 * i + 0][numCoefs + 0] = -1;
      summandRoots[2 * i + 1][numCoefs + 1] = -1;
    }
    
    return SumOfSquares.solveSumOfSquares(summandRoots, precision, false);
  }

  public static double[] computeErrors(double[][] controlPoints, double[] params) {
    int numCoefs = params.length - 2;

    double dx = params[numCoefs + 0];
    double dy = params[numCoefs + 1];

    double[] errors = new double[controlPoints.length];
    for (int i = 0; i != controlPoints.length; ++i) {
      double x0 = 2 * controlPoints[i][0] + lx - 0.5 * (sx + 1);
      double y0 = 2 * controlPoints[i][1] + ly - 0.5 * (sy + 1);

      double x1 = 2 * controlPoints[i][2] + lx - 0.5 * (sx + 1);
      double y1 = 2 * controlPoints[i][3] + ly - 0.5 * (sy + 1);

      double r0sq = Math.sqrt(x0 * x0 + y0 * y0);
      double r1sq = Math.sqrt(x1 * x1 + y1 * y1);

      double px = x1 - x0 - dx;
      double py = y1 - y0 - dy;
      for (int j = 0; j != numCoefs; ++j) {
        px += params[j] * (x1 * Math.pow(r1sq, j + 1) - x0 * Math.pow(r0sq, j + 1));
        py += params[j] * (y1 * Math.pow(r1sq, j + 1) - y0 * Math.pow(r0sq, j + 1));
      }

      errors[i] = Math.sqrt(px * px + py * py);
    }

    return errors;
  }

  public static double[][] getSubset(double[][] values, Set<Integer> set,
      Map<Integer, Integer> from, Map<Integer, Integer> to) {
    double[][] result = new double[set.size()][];

    int index = 0;
    for (Integer i : set) {
      result[index] = values[i];

      from.put(index, i);
      to.put(i, index);

      ++index;
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    final int numCoefs = 3;

    double[][] controlPoints = Stitcher2.readControlPoints(DataTools.DIR + "comp00/combined-0.90.pto")
        .get(new Pair<Integer, Integer>(23, 28)).toArray(new double[][] {});
    //double[][] controlPoints = readControlPoints(DataTools.DIR + "comp00/00-34-35.pto");
    //double[][] controlPoints = readControlPoints(DataTools.DIR + "cp/" + lx + ly + "-34-35-fine");
    int numPoints = controlPoints.length;
    System.out.println(numPoints + " control points");

    TreeSet<Integer> good = new TreeSet<Integer>();
    for (int i = 0; i != numPoints; ++i)
      good.add(i);

    while (good.size() > 0.5 * numPoints) {
      TreeMap<Integer, Integer> from = new TreeMap<Integer, Integer>();
      TreeMap<Integer, Integer> to = new TreeMap<Integer, Integer>();

      double[][] subsetControlPoints = getSubset(controlPoints, good, from, to);

      double[] params = computeRadialDistortion(subsetControlPoints, numCoefs, 0);
      double[] errors = computeErrors(subsetControlPoints, params);

      double meanError = 0;
      double meanVariance = 0;
      double maxError = 0;
      int maxIndex = 0;
      for (int i = 0; i != errors.length; ++i) {
        meanError += errors[i];
        meanVariance += errors[i] * errors[i];

        if (errors[i] > maxError) {
          maxError = errors[i];
          maxIndex = i;
        }
      }
      meanError /= errors.length;
      meanVariance /= errors.length;

      good.remove(from.get(maxIndex));

      System.out.println("dx " + params[numCoefs] + ", dy " + params[numCoefs + 1]);
      System.out.print("barrel coefs: ");
      for (int i = 0; i != numCoefs; ++i)
        System.out.print(params[i] + ", ");
      System.out.println();
      System.out.println("errors: mean " + meanError + ", mean deviation "
          + Math.sqrt(meanVariance) + ", max " + maxError);
      System.out.println();
    }
  }

}
