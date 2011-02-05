package devignetter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import data.DataTools;

public class DevigSolver {

  public static final int SX = 3250;
  public static final int SY = 2450;

  public static final double SLOPE = 64;

  public static final int NUM_POINTS_X = (int) (SX / SLOPE) + 1;
  public static final int NUM_POINTS_Y = (int) (SY / SLOPE) + 1;

  public static final int N = NUM_POINTS_X * NUM_POINTS_Y;

  public static int[][] indices;
  public static double[][] entries;
  public static double[] diagonal;

  public static void load() throws IOException {
    indices = new int[N][];
    entries = new double[N][];
    diagonal = new double[N];

    TreeMap<Integer, Double> matrix[] = DataTools.readIntegerDoubleMapArray(DataTools
        .openReading(DataTools.DIR + "stitching/devig.dat"));

    double max = 0;
    for (int i = 0; i != N; ++i) {
      for (int j : matrix[i].keySet()) {
        Double a = matrix[i].get(j);
        Double b = matrix[j].get(i);
        
        if (a == null)
          a = 0.;
        if (b == null)
          b = 0.;
        max = Math.max(max, Math.abs(a - b));
      }
    }

    System.out.println("symdiff " + max);
    
    for (int i = 0; i != N; ++i) {
      indices[i] = new int[matrix[i].size()];
      entries[i] = new double[matrix[i].size()];

      int j = 0;
      for (int k : matrix[i].keySet()) {
        indices[i][j] = k;
        entries[i][j] = matrix[i].get(k);
        ++j;
      }

      diagonal[i] = matrix[i].get(i);
      if (diagonal[i] == 0)
        throw new RuntimeException();
    }

    checkForNaN(entries);
  }

  public static void checkForNaN(double[] vector) {
    for (int i = 0; i != vector.length; ++i)
      if (Double.isNaN(vector[i]) || Double.isInfinite(vector[i]))
        throw new RuntimeException();
  }

  public static void checkForNaN(double[][] matrix) {
    for (int i = 0; i != matrix.length; ++i)
      for (int j = 0; j != matrix[i].length; ++j)
        if (Double.isNaN(matrix[i][j]) || Double.isInfinite(matrix[i][j]))
          throw new RuntimeException();
  }

  public static double[] apply(double[] vector) {
    double[] result = new double[N];
    for (int i = 0; i != N; ++i)
      for (int j = 0; j != indices[i].length; ++j)
        result[i] += entries[i][j] * vector[indices[i][j]];
    return result;
  }
  
  public static double getNorm(double[] vector) {
    double sum = 0;
    for (int i = 0; i != N; ++i)
      sum += vector[i] * vector[i];
    return Math.sqrt(sum);
  }
  
  public static void normalize(double[] vector) {
    double norm = 1 / getNorm(vector);
    for (int i = 0; i != N; ++i)
      vector[i] *= norm;
  }
  
  public static void makeNorm(double[] vector, double s) {
    double norm = Math.sqrt(s) / getNorm(vector);
    for (int i = 0; i != N; ++i)
      vector[i] *= norm;
  }
  
  public static double getDistance(double[] a, double[] b) {
    double sum = 0;
    for (int i = 0; i != N; ++i) {
      double c = a[i] - b[i];
      sum += c * c;
    }
    return Math.sqrt(sum);
  }

  public static void main(String[] args) throws IOException {
    load();
    
    double[] values = new double[N];
    double[] newValues = new double[N];

    double[] oldValues = null;

    for (int i = 0; i != N; ++i)
      values[i] = Math.sqrt(1. / N);

    for (int step = 0; step != 100; ++step) {
      for (int i = 0; i != N; ++i) {
        newValues[i] = 0;
        for (int j = 0; j != indices[i].length; ++j)
          if (indices[i][j] != i)
            newValues[i] -= entries[i][j] * values[indices[i][j]];

        newValues[i] /= diagonal[i];
      }
      
      makeNorm(newValues, N);

      if (step % 10 == 0) {
        double distance = getDistance(values, newValues);
        System.out.println("distance " + distance);
        if (distance < 1E-20)
          break;
      }
      
      double[] temp = values;
      values = newValues;
      newValues = temp;
    }

    double[][] solution = new double[NUM_POINTS_Y][NUM_POINTS_X];
    for (int y = 0; y != NUM_POINTS_Y; ++y) {
      for (int x = 0; x != NUM_POINTS_X; ++x) {
        solution[y][x] = values[NUM_POINTS_X * y + x];
        System.out.printf("%6f ", solution[y][x]);
      }
      System.out.println();
    }

    DataOutputStream out = DataTools.openWriting(DataTools.DIR + "stitching/devig-solved.dat");
    DataTools.writeMatrixDouble(out, solution);
    out.close();
  }
}
