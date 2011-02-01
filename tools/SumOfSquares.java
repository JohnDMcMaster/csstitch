package tools;

import general.collections.Pair;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public strictfp class SumOfSquares {
  
  public static double[] solveSumOfSquares(double[][] summandRoots, int precision, boolean verbose) {
    TreeSet<Integer> good = new TreeSet<Integer>();
    for (int i = 0; i != summandRoots.length; ++i)
      good.add(i);
    
    return solveSumOfSquares(summandRoots, good, precision, verbose);
  }
  
  public static double[] solveSumOfSquares(double[][] summandRoots, TreeSet<Integer> good,
      int precision, boolean verbose) {
    int numVars = summandRoots[0].length - 1;
    
    double[] c = new double[numVars];
    for (int i = 0; i != c.length; ++i)
      for (int j : good)
        c[i] -= summandRoots[j][i] * summandRoots[j][c.length];
    
    double[][] a = new double[numVars][numVars];
    for (int i = 0; i != a.length; ++i)
      for (int k = 0; k != a[0].length; ++k)
        for (int j : good)
          a[i][k] += summandRoots[j][i] * summandRoots[j][k];
    
    return LinearEquation.solveLinearEquation(a, c, precision, verbose);
  }
  
  public static double[] solveSumOfSquares2D(double[][] summandRoots, TreeSet<Integer> good,
      int precision, boolean verbose) {
    int numVars = summandRoots[0].length - 1;
    
    double[] c = new double[numVars];
    for (int i = 0; i != c.length; ++i)
      for (int j : good) {
        c[i] -= summandRoots[2 * j + 0][i] * summandRoots[2 * j + 0][c.length];
        c[i] -= summandRoots[2 * j + 1][i] * summandRoots[2 * j + 1][c.length];
      }
    
    double[][] a = new double[numVars][numVars];
    for (int i = 0; i != a.length; ++i)
      for (int k = 0; k != a[0].length; ++k)
        for (int j : good) {
          a[i][k] += summandRoots[2 * j + 0][i] * summandRoots[2 * j + 0][k];
          a[i][k] += summandRoots[2 * j + 1][i] * summandRoots[2 * j + 1][k];
        }
    
    return LinearEquation.solveLinearEquation(a, c, precision, verbose);
  }
  
  public static double[] computeErrors(double[][] summandRoots, double[] params) {
    int numVars = params.length;
    int numSummands = summandRoots.length;
    
    double[] errors = new double[numSummands];
    for (int i = 0; i != numSummands; ++i) {
      double c = summandRoots[i][numVars];
      for (int j = 0; j != numVars; ++j)
        c += summandRoots[i][j] * params[j];
      errors[i] = Math.abs(c);
    }
    
    return errors;
  }
  
  public static int computeErrors(double[][] summandRoots, TreeSet<Integer> good, double[] params,
      double[] errors) {
    double maxError = -1;
    int maxIndex = -1;
    for (int i : good) {
      double c = summandRoots[i][params.length];
      for (int j = 0; j != params.length; ++j)
        c += summandRoots[i][j] * params[j];
      errors[i] = Math.abs(c);
      
      if (errors[i] > maxError) {
        maxError = errors[i];
        maxIndex = i;
      }
    }
    
    return maxIndex;
  }
  
  public static double[] computeErrors2D(double[][] summandRoots, double[] params) {
    int numVars = params.length;
    int n = summandRoots.length / 2;
    
    double[] errors = new double[n];
    for (int i = 0; i != n; ++i) {
      double a = summandRoots[2 * i + 0][numVars];
      for (int j = 0; j != numVars; ++j)
        a += summandRoots[2 * i + 0][j] * params[j];
      
      double b = summandRoots[2 * i + 1][numVars];
      for (int j = 0; j != numVars; ++j)
        b += summandRoots[2 * i + 1][j] * params[j];
      
      errors[i] = Math.sqrt(a * a + b * b);
    }
    
    return errors;
  }
  
  public static int computeErrors2D(double[][] summandRoots, TreeSet<Integer> good,
      double[] params, double[] errors) {
    double maxError = -1;
    int maxIndex = -1;
    for (int i : good) {
      double a = summandRoots[2 * i + 0][params.length];
      for (int k = 0; k != params.length; ++k)
        a += summandRoots[2 * i + 0][k] * params[k];
      
      double b = summandRoots[2 * i + 1][params.length];
      for (int k = 0; k != params.length; ++k)
        b += summandRoots[2 * i + 1][k] * params[k];
      
      errors[i] = Math.sqrt(a * a + b * b);
      if (errors[i] > maxError) {
        maxError = errors[i];
        maxIndex = i;
      }
    }
    
    return maxIndex;
  }
  
  public static double[] mergeErrors2D(double[] errorsX, double[] errorsY) {
    double[] errors = new double[errorsX.length];
    for (int i = 0; i != errorsX.length; ++i)
      errors[i] = Math.sqrt(errorsX[i] * errorsX[i] + errorsY[i] * errorsY[i]);
    return errors;
  }
  
  public static double[] computeErrorMarkers(Set<Integer> selection, double[] errors) {
    double mean = 0, deviation = 0, max = -1, maxIndex = -1;
    for (int i : selection) {
      if (errors[i] > max) {
        max = errors[i];
        maxIndex = i;
      }
      
      mean += Math.abs(errors[i]);
      deviation += errors[i] * errors[i];
    }
    mean /= selection.size();
    deviation = Math.sqrt(deviation / selection.size());
    return new double[] {mean, deviation, max, maxIndex};
  }
  
  public static void outputErrorMarkers(double[] markers) {
    System.err.println("mean " + markers[0] + ", deviation " + markers[1] + ", max " + markers[2]
        + " (" + (int) markers[3] + ")");
  }
  
  public static void outputErrorInfo(double[] errors) {
    TreeSet<Integer> selection = new TreeSet<Integer>();
    for (int i = 0; i != errors.length; ++i)
      selection.add(i);
    outputErrorInfo(selection, errors);
  }
  
  public static void outputErrorInfo(Set<Integer> selection, double[] errors) {
    outputErrorMarkers(computeErrorMarkers(selection, errors));
  }
  
  public static TreeSet<Integer> iterativelySolveSumOfSquares(double[][] summandRoots,
      double errorThreshold, int numPointsThreshold, int precision, boolean verbose) {
    int numVars = summandRoots[0].length - 1;
    int numSummands = summandRoots.length;
    
    double[] c = new double[numVars];
    double[][] a = new double[numVars][numVars];
    
    double[] errors = new double[numSummands];
    
    TreeSet<Integer> good = new TreeSet<Integer>();
    for (int i = 0; i != numSummands; ++i)
      good.add(i);
    
    for (int i = 0; i != c.length; ++i)
      for (int j : good)
        c[i] -= summandRoots[j][i] * summandRoots[j][c.length];
    
    for (int i = 0; i != a.length; ++i)
      for (int k = 0; k != a[0].length; ++k)
        for (int j : good)
          a[i][k] += summandRoots[j][i] * summandRoots[j][k];
    
    while (true) {
      double[] params = LinearEquation.solveLinearEquation(a, c, precision, false);
      int maxIndex = computeErrors(summandRoots, good, params, errors);
      
      if (verbose)
        System.err.println("num points: " + good.size() + "; error: " + errors[maxIndex]);
      
      if (errors[maxIndex] < errorThreshold)
        return good;
      
      good.remove(maxIndex);
      if (good.size() <= numPointsThreshold)
        return good;
      
      for (int i = 0; i != c.length; ++i)
        c[i] += summandRoots[maxIndex][i] * summandRoots[maxIndex][c.length];
      
      for (int i = 0; i != a.length; ++i)
        for (int k = 0; k != a[0].length; ++k)
          a[i][k] -= summandRoots[maxIndex][i] * summandRoots[maxIndex][k];
    }
  }
  
  public static TreeSet<Integer> iterativelySolveSumOfSquares2D(double[][] summandRoots,
      double errorThreshold, int numPointsThreshold, int precision, boolean verbose) {
    return iterativelySolveSumOfSquares2D(summandRoots, errorThreshold, numPointsThreshold,
        precision, -1, verbose);
  }
  
  public static TreeSet<Integer> iterativelySolveSumOfSquares2D(double[][] summandRoots,
      double errorThreshold, int numPointsThreshold, int precision, double removalQuantil,
      boolean verbose) {
    int numVars = summandRoots[0].length - 1;
    int n = summandRoots.length / 2;
    
    double[] c = new double[numVars];
    double[][] a = new double[numVars][numVars];
    
    double[] errors = new double[n];
    
    TreeSet<Integer> good = new TreeSet<Integer>();
    for (int i = 0; i != n; ++i)
      good.add(i);
    
    for (int j = 0; j != summandRoots.length; ++j)
      for (int i = 0; i != c.length; ++i)
        c[i] -= summandRoots[j][i] * summandRoots[j][c.length];
    
    for (int j = 0; j != summandRoots.length; ++j) {
      double[] row = summandRoots[j];
      for (int i = 0; i != a.length; ++i) {
        double[] as = a[i];
        double rowI = row[i];
        for (int k = 0; k != as.length; ++k)
          as[k] += rowI * row[k];
      }
      
      if (j % 10000 == 0 && j != 0)
        System.err.println(j);
    }
    
    while (true) {
      double[] copyC = c.clone();
      double[][] copyA = new double[a.length][];
      for (int i = 0; i != a.length; ++i)
        copyA[i] = a[i].clone();
      
      double[] params = LinearEquation.solveLinearEquation(copyA, copyC, precision, false);
      int maxIndex = computeErrors2D(summandRoots, good, params, errors);
      
      if (maxIndex == -1)
        return good;
      
      if (verbose)
        System.err.println("num points: " + good.size() + "; error: " + errors[maxIndex]);
      
      if (errors[maxIndex] < errorThreshold)
        return good;
      
      TreeSet<Pair<Double, Integer>> points = new TreeSet<Pair<Double, Integer>>();
      
      int maxNumRemovals = Math.max(1,
          Math.min(good.size() - numPointsThreshold, removalQuantil > 0
              ? (int) (removalQuantil * good.size()) : (int) -removalQuantil));
      
      for (int i : good)
        if (errors[i] >= errorThreshold) {
          points.add(new Pair<Double, Integer>(errors[i], i));
          if (points.size() > maxNumRemovals) {
            Iterator<Pair<Double, Integer>> it = points.iterator();
            it.next();
            it.remove();
          }
        }
      
      for (Pair<Double, Integer> point : points) {
        int index = point.getB();
        good.remove(index);
        
        for (int i = 0; i != c.length; ++i) {
          c[i] += summandRoots[2 * index + 0][i] * summandRoots[2 * index + 0][c.length];
          c[i] += summandRoots[2 * index + 1][i] * summandRoots[2 * index + 1][c.length];
        }
        
        for (int i = 0; i != a.length; ++i)
          for (int k = 0; k != a[0].length; ++k) {
            a[i][k] -= summandRoots[2 * index + 0][i] * summandRoots[2 * index + 0][k];
            a[i][k] -= summandRoots[2 * index + 1][i] * summandRoots[2 * index + 1][k];
          }
      }
      
      if (good.size() <= numPointsThreshold)
        return good;
    }
  }
}
