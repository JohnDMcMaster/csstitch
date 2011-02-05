package stitcher;

import general.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import tools.SumOfSquares;

import data.DataTools;

public class Stitcher3 {
  
  public static final int SU = 5;
  public static final int SV = 6;

  public static void combineKeypoints(String dir) throws IOException {
    PrintStream out = new PrintStream(dir + "/combined.pto");

    for (int y = 0; y != SV; ++y)
      for (int x = 0; x != SU; ++x)
        out.println("i w1625 h1225 f0 a0 b0 c0 d0 e0 p0 r0 v180 y0  u10 n\"" + x + "-" + y
            + ".PNG\"");
    out.println();

    for (String filename : new File(dir).list())
      if (filename.startsWith(dir.substring(dir.length() - 2)) && filename.endsWith(".pto")) {
        System.out.println(filename);

        int x0 = Integer.parseInt(filename.substring(3, 4));
        int y0 = Integer.parseInt(filename.substring(4, 5));

        int x1 = Integer.parseInt(filename.substring(6, 7));
        int y1 = Integer.parseInt(filename.substring(7, 8));

        int n0 = 5 * y0 + x0;
        int n1 = 5 * y1 + x1;

        for (double[] point : Stitcher.readControlPoints(dir + "/" + filename))
          out.println("c n" + n0 + " N" + n1 + " x" + point[0] + " y" + point[1] + " X" + point[2]
              + " Y" + point[3] + " t0");
        out.println();
      }

    out.close();
  }

  public static double[][] prepareSummandRoots(double[][] controlPoints, int lx, int ly, int sx,
      int sy, int numCoefs) throws IOException {
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

    return summandRoots;
  }

  public static double[][] prepareFixedLenseSummandRootsCoord(
      TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints, int lx, int ly, int sx,
      int sy, double[] coefs, int coord) {
    int numImages = Stitcher2.getNumImages(controlPoints);
    int numControlPoints = Stitcher2.getNumControlPoints(controlPoints);

    double[][] summandRoots = new double[numControlPoints][numImages];
    int i = 0;

    for (Pair<Integer, Integer> p : controlPoints.keySet()) {
      ArrayList<double[]> points = controlPoints.get(p);
      for (double[] point : points) {
        double x0 = 2 * point[0] + lx - 0.5 * (sx + 1);
        double y0 = 2 * point[1] + ly - 0.5 * (sy + 1);

        double x1 = 2 * point[2] + lx - 0.5 * (sx + 1);
        double y1 = 2 * point[3] + ly - 0.5 * (sy + 1);

        double r0 = Math.sqrt(x0 * x0 + y0 * y0);
        double r1 = Math.sqrt(x1 * x1 + y1 * y1);

        double r0pow = r0;
        double r1pow = r1;

        double factor0 = 1;
        double factor1 = 1;

        for (int j = 0; j != coefs.length; ++j) {
          factor0 += coefs[j] * r0pow;
          factor1 += coefs[j] * r1pow;

          r0pow *= r0;
          r1pow *= r1;
        }

        x0 *= factor0;
        y0 *= factor0;

        x1 *= factor1;
        y1 *= factor1;

        if (p.getA() != 0)
          summandRoots[i][p.getA() - 1] = 1;

        if (p.getB() != 0)
          summandRoots[i][p.getB() - 1] = -1;

        summandRoots[i][numImages - 1] = coord == 0 ? x1 - x0 : y1 - y0;

        ++i;
      }
    }

    return summandRoots;

    //TreeSet<Integer> good = SumOfSquares.iterativelySolveSumOfSquares(summandRoots, 1.5, 1000, 0, true);
    //return SumOfSquares.solveSumOfSquares(summandRoots, 0, true);
  }

  public static double[][] prepareFixedLenseSummandRootsTwoImages(double[][] controlPoints, int lx,
      int ly, int sx, int sy, double[] coefs) {
    int n = controlPoints.length;
    double[][] summandRoots = new double[2 * n][3];

    for (int i = 0; i != n; ++i) {
      double x0 = 2 * controlPoints[i][0] + lx - 0.5 * (sx + 1);
      double y0 = 2 * controlPoints[i][1] + ly - 0.5 * (sy + 1);

      double x1 = 2 * controlPoints[i][2] + lx - 0.5 * (sx + 1);
      double y1 = 2 * controlPoints[i][3] + ly - 0.5 * (sy + 1);

      double r0 = Math.sqrt(x0 * x0 + y0 * y0);
      double r1 = Math.sqrt(x1 * x1 + y1 * y1);

      double r0pow = r0;
      double r1pow = r1;

      double factor0 = 1;
      double factor1 = 1;

      for (int j = 0; j != coefs.length; ++j) {
        factor0 += coefs[j] * r0pow;
        factor1 += coefs[j] * r1pow;

        r0pow *= r0;
        r1pow *= r1;
      }

      x0 *= factor0;
      y0 *= factor0;

      x1 *= factor1;
      y1 *= factor1;

      summandRoots[2 * i + 0][2] = x1 - x0;
      summandRoots[2 * i + 1][2] = y1 - y0;

      summandRoots[2 * i + 0][0] = -1;
      summandRoots[2 * i + 1][1] = -1;
    }

    return summandRoots;
  }

