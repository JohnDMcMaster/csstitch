package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;
import java.util.TreeSet;

public class ColorScalingCorrect {

  public static double[] solve(TreeMap<Double, Double> points) {
    double sum_s = 0, sum_s_sq = 0, sum_d = 0, mixed_prod = 0;
    for (double s : points.keySet()) {
      double d = points.get(s);
      sum_s += s;
      sum_d += d;
      sum_s_sq += s * s;
      mixed_prod += s * d;
    }

    double factor = (points.size() * mixed_prod - sum_s * sum_d)
        / (points.size() * sum_s_sq - sum_s * sum_s);
    double shift = (sum_d - sum_s * factor) / points.size();
    return new double[] {factor, shift};
  }

  public static void main(String[] args) throws IOException {
    double threshold = Double.valueOf(args[1]);
    
    TreeMap<Double, Double> pointsX = new TreeMap<Double, Double>();
    TreeMap<Double, Double> pointsY = new TreeMap<Double, Double>();

    BufferedReader in = new BufferedReader(new FileReader(args[0]));
    String line;
    while ((line = in.readLine()) != null) {
      if (!line.startsWith("c"))
        continue;

      String[] entries = line.split(" ");
      double sx = Double.valueOf(entries[3].substring(1));
      double sy = Double.valueOf(entries[4].substring(1));
      double dx = Double.valueOf(entries[5].substring(1));
      double dy = Double.valueOf(entries[6].substring(1));

      pointsX.put(sx, dx);
      pointsY.put(sy, dy);
    }
   
    System.out.print("X ");
    process(pointsX, threshold);
    
    System.out.print("Y ");
    process(pointsY, threshold);
  }
    
  // (sum_i s_i^2) f + (sum_i s_i) a = (sum_i d_i s_i)
  // (sum_i s_i  ) f + (sum_i   1) a = (sum_i d_i    )

  public static void process(TreeMap<Double, Double> points, double threshold) {
    double[] sol;
    while (true) {
      sol = solve(points);

      double maxError = 0;
      double maxErrorPoint = 0;
      for (double s : points.keySet()) {
        double dist = Math.abs(s * sol[0] + sol[1] - points.get(s));
        if (dist > maxError) {
          maxError = dist;
          maxErrorPoint = s;
        }
      }

      points.remove(maxErrorPoint);
      if (maxError < threshold)
        break;
    }
    
    System.out.println("solution: " + sol[0] + ", " + sol[1] + "; fix: " + sol[1] / (1 - sol[0]) + "; " + points.size() + " points");
  }

}
