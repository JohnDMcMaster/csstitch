package devignetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DistanceCalculator {

  public static void main(String[] args) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    ArrayList<Double>[] list = new ArrayList[2];
    for (int i = 0; i != 2; ++i) {
      list[i] = new ArrayList<Double>();
      
      while (true) {
        String line = in.readLine();
        if (line.length() == 0)
          break;

        list[i].add(Double.parseDouble(line));
      }
    }

    int n = list[0].size();
    if (n != list[1].size())
      throw new RuntimeException();

    for (int i = 0; i != 2; ++i) {
      for (int j = 0; j != n; ++j)
        list[i].set(j, list[i].get(j) * Math.pow(2E6, j + 1));
    }

    double dist = 0, max = 0;
    for (int j = 0; j != n; ++j) {
      double error = Math.abs(list[0].get(j) - list[1].get(j));
      if (error > max)
        max = error;
      dist += error * error;
    }
    System.out.println("distance " + Math.sqrt(dist) + ", deviation " + Math.sqrt(dist / n)
        + ", max " + max);
    
    for (int i = 0; i != 2; ++i) {
      for (int j = 0; j != n; ++j)
        System.out.println(list[i].get(j));
      System.out.println();
    }
  }
}
