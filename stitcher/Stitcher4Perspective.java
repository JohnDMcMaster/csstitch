package stitcher;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import data.DataTools;
import distributed.Bootstrap;

import tools.LinearEquation;
import tools.SumOfSquares;

public class Stitcher4Perspective {
  
  public static ArrayList<int[]> controlPointsImages = new ArrayList<int[]>();
  public static ArrayList<double[]> controlPointsCoords = new ArrayList<double[]>();
  
  public static double[][] factors;
  public static double[] errors;
  
  public static double[] values;
  
  public static void load() {
    int n = controlPointsImages.size();
    
    factors = new double[2 * n][2 * Stitcher4.NUM_IMAGES + 3];
    errors = new double[n];
    values = new double[2 * Stitcher4.NUM_IMAGES + 3];
    
    values[2 * Stitcher4.NUM_IMAGES + 2] = 1;
    
    for (int i = 0; i != n; ++i) {
      int lx = controlPointsImages.get(i)[0];
      int ly = controlPointsImages.get(i)[1];
      
      int a = controlPointsImages.get(i)[2];
      int b = controlPointsImages.get(i)[3];
      
      double[] point = controlPointsCoords.get(i);
      
      double x0 = 2 * point[0] + lx - 0.5 * (Stitcher4.SX - 1);
      double y0 = 2 * point[1] + ly - 0.5 * (Stitcher4.SY - 1);
      
      double x1 = 2 * point[2] + lx - 0.5 * (Stitcher4.SX - 1);
      double y1 = 2 * point[3] + ly - 0.5 * (Stitcher4.SY - 1);
      
      double r0sq = x0 * x0 + y0 * y0;
      double r1sq = x1 * x1 + y1 * y1;
      
      double r0pow = 1;
      double r1pow = 1;
      
      double factor0 =
          Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS];
      double factor1 =
          Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS];
      
