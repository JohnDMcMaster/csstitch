package tools;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.TreeSet;

public strictfp class LinearEquation {

  public static double[] solveLinearEquation(double[][] a, double[] c) {
    return solveLinearEquation(a, c, 0);
  }

  public static double[] solveLinearEquation(double[][] a, double[] c, int precision) {
    return solveLinearEquation(a, c, precision, false);
  }

  public static double[] solveLinearEquation(double[][] a, double[] c, int precision,
      boolean verbose) {
    if (precision == 0)
      return solveLinearEquation(a, c, verbose);

    return solveLinearEquationAccurately(a, c, precision, verbose);
  }

  public static double[] solveLinearEquation(double[][] a, double[] c, boolean verbose) {
    int[] pivotColumns = new int[c.length];
    TreeSet<Integer> pivotColumnSet = new TreeSet<Integer>();

    for (int t = 0; t != c.length; ++t) {
      if (verbose)
        System.out.println("processing equation " + t + "...");

      double max = 0;
      int maxRow = 0, maxColumn = 0;
      for (int z = t; z != c.length; ++z)
        for (int u = 0; u != a[0].length; ++u)
          if (!pivotColumnSet.contains(u) && Math.abs(a[z][u]) > max) {
            max = Math.abs(a[z][u]);
            maxRow = z;
            maxColumn = u;
          }

      if (verbose)
        System.out.println("pivot element has norm " + max);

      pivotColumns[t] = maxColumn;
      pivotColumnSet.add(maxColumn);

      double[] temp = a[maxRow];
      a[maxRow] = a[t];
      a[t] = temp;

      double temp2 = c[maxRow];
      c[maxRow] = c[t];
      c[t] = temp2;

      for (int z = 0; z != c.length; ++z)
        if (z != t) {
          double factor = a[z][maxColumn] / a[t][maxColumn];
          
          for (int u = 0; u != a[0].length; ++u)
            a[z][u] -= factor * a[t][u];
          c[z] -= factor * c[t];
        }
    }

    double[] b = new double[c.length];
    for (int t = 0; t != c.length; ++t)
      b[pivotColumns[t]] = c[t] / a[t][pivotColumns[t]];
    return b;
  }

  public static double[] solveLinearEquationAccurately(double[][] matrixA, double[] vectorC,
      int precision, boolean verbose) {
    MathContext context = new MathContext(precision);

    BigDecimal[][] a = new BigDecimal[matrixA.length][matrixA[0].length];
    for (int t = 0; t != a.length; ++t)
      for (int u = 0; u != a[0].length; ++u)
        a[t][u] = BigDecimal.valueOf(matrixA[t][u]);

    BigDecimal[] c = new BigDecimal[vectorC.length];
    for (int t = 0; t != c.length; ++t)
      c[t] = BigDecimal.valueOf(vectorC[t]);

    int[] pivotColumns = new int[c.length];
    TreeSet<Integer> pivotColumnSet = new TreeSet<Integer>();

    for (int t = 0; t != c.length; ++t) {
      if (verbose)
        System.out.println("processing equation " + t + "...");

      BigDecimal max = BigDecimal.ZERO;
      int maxRow = 0, maxColumn = 0;
      for (int z = t; z != c.length; ++z)
        for (int u = 0; u != a[0].length; ++u)
          if (!pivotColumnSet.contains(u) && a[z][u].abs(context).compareTo(max) > 0) {
            max = a[z][u].abs();
            maxRow = z;
            maxColumn = u;
          }

      if (verbose)
        System.out.println("pivot element has norm " + max);

      pivotColumns[t] = maxColumn;
      pivotColumnSet.add(maxColumn);

      BigDecimal[] temp = a[maxRow];
      a[maxRow] = a[t];
      a[t] = temp;

      BigDecimal temp2 = c[maxRow];
      c[maxRow] = c[t];
      c[t] = temp2;

      for (int z = 0; z != c.length; ++z)
        if (z != t) {
          BigDecimal factor = a[z][maxColumn].divide(a[t][maxColumn], context);
          
          for (int u = 0; u != a[0].length; ++u)
            a[z][u] = a[z][u].subtract(a[t][u].multiply(factor, context), context);
          c[z] = c[z].subtract(c[t].multiply(factor, context), context);
        }
    }

    double[] b = new double[c.length];
    for (int t = 0; t != c.length; ++t)
      b[pivotColumns[t]] = c[t].divide(a[t][pivotColumns[t]], context).doubleValue();
    return b;
  }

}