  public static double[][] prepareFixedCoordsSummandRoots(
      TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints, int lx, int ly, int sx,
      int sy, double[][] positions, int numCoefs) {
    int numImages = Stitcher2.getNumImages(controlPoints);
    int numControlPoints = Stitcher2.getNumControlPoints(controlPoints);

    double[][] summandRoots = new double[2 * numControlPoints][numCoefs + 1];
    int i = 0;

    for (Pair<Integer, Integer> p : controlPoints.keySet()) {
      ArrayList<double[]> points = controlPoints.get(p);
      for (double[] point : points) {
        double x0 = 2 * point[0] + lx - 0.5 * (sx + 1);
        double y0 = 2 * point[1] + ly - 0.5 * (sy + 1);

        double x1 = 2 * point[2] + lx - 0.5 * (sx + 1);
        double y1 = 2 * point[3] + ly - 0.5 * (sy + 1);

        double r0 = Math.sqrt(x0 * x0 + y0 * y0);
        double r1 = Math.sqrt(x1 * x1 + y1 * y1);

        double r0pow = r0;
        double r1pow = r1;

        for (int j = 0; j != numCoefs; ++j) {
          summandRoots[2 * i + 0][j] = x1 * r1pow - x0 * r0pow;
          summandRoots[2 * i + 1][j] = y1 * r1pow - y0 * r0pow;

          r0pow *= r0;
          r1pow *= r1;
        }

        summandRoots[2 * i + 0][numCoefs] = (x1 - positions[p.getB()][0])
            - (x0 - positions[p.getA()][0]);
        summandRoots[2 * i + 1][numCoefs] = (y1 - positions[p.getB()][1])
            - (y0 - positions[p.getA()][1]);

        ++i;
      }
    }

    return summandRoots;
  }

  public static double[][] estimatePositions(
      TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints, int lx, int ly, int sx,
      int sy, double[] coefs) {
    int su = 0, sv = 0;
    for (Pair<Integer, Integer> p : controlPoints.keySet()) {
      su = Math.max(su, p.getA() % 5 + 1);
      su = Math.max(su, p.getB() % 5 + 1);

      sv = Math.max(su, p.getA() / 5 + 1);
      sv = Math.max(su, p.getB() / 5 + 1);
    }

    double[][] summandRootsX = prepareFixedLenseSummandRootsCoord(controlPoints, lx, ly, sx, sy,
        coefs, 0);
    double[][] summandRootsY = prepareFixedLenseSummandRootsCoord(controlPoints, lx, ly, sx, sy,
        coefs, 1);

    double[] positionsX = SumOfSquares.solveSumOfSquares(summandRootsX, 0, false);
    double[] positionsY = SumOfSquares.solveSumOfSquares(summandRootsY, 0, false);

    double[][] positions = new double[positionsX.length + 1][2];
    for (int i = 0; i != positions.length; ++i) {
      positions[i][0] = i == 0 ? 0 : positionsX[i - 1];
      positions[i][1] = i == 0 ? 0 : positionsY[i - 1];
    }

    System.out.println("positions:");
    for (int v = 0; v != sv; ++v) {
      for (int u = 0; u != su; ++u)
        System.out.print("(" + positions[su * v + u][0] + ", " + positions[su * v + u][1] + "); ");
      System.out.println();
    }

    double[] errorsX = SumOfSquares.computeErrors(summandRootsX, coefs);
    double[] errorsY = SumOfSquares.computeErrors(summandRootsY, coefs);

    double[] errors = SumOfSquares.mergeErrors2D(errorsX, errorsY);
    SumOfSquares.outputErrorInfo(errors);
    System.out.println();

    return positions;
  }

  public static double[] estimateLenseParameters(
      TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints, int lx, int ly, int sx,
      int sy, double[][] positions, int numCoefs) {
    double[][] summandRoots = prepareFixedCoordsSummandRoots(controlPoints, lx, ly, sx, sy,
        positions, numCoefs);

    double[] coefs = SumOfSquares.solveSumOfSquares(summandRoots, 0, false);

    System.out.println("coefficients: ");
    for (int i = 0; i != coefs.length; ++i)
      System.out.print(coefs[i] + ", ");
    System.out.println();

    double[] errors = SumOfSquares.computeErrors2D(summandRoots, coefs);

    SumOfSquares.outputErrorInfo(errors);
    System.out.println();

    return coefs;
  }

  public static void writeDoubleMatrixAndQuit(String filename, double[][] a) throws IOException {
    PrintStream out = new PrintStream(DataTools.DIR + filename);
    for (int i = 0; i != a.length; ++i) {
      for (int j = 0; j != a[i].length; ++j)
        out.print(a[i][j] + ", ");
      out.println();
    }
    out.close();
    System.exit(0);
  }

