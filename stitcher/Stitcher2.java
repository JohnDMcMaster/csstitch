package stitcher;

import general.collections.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import data.DataTools;

import tools.SumOfSquares;

public class Stitcher2 {

  public static TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> readControlPoints(
      String filename) throws IOException {
    TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints = new TreeMap<Pair<Integer, Integer>, ArrayList<double[]>>();

    BufferedReader in = new BufferedReader(new FileReader(filename));
    String line;
    while ((line = in.readLine()) != null) {
      if (!line.startsWith("c"))
        continue;

      String[] entries = line.split(" ");

      int n0 = Integer.valueOf(entries[1].substring(1));
      int n1 = Integer.valueOf(entries[2].substring(1));
      Pair<Integer, Integer> p = new Pair<Integer, Integer>(n0, n1);

      double x0 = Double.valueOf(entries[3].substring(1));
      double y0 = Double.valueOf(entries[4].substring(1));

      double x1 = Double.valueOf(entries[5].substring(1));
      double y1 = Double.valueOf(entries[6].substring(1));

      if (!controlPoints.containsKey(p))
        controlPoints.put(p, new ArrayList<double[]>());

      controlPoints.get(p).add(new double[] {x0, y0, x1, y1});
    }

    return controlPoints;
  }

  public static int getNumControlPoints(
      TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints) {
    int num = 0;
    for (ArrayList<double[]> points : controlPoints.values())
      num += points.size();
    return num;
  }

  public static int getNumImages(TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints) {
    int maxIndex = -1;
    for (Pair<Integer, Integer> p : controlPoints.keySet())
      maxIndex = Math.max(maxIndex, Math.max(p.getA(), p.getB()));
    return maxIndex + 1;
  }

  public static double[] stitch(TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints, int lx, int ly, int sx, int sy, int numCoefs) {
    int numImages = getNumImages(controlPoints);
    int numControlPoints = getNumControlPoints(controlPoints);

    double[][] summandRoots = new double[2 * numControlPoints][numCoefs + 2 * (numImages - 1) + 1];

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

        if (p.getA() != 0) {
          summandRoots[2 * i + 0][numCoefs + 2 * (p.getA() - 1) + 0] = 1;
          summandRoots[2 * i + 1][numCoefs + 2 * (p.getA() - 1) + 1] = 1;
        }

        if (p.getB() != 0) {
          summandRoots[2 * i + 0][numCoefs + 2 * (p.getB() - 1) + 0] = -1;
          summandRoots[2 * i + 1][numCoefs + 2 * (p.getB() - 1) + 1] = -1;
        }

        summandRoots[2 * i + 0][numCoefs + 2 * (numImages - 1)] = x1 - x0;
        summandRoots[2 * i + 1][numCoefs + 2 * (numImages - 1)] = y1 - y0;

        ++i;
      }
    }
    
    //TreeSet<Integer> good = SumOfSquares.iterativelySolveSumOfSquares(summandRoots, 1.5, 1000, 0, true);
    return SumOfSquares.solveSumOfSquares(summandRoots, 0, true);
  }

  public static void main(String[] args) throws IOException {
    final int numCoefs = 3;
    
    final int lx = 0;
    final int ly = 0;
    
    final int sx = 3250;
    final int sy = 2450;
    
    TreeMap<Pair<Integer, Integer>, ArrayList<double[]>> controlPoints = readControlPoints(DataTools.DIR
        + "/comp00/out2.pto");
    int numImages = getNumImages(controlPoints);
    System.out.println("read control points from " + numImages + " images");
    
    double[] params = stitch(controlPoints, lx, ly, sx, sy, numCoefs);
  
    System.out.println("optical abberation coefficients:");
    for (int i = 0; i != numCoefs; ++i)
      System.out.println(params[i]);
    System.out.println();
    
    System.out.println("positions:");
    for (int i = 0; i != numImages; ++i) {
      double x = i == 0 ? 0 : params[numCoefs + 2 * (i - 1) + 0];
      double y = i == 0 ? 0 : params[numCoefs + 2 * (i - 1) + 1];
      System.out.println("(" + x + ", " + y + ")");
    }
    System.out.println();
  }

}
