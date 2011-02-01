package stitcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import tools.LinearEquation;
import tools.SumOfSquares;

import data.DataTools;
import distributed.Bootstrap;

public strictfp class Affine {

  public static final int alignmentWeight = 10;
  public static final int numVars = 6;

  public static int numStitches;

  public static double[][] factors;
  public static double[] errors;

  public static double[] values;

  public static TreeSet<Integer> normalRows;
  public static TreeSet<Integer> alignmentRows;
  public static TreeSet<Integer> everything;

  public static void load(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");

    numStitches = scanner.nextInt();

    ArrayList<double[]> array = new ArrayList<double[]>();

    normalRows = new TreeSet<Integer>();
    alignmentRows = new TreeSet<Integer>();
    everything = new TreeSet<Integer>();

    int n = 0;
    for (;;) {
      String type = scanner.next("[a-zA-Z0-9]+");

      if (type.equals("end"))
        break;

      if (type.equals("n")) {
        double[][] row = new double[numStitches][2];
        for (int i = 0; i != numStitches; ++i) {
          row[i][0] = scanner.nextDouble();
          row[i][1] = scanner.nextDouble();
        }

        for (int p = 0; p != numStitches; ++p)
          for (int q = p + 1; q != numStitches; ++q) {
            if (q == 2 && !(n >= 9 && n < 12))
              continue;

            double[][] entries = new double[2][numVars * numStitches];
            for (int j = 0; j != 2; ++j) {
              int r = j == 0 ? p : q;
              int s = j == 0 ? 1 : -1;

              entries[0][numVars * r + 0] = s * row[r][0];
              entries[0][numVars * r + 1] = s * row[r][1];
              entries[0][numVars * r + numVars - 2] = s * 1;
              entries[0][numVars * r + numVars - 1] = s * 0;

              entries[1][numVars * r + 2] = s * row[r][0];
              entries[1][numVars * r + 3] = s * row[r][1];
              entries[1][numVars * r + numVars - 2] = s * 0;
              entries[1][numVars * r + numVars - 1] = s * 1;
            }

            normalRows.add(array.size() + 0);
            normalRows.add(array.size() + 1);

            array.add(entries[0]);
            array.add(entries[1]);
          }
        ++n;
      } else if (type.startsWith("h")) {
        int p = Integer.parseInt(type.substring(1));
        double[][][] rows = new double[2][numStitches][2];
        for (int j = 0; j != 2; ++j)
          for (int i = 0; i != numStitches; ++i) {
            rows[j][i][0] = scanner.nextDouble();
            rows[j][i][1] = scanner.nextDouble();
          }

        double[] entry = new double[numVars * numStitches + 2];

        entry[numVars * p + 2] = rows[0][p][0] - rows[1][p][0];
        entry[numVars * p + 3] = rows[0][p][1] - rows[1][p][1];

        for (int i = 0; i != alignmentWeight; ++i) {
          alignmentRows.add(array.size());
          array.add(entry);
        }
      } else if (type.startsWith("v")) {
        int p = Integer.parseInt(type.substring(1));
        double[][][] rows = new double[2][numStitches][2];
        for (int j = 0; j != 2; ++j)
          for (int i = 0; i != numStitches; ++i) {
            rows[j][i][0] = scanner.nextDouble();
            rows[j][i][1] = scanner.nextDouble();
          }

        double[] entry = new double[numVars * numStitches];

        entry[numVars * p + 0] = rows[0][p][0] - rows[1][p][0];
        entry[numVars * p + 1] = rows[0][p][1] - rows[1][p][1];

        for (int i = 0; i != alignmentWeight; ++i) {
          alignmentRows.add(array.size());
          array.add(entry);
        }
      }
    }

    factors = array.toArray(new double[][] {});
    errors = new double[array.size()];

    values = new double[numVars * numStitches];
    resetValues();

    everything.addAll(normalRows);
    everything.addAll(alignmentRows);
  }

  public static void resetValues() {
    for (int i = 0; i != numStitches; ++i) {
      values[numVars * i + 0] = 1;
      values[numVars * i + 1] = 0;
      values[numVars * i + 2] = 0;
      values[numVars * i + 3] = 1;
      values[numVars * i + 4] = 0;
      values[numVars * i + 5] = 0;
    }
  }

  public static void computeErrors() {
    for (int i = 0; i != factors.length; ++i) {
      errors[i] = 0;
      for (int k = 0; k != values.length; ++k)
        errors[i] += factors[i][k] * values[k];
    }
  }

  public static void solve(Set<Integer> selection, Set<Integer> variables) {
    Integer[] varMap = variables.toArray(new Integer[] {});

    TreeSet<Integer> nonVars = new TreeSet<Integer>();
    for (int i = 0; i != values.length; ++i)
      nonVars.add(i);
    nonVars.removeAll(variables);
    Integer[] nonVarMap = nonVars.toArray(new Integer[] {});

    double[][] a = new double[varMap.length][varMap.length];
    double[] c = new double[varMap.length];

    for (int i : selection)
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

  public static TreeSet<Integer> select(int zero, boolean affine) {
    TreeSet<Integer> variables = new TreeSet<Integer>();
    for (int i = 0; i != numStitches; ++i) {
      if (affine)
        for (int j = 0; j != numVars - 2; ++j)
          variables.add(numVars * i + j);

      variables.add(numVars * i + numVars - 2);
      variables.add(numVars * i + numVars - 1);
    }

    variables.remove(numVars * zero + 0);
    variables.remove(numVars * zero + 3);

    variables.remove(numVars * zero + numVars - 2);
    variables.remove(numVars * zero + numVars - 1);

    return variables;
  }

  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);
    load(DataTools.DIR + "affine.txt");

    TreeSet<Integer> selection = select(0, true);
    solve(everything, selection);
    computeErrors();
    SumOfSquares.outputErrorInfo(everything, errors);

    for (int i = 0; i != numStitches; ++i) {
      System.out.print("{");
      for (int j = 0; j != numVars; ++j) {
        System.out.print(values[numVars * i + j]);
        if (j + 1 != numVars)
          System.out.print(", ");
      }
      System.out.print("}");
      if (i + 1 != numStitches)
        System.out.print(",");
      System.out.println();
    }
  }

}