      for (int j = 1; j != Stitcher4.NUM_COEFS; ++j) {
        r0pow *= r0sq;
        r1pow *= r1sq;
        
        factor0 +=
            Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS + j]
                * Stitcher4.LINEAR_FACTORS_START[ly][lx] * r0pow;
        factor1 +=
            Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS + j]
                * Stitcher4.LINEAR_FACTORS_START[ly][lx] * r1pow;
      }
      
      x0 *= factor0;
      y0 *= factor0;
      
      x1 *= factor1;
      y1 *= factor1;
      
      factors[2 * i + 0][2 * a + 0] = 1;
      factors[2 * i + 1][2 * a + 1] = 1;
      
      factors[2 * i + 0][2 * b + 0] = -1;
      factors[2 * i + 1][2 * b + 1] = -1;
      
      factors[2 * i + 0][2 * Stitcher4.NUM_IMAGES + 2] = x0 - x1;
      factors[2 * i + 1][2 * Stitcher4.NUM_IMAGES + 2] = y0 - y1;
      
      factors[2 * i + 0][2 * Stitcher4.NUM_IMAGES + 0] = x0 * x0 - x1 * x1;
      factors[2 * i + 1][2 * Stitcher4.NUM_IMAGES + 0] = x0 * y0 - x1 * y1;
      
      factors[2 * i + 0][2 * Stitcher4.NUM_IMAGES + 1] = y0 * x0 - y1 * x1;
      factors[2 * i + 1][2 * Stitcher4.NUM_IMAGES + 1] = y0 * y0 - y1 * y1;
    }
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
    for (int i = 1; i != Stitcher4.NUM_IMAGES; ++i)
      variables.add(2 * i + coord);
    
    solve(scaledSelection, variables);
  }
  
  public static void solveForPositions(Set<Integer> selection) {
    for (int coord = 0; coord != 2; ++coord)
      solveForCoord(selection, coord);
  }
  
  public static void solveForPerspective(Set<Integer> selection) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    variables.add(2 * Stitcher4.NUM_IMAGES + 0);
    variables.add(2 * Stitcher4.NUM_IMAGES + 1);
    
    solve(Stitcher4.scaleSelection(selection), variables);
  }
  
  public static void solveEverything(Set<Integer> selection) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 2; i != 2 * Stitcher4.NUM_IMAGES + 2; ++i)
      variables.add(i);
    
    solve(Stitcher4.scaleSelection(selection), variables);
  }
  
  public static void outputPositions() {
    for (int image = 0; image != Stitcher4.NUM_IMAGES; ++image)
      System.out.printf("(%08.2f, %08.2f)\n", values[2 * image + 0], values[2 * image + 1]);
    System.out.println();
  }
  
  public static void outputPerspective() {
    System.out.println(values[2 * Stitcher4.NUM_IMAGES + 0]);
    System.out.println(values[2 * Stitcher4.NUM_IMAGES + 1]);
    System.out.println();
  }
  
  public static void doStep(TreeSet<Integer> selection) {
    solveEverything(selection);
    
    outputPositions();
    outputPerspective();
    
    computeErrors(selection);
    SumOfSquares.outputErrorInfo(selection, errors);
    
    Stitcher4.checkOverlaps(selection, factors, values);
  }
  
  public static void outputParameters(String filename) throws IOException {
    PrintStream out = new PrintStream(filename);
    out.println(Stitcher4.NUM_IMAGES);
    for (int image = 0; image != Stitcher4.NUM_IMAGES; ++image)
      out.printf("(" + values[2 * image + 0] + ", " + values[2 * image + 1] + "); ");
    out.println();
    out.println(Stitcher4.NUM_COEFS);
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        out.print(Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS]
            + ", ");
        for (int i = 1; i != Stitcher4.NUM_COEFS; ++i)
          out.print(Stitcher4.LINEAR_FACTORS_START[ly][lx]
              * Stitcher4.values[2 * Stitcher4.NUM_IMAGES + (2 * ly + lx) * Stitcher4.NUM_COEFS + i]
              + ", ");
        out.println();
      }
    out.println();
    out.println(values[2 * Stitcher4.NUM_IMAGES + 0]);
    out.println(values[2 * Stitcher4.NUM_IMAGES + 1]);
    out.println();
    out.close();
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    
    Stitcher4.load(new Stitcher4.LoadHandler() {
      public void handle(int i, int lx, int ly, int a, int b, double[] point) {
        controlPointsImages.add(new int[] {lx, ly, a, b});
        controlPointsCoords.add(point);
      }
    });
    
    TreeSet<Integer> selection = new TreeSet<Integer>();
    for (int i = 0; i != Stitcher4.factors.length / 2; ++i)
      selection.add(i);
    
    Stitcher4.doModernFiltering(selection, 2000);
    Stitcher4.doModernFiltering(selection, 1000);
    Stitcher4.doModernFiltering(selection, 500);
    Stitcher4.doModernFiltering(selection, 200);
    Stitcher4.doModernFiltering(selection, 100);
    Stitcher4.doModernFiltering(selection, 50);
    Stitcher4.doModernFiltering(selection, 20);
    Stitcher4.doModernFiltering(selection, 10);
    
    Stitcher4.doModernAnalysis(selection);
    
    Stitcher4.doOverlapSievingStep(selection, 7);
    Stitcher4.doModernAnalysis(selection);
    
    Stitcher4.doOverlapSievingStep(selection, 5);
    Stitcher4.doModernAnalysis(selection);
    
    int numPoints = -1;
    while (numPoints != (numPoints = selection.size())) {
      Stitcher4.doOverlapSievingStep(selection, 3);
      Stitcher4.doModernAnalysis(selection);
    }
    
    load();
    System.out.println("loaded " + Stitcher4.factors.length / 2 + " control points");
    System.out.println();
    
    for (int i = 0; i != 2 * Stitcher4.NUM_IMAGES; ++i)
      values[i] = Stitcher4.values[i];
    
    doStep(selection);
    outputParameters(DataTools.DIR + "stitching" + StitchInfo.SUFFICES[Stitcher4.STITCH] + "/"
        + "modern-filtering-10m7m5m3mlpn.txt");
  }
  
}
