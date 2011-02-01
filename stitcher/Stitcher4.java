package stitcher;

import general.collections.Pair;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import data.DataTools;

import tools.LinearEquation;
import tools.SumOfSquares;

public class Stitcher4 {
  
  public static int STITCH = 1;
  
  public static final int NUM_COEFS = 3;
  public static double[][] LINEAR_FACTORS_START = { {1.0, 0.99929}, {0.99929, 0.99823}};
  
  public static final int NUM_IMAGES = StitchInfo.NUM_IMAGES[STITCH];
  
  public static final int SX = 3250;
  public static final int SY = 2450;
  
  public static TreeMap<Pair<Integer, Integer>, ArrayList<double[]>>[][] controlPoints;
  
  public static double[][] factors;
  public static double[] errors;
  
  public static double[] values = new double[2 * NUM_IMAGES + 4 * NUM_COEFS];
  
  public static TreeMap<Pair<Integer, Integer>, TreeSet<Integer>>[][] overlaps;
  
  public static TreeSet<Integer> scaleSelection(Set<Integer> selection) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    for (int i : selection) {
      result.add(2 * i + 0);
      result.add(2 * i + 1);
    }
    return result;
  }
  
  public static void computeErrors(Set<Integer> selection) {
    for (int i = 0; i != errors.length; ++i)
      errors[i] = 0;
    
    for (int i : selection) {
      for (int j = 0; j != 2; ++j) {
        double a = 0;
        for (int k = 0; k != values.length; ++k)
          a += factors[2 * i + j][k] * values[k];
        errors[i] += a * a;
      }
      errors[i] = Math.sqrt(errors[i]);
    }
  }
  
  public static double[][]
      prepareSummandRoots(Set<Integer> scaledSelection, Set<Integer> variables) {
    double[][] summandRoots = new double[scaledSelection.size()][variables.size() + 1];
    int i = 0;
    for (int j : scaledSelection) {
      summandRoots[i][variables.size()] = 0;
      
      int k = 0;
      for (int l = 0; l != values.length; ++l)
        if (variables.contains(l))
          summandRoots[i][k++] = factors[j][l];
        else
          summandRoots[i][variables.size()] += factors[j][l] * values[l];
      
      ++i;
    }
    
    return summandRoots;
  }
  
  public static void sieveOverlaps(Set<Integer> selection, double errorThreshold, boolean verbose) {
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        if (verbose)
          System.out.println("subpixel " + lx + ly + ":");
        
        for (Pair<Integer, Integer> p : overlaps[ly][lx].keySet()) {
          TreeSet<Integer> variables = new TreeSet<Integer>();
          variables.add(2 * p.getB() + 0);
          variables.add(2 * p.getB() + 1);
          
          TreeSet<Integer> sel = new TreeSet<Integer>(overlaps[ly][lx].get(p));
          sel.retainAll(selection);
          
          TreeSet<Integer> scaledSelection = scaleSelection(sel);
          
          double[][] summandRoots = prepareSummandRoots(scaledSelection, variables);
          TreeSet<Integer> result = new TreeSet<Integer>();
          if (summandRoots.length != 0)
            result =
                SumOfSquares.iterativelySolveSumOfSquares2D(summandRoots, errorThreshold, 0, 0,
                    false);
          
          int a = sel.size();
          
          Integer[] selArray = sel.toArray(new Integer[] {});
          for (int i : result)
            sel.remove(selArray[i]);
          
          selection.removeAll(sel);
          
          int b = sel.size();
          
          if (verbose) {
            int x0 = p.getA() % 5;
            int y0 = p.getA() / 5;
            
            int x1 = p.getB() % 5;
            int y1 = p.getB() / 5;
            
            System.out.printf("%d%d-%d%d: %05.2f filtered (%d / %d)\n", x0, y0, x1, y1, 100. * b
                / a, b, a);
          }
        }
        
        if (verbose)
          System.out.println();
      }
  }
  
  public static void solve(Set<Integer> scaledSelection, Set<Integer> variables) {
    Integer[] varMap = variables.toArray(new Integer[] {});
    
    TreeSet<Integer> nonVars = new TreeSet<Integer>();
    for (int i = 0; i != values.length; ++i)
      nonVars.add(i);
    nonVars.removeAll(variables);
    Integer[] nonVarMap = nonVars.toArray(new Integer[] {});
    
    double[][] a = new double[varMap.length][varMap.length];
    double[] c = new double[varMap.length];
    
    for (int i : scaledSelection)
      for (int j = 0; j != varMap.length; ++j) {
        for (int k = 0; k != varMap.length; ++k)
          a[j][k] += factors[i][varMap[j]] * factors[i][varMap[k]];
        
        double constant = 0;
        for (int k : nonVarMap)
          constant -= factors[i][k] * values[k];
        
        c[j] += factors[i][varMap[j]] * constant;
      }
    
    double[] newValues = LinearEquation.solveLinearEquation(a, c, 0, false);
    for (int j = 0; j != varMap.length; ++j)
      values[varMap[j]] = newValues[j];
  }
  
  public static void solveForCoord(Set<Integer> selection, int coord) {
    values[coord] = 0;
    
    TreeSet<Integer> scaledSelection = new TreeSet<Integer>();
    for (int i : selection)
      scaledSelection.add(2 * i + coord);
    
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 1; i != NUM_IMAGES; ++i)
      variables.add(2 * i + coord);
    
    solve(scaledSelection, variables);
  }
  
  public static void solveForPositions(Set<Integer> selection) {
    for (int coord = 0; coord != 2; ++coord)
      solveForCoord(selection, coord);
  }
  
  public static void solveForLenseParameters(Set<Integer> selection, int lx, int ly) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 1; i != NUM_COEFS; ++i)
      variables.add(2 * NUM_IMAGES + (0) * NUM_COEFS + i);
    
    variables.add(2 * NUM_IMAGES + (2 * ly + lx));
    variables.remove(2 * NUM_IMAGES + (0) * NUM_COEFS);
    //variables.remove(2 * NUM_IMAGES + (2 * 0 + 1) * NUM_COEFS);
    
    solve(scaleSelection(selection), variables);
  }
  
  public static void solveForLenseParameters(Set<Integer> selection) {
    /*for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        if (hasSubpixelPoint(selection, lx, ly))
    Stitcher4.doOverlapSievingStep(selection, 3);
    Stitcher4.doModernAnalysis(selection);

          solveForLenseParameters(selection, lx, ly);*/

    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 1; i != NUM_COEFS; ++i)
      variables.add(2 * NUM_IMAGES + (0) * NUM_COEFS + i);
    
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        variables.add(2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS);
    
    variables.remove(2 * NUM_IMAGES);
    solve(scaleSelection(selection), variables);
    copyLenseParameters();
  }
  
  public static void solveEverything(Set<Integer> selection) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 2; i != 2 * NUM_IMAGES; ++i)
      variables.add(i);
    
    /*for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        if (hasSubpixelPoint(selection, lx, ly))
          for (int i = 0; i != NUM_COEFS; ++i)
            variables.add(2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS + i);*/

    for (int i = 1; i != NUM_COEFS; ++i)
      variables.add(2 * NUM_IMAGES + (0) * NUM_COEFS + i);
    
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        variables.add(2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS);
    
    variables.remove(2 * NUM_IMAGES);
    //variables.remove(2 * NUM_IMAGES + (2 * 1 + 0) * NUM_COEFS);
    
    solve(scaleSelection(selection), variables);
    copyLenseParameters();
  }
  
  public static TreeSet<Integer> getSubpixelPoints(int lx, int ly) {
    TreeSet<Integer> selection = new TreeSet<Integer>();
    for (TreeSet<Integer> set : overlaps[ly][lx].values())
      selection.addAll(set);
    return selection;
  }
  
  public static boolean hasSubpixelPoint(Set<Integer> selection, int lx, int ly) {
    for (TreeSet<Integer> set : overlaps[ly][lx].values())
      if (!Collections.disjoint(set, selection))
        return true;
    return false;
  }
  
  public static void outputPositions() {
    for (int image = 0; image != NUM_IMAGES; ++image)
      System.out.printf("(%08.2f, %08.2f)\n", values[2 * image + 0], values[2 * image + 1]);
    System.out.println();
  }
  
  public static void outputLenseParameters() {
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        System.out.print("subpixel " + lx + ly + ": ");
        for (int i = 0; i != NUM_COEFS; ++i)
          System.out.printf("%010.5f, ", 1E3 * Math.pow(1E6, i)
              * values[2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS + i]);
        System.out.println();
      }
    System.out.println();
  }
  
  public static void outputControlPointInfo(int i) {
    int boundary = 0;
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        int j;
        for (Pair<Integer, Integer> p : overlaps[ly][lx].keySet()) {
          j = overlaps[ly][lx].get(p).last();
          if (i <= j) {
            int x0 = p.getA() % 5;
            int y0 = p.getA() / 5;
            
            int x1 = p.getB() % 5;
            int y1 = p.getB() / 5;
            
            int k = i - overlaps[ly][lx].get(p).first();
            double[] point = controlPoints[ly][lx].get(p).get(k);
            
            System.out.println("control point information for point " + i + ":");
            System.out.println("subpixel " + lx + ly);
            System.out.println("subpixel number " + (i - boundary));
            System.out.println("overlap " + x0 + y0 + "-" + x1 + y1);
            System.out.println("overlap number " + k);
            System.out.printf("positions: (%4.2f, %4.2f); (%4.2f, %4.2f)\n", point[0], point[1],
                point[2], point[3]);
            System.out.println();
            
            return;
          }
        }
        boundary = i + 1;
      }
  }
  
  public static interface LoadHandler {
    public void handle(int i, int lx, int ly, int a, int b, double[] point);
  }
  
  public static void load() throws IOException {
    load(null);
  }
  
  @SuppressWarnings("unchecked")
  public static void load(LoadHandler handler) throws IOException {
    controlPoints = new TreeMap[2][2];
    int numPoints = 0;
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        controlPoints[ly][lx] =
            Stitcher2.readControlPoints(DataTools.DIR + "comp" + StitchInfo.SUFFICES[STITCH] + "-"
                + lx + ly + "/combined-0.90.pto");
        for (ArrayList<double[]> points : controlPoints[ly][lx].values())
          numPoints += points.size();
      }
    
    factors = new double[2 * numPoints][2 * NUM_IMAGES + 4 * NUM_COEFS];
    errors = new double[numPoints];
    
    overlaps = new TreeMap[2][2];
    int i = 0;
    
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        overlaps[ly][lx] = new TreeMap<Pair<Integer, Integer>, TreeSet<Integer>>();
        
        for (Pair<Integer, Integer> p : controlPoints[ly][lx].keySet()) {
          TreeSet<Integer> set = new TreeSet<Integer>();
          for (double[] point : controlPoints[ly][lx].get(p)) {
            double x0 = 2 * point[0] + lx - 0.5 * (SX - 1);
            double y0 = 2 * point[1] + ly - 0.5 * (SY - 1);
            
            double x1 = 2 * point[2] + lx - 0.5 * (SX - 1);
            double y1 = 2 * point[3] + ly - 0.5 * (SY - 1);
            
            double r0sq = x0 * x0 + y0 * y0;
            double r1sq = x1 * x1 + y1 * y1;
            
            double r0pow = 1;
            double r1pow = 1;
            
            factors[2 * i + 0][2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS] = x0 - x1;
            factors[2 * i + 1][2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS] = y0 - y1;
            
            for (int j = 1; j != NUM_COEFS; ++j) {
              r0pow *= r0sq;
              r1pow *= r1sq;
              
              factors[2 * i + 0][2 * NUM_IMAGES + (0) * NUM_COEFS + j] =
                  LINEAR_FACTORS_START[ly][lx] * (x0 * r0pow - x1 * r1pow);
              factors[2 * i + 1][2 * NUM_IMAGES + (0) * NUM_COEFS + j] =
                  LINEAR_FACTORS_START[ly][lx] * (y0 * r0pow - y1 * r1pow);
            }
            
            factors[2 * i + 0][2 * p.getA() + 0] = 1;
            factors[2 * i + 1][2 * p.getA() + 1] = 1;
            
            factors[2 * i + 0][2 * p.getB() + 0] = -1;
            factors[2 * i + 1][2 * p.getB() + 1] = -1;
            
            if (handler != null)
              handler.handle(i, lx, ly, p.getA(), p.getB(), point);
            
            set.add(i++);
          }
          
          overlaps[ly][lx].put(p, set);
          
          values[2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS + 0] = LINEAR_FACTORS_START[ly][lx];
        }
      }
  }
  
  public static void doOverlapSievingStep(Set<Integer> selection, double threshold) {
    sieveOverlaps(selection, threshold, false);
    System.out.println("filtering with threshold " + threshold + " leaves " + selection.size()
        + " control points");
    System.out.println();
  }
  
  public static void copyLenseParameters() {
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        for (int i = 1; i != NUM_COEFS; ++i)
          values[2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS + i] =
              values[2 * NUM_IMAGES + (0) * NUM_COEFS + i];
  }
  
  public static void doPositionStep(Set<Integer> selection) {
    solveForPositions(selection);
  }
  
  public static void doLenseStep(Set<Integer> selection) {
    solveForLenseParameters(selection);
    //outputLenseParameters();
  }
  
  public static void filterPoints(Set<Integer> selection, double threshold) {
    for (int i : new TreeSet<Integer>(selection))
      if (errors[i] > threshold)
        selection.remove(i);
    System.out.println(selection.size() + " points remaining");
    System.out.println();
  }
  
  public static void doModernFiltering(Set<Integer> selection, double threshold) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 2; i != 2 * NUM_IMAGES; ++i)
      variables.add(i);
    
    double[][] summandRoots = prepareSummandRoots(scaleSelection(selection), variables);
    System.out.println("summands prepared");
    
    TreeSet<Integer> result =
        SumOfSquares.iterativelySolveSumOfSquares2D(summandRoots, threshold, 0, 0, true);
    
    Integer[] selArray = selection.toArray(new Integer[] {});
    selection.clear();
    for (int i : result)
      selection.add(selArray[i]);
    
    System.out.println(selection.size() + " points remaining");
    System.out.println();
  }
  
  public static void doTraditionalAnalysis(Set<Integer> selection) {
    doPositionStep(selection);
    doLenseStep(selection);
    
    computeErrors(selection);
    SumOfSquares.outputErrorInfo(selection, errors);
    
    checkOverlaps(selection);
  }
  
  public static void doModernAnalysis(Set<Integer> selection) {
    solveEverything(selection);
    
    outputPositions();
    outputLenseParameters();
    
    computeErrors(selection);
    SumOfSquares.outputErrorInfo(selection, errors);
    
    checkOverlaps(selection);
  }
  
  public static void outputWorstControlPoint(Set<Integer> selection) {
    double[] markers = SumOfSquares.computeErrorMarkers(selection, errors);
    int maxIndex = (int) markers[3];
    System.out.println(maxIndex + ": " + markers[2]);
    outputControlPointInfo(maxIndex);
  }
  
  public static void outputParameters(String filename) throws IOException {
    PrintStream out = new PrintStream(filename);
    out.println(NUM_IMAGES);
    for (int image = 0; image != NUM_IMAGES; ++image)
      out.printf("(" + values[2 * image + 0] + ", " + values[2 * image + 1] + ")\n");
    out.println();
    out.println(NUM_COEFS);
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        out.println(values[2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS] + ", ");
        for (int i = 1; i != NUM_COEFS; ++i)
          out.print(LINEAR_FACTORS_START[ly][lx]
              * values[2 * NUM_IMAGES + (2 * ly + lx) * NUM_COEFS + i] + ", ");
        out.println();
      }
    out.println();
    out.close();
  }
  
  public static void checkOverlaps(Set<Integer> selection) {
    checkOverlaps(selection, factors, values);
  }
  
  // HACK
  public static void checkOverlaps(Set<Integer> selection, double[][] factors, double[] values) {
    TreeMap<Pair<Integer, Integer>, double[]> shifts =
        new TreeMap<Pair<Integer, Integer>, double[]>();
    
    for (int a = 0; a != NUM_IMAGES; ++a)
      for (int b = a + 1; b != NUM_IMAGES; ++b) {
        // HACK
        int u0 = a % 5;
        int v0 = a / 5;
        
        int u1 = b % 5;
        int v1 = b / 5;
        
        if (Math.abs(u0 - u1) + Math.abs(v0 - v1) != 1)
          continue;
        
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (int ly = 0; ly != 2; ++ly)
          for (int lx = 0; lx != 2; ++lx) {
            TreeSet<Integer> temp;
            
            temp = overlaps[ly][lx].get(new Pair<Integer, Integer>(a, b));
            if (temp != null)
              set.addAll(temp);
            temp = overlaps[ly][lx].get(new Pair<Integer, Integer>(b, a));
            
            if (temp != null)
              set.addAll(temp);
          }
        
        set.retainAll(selection);
        
        int numPoints = set.size();
        double[] shift = new double[2];
        
        for (int i : set) {
          for (int j = 0; j != 2; ++j) {
            double z = 0;
            for (int k = 0; k != values.length; ++k)
              z -= factors[2 * i + j][k] * values[k];
            shift[j] += z;
          }
        }
        
        for (int i = 0; i != 2; ++i)
          shift[i] /= numPoints;
        
        shifts.put(new Pair<Integer, Integer>(a, b), new double[] {shift[0], shift[1], numPoints});
      }
    
    int minNumPoints = Integer.MAX_VALUE, maxNumPoints = 0;
    double mean = 0, deviation = 0, max = 0;
    Pair<Integer, Integer> maxIndex = null, minNumPointsIndex = null;
    for (Pair<Integer, Integer> p : shifts.keySet()) {
      double[] s = shifts.get(p);
      double e = Math.sqrt(s[0] * s[0] + s[1] * s[1]);
      mean += e;
      deviation += e * e;
      if (e > max) {
        max = e;
        maxIndex = p;
        maxNumPoints = (int) s[2];
      }
      if (s[2] < minNumPoints) {
        minNumPoints = (int) s[2];
        minNumPointsIndex = p;
      }
    }
    mean /= shifts.size();
    deviation = Math.sqrt(deviation / shifts.size());
    
    System.out.println("mean " + mean + ", deviation " + deviation + ", max " + max + " ("
        + maxIndex + ", " + maxNumPoints + "points)");
    System.out.println("min #points " + minNumPoints + " (" + minNumPointsIndex + ")");
    System.out.println();
  }
  
  public static void main(String[] args) throws IOException {
    load();
    System.out.println("loaded " + factors.length / 2 + " control points");
    System.out.println();
    
    TreeSet<Integer> selection = new TreeSet<Integer>();
    for (int i = 0; i != factors.length / 2; ++i)
      selection.add(i);
    //TreeSet<Integer> selection = getSubpixelPoints(1, 0);
    
    doModernFiltering(selection, 2000);
    doModernFiltering(selection, 1500);
    doModernFiltering(selection, 1000);
    doModernFiltering(selection, 500);
    doModernFiltering(selection, 200);
    doModernFiltering(selection, 100);
    doModernFiltering(selection, 50);
    
    doModernFiltering(selection, 20);
    doModernAnalysis(selection);
    
    doOverlapSievingStep(selection, 15);
    doModernAnalysis(selection);
    
    doOverlapSievingStep(selection, 10);
    doModernAnalysis(selection);
    
    doOverlapSievingStep(selection, 7);
    doModernAnalysis(selection);
    
    doOverlapSievingStep(selection, 5);
    doModernAnalysis(selection);
    
    doOverlapSievingStep(selection, 3);
    doModernAnalysis(selection);
    
    int numPoints = -1;
    while (numPoints != (numPoints = selection.size())) {
      doOverlapSievingStep(selection, 3);
      doModernAnalysis(selection);
    }
    
    outputParameters(DataTools.DIR + "stitching" + StitchInfo.SUFFICES[STITCH]
        + "/s20m10m5m3ml.txt");
  }
  
}