  public static void main(String[] args) throws IOException {
    //final double[] coefsFirstRun = {0.0, -18.E-10, 0.0};
    final double[] coefsFirstRun = {0.0, 0, 0.0};

    final int sx = 3250;
    final int sy = 2450;

    final int lx = 0;
    final int ly = 0;

    combineKeypoints(DataTools.DIR + "comp-ppu-10");
    //combineKeypoints(DataTools.DIR + "comp-cpu-3-10");
    //combineKeypoints(DataTools.DIR + "comp-cpu-3-01");
    //combineKeypoints(DataTools.DIR + "comp-cpu-3-11");
    System.exit(0);

    TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints = Stitcher2
        .readControlPoints(DataTools.DIR + "comp00/combined-0.90.pto");

    for (Pair<Integer, Integer> p : controlPoints.keySet()) {
      int x0 = p.getA() % 5;
      int y0 = p.getA() / 5;

      int x1 = p.getB() % 5;
      int y1 = p.getB() / 5;

      System.out.println(x0 + "" + y0 + " - " + x1 + "" + y1);

      double[][] points = controlPoints.get(p).toArray(new double[][] {});
      double[][] summandRoots = prepareFixedLenseSummandRootsTwoImages(points, lx, ly, sx, sy,
          coefsFirstRun);

      System.out.println(points.length + " points");

      TreeSet<Integer> good = SumOfSquares.iterativelySolveSumOfSquares2D(summandRoots, 5, 0, 0,
          false);
      //double[] params = SumOfSquares.solveSumOfSquares2D(summandRoots, good, 0, false);
      //System.out.print("optical parameters: ");
      //for (int i = 0; i != numCoefsFirstRun; ++i)
      //  System.out.print(params[i] + ", ");
      //System.out.println();
      //System.out.println("positions: " + params[numCoefsFirstRun] + ", "
      //    + params[numCoefsFirstRun + 1]);

      //double[] errors = new double[summandRoots.length / 2];
      //int maxIndex = SumOfSquares.computeErrors2D(summandRoots, good, params, errors);

      System.out.println(good.size() + " points remaining");
      //System.out.println("max error: " + errors[maxIndex]);
      //System.out.println();

      ArrayList<double[]> filteredPoints = new ArrayList<double[]>();
      for (int i : good)
        filteredPoints.add(points[i]);
      controlPoints.put(p, filteredPoints);
    }

    final int numCoefs = 3;

    double[] coefs = coefsFirstRun.clone();
    double[][] positions;

    for (int i = 0; i != 100; ++i) {
      positions = estimatePositions(controlPoints, lx, ly, sx, sy, coefs);
      coefs = estimateLenseParameters(controlPoints, lx, ly, sx, sy, positions, numCoefs);
    }

    /*final int limx = 5;
    final int limy = 7;

    for (Iterator<Pair<Integer, Integer>> i = controlPoints.keySet().iterator(); i.hasNext();) {
      Pair<Integer, Integer> p = i.next();

      int x0 = p.getFirst() % 5;
      int y0 = p.getFirst() / 5;

      int x1 = p.getSecond() % 5;
      int y1 = p.getSecond() / 5;

      if (x0 >= limx || y0 >= limy || x1 >= limx || y1 >= limy)
        i.remove();
    }

    int n = Stitcher2.getNumControlPoints(controlPoints);
    System.out.println(n + " control points remaining");

    double[] coefs = {0.0, 0.0, 0.0};
    for (int z = 0; z != 10; ++z) {
      coefs[1] = -18.E-10 - 1E-11 * z;
      System.out.println(z);

      System.out.println("preparing summand roots...");
      double[][] summandRootsX = prepareFixedLenseSummandRootsCoord(controlPoints, lx, ly, sx, sy,
          coefs, 0);
      double[][] summandRootsY = prepareFixedLenseSummandRootsCoord(controlPoints, lx, ly, sx, sy,
          coefs, 1);

      System.out.println("solving systems...");
      System.out.println("x...");
      double[] paramsX = SumOfSquares.solveSumOfSquares(summandRootsX, 0, false);
      System.out.println("y...");
      double[] paramsY = SumOfSquares.solveSumOfSquares(summandRootsY, 0, false);

      System.out.println("calculating errors...");
      double[] errorsX = SumOfSquares.computeErrors(summandRootsX, paramsX);
      double[] errorsY = SumOfSquares.computeErrors(summandRootsY, paramsY);

      double mean = 0, deviation = 0, max = 0;
      for (int i = 0; i != n; ++i) {
        double error = Math.sqrt(errorsX[i] * errorsX[i] + errorsY[i] * errorsY[i]);
        if (error > max)
          max = error;

        mean += error;
        deviation += error * error;
      }
      mean = mean / n;
      deviation = Math.sqrt(deviation / n);
      System.out.println("mean " + mean + ", deviation " + deviation + ", max " + max);

      System.out.println();
    }*/
  }
}
