package devignetter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DevigPlotter {

  public static void main(String[] args) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    ArrayList<Double> list = new ArrayList<Double>();
    list = new ArrayList<Double>();

    while (true) {
      String line = in.readLine();
      if (line.length() == 0)
        break;

      list.add(Double.parseDouble(line));
    }

    for (int x = 0; x != 2000; ++x) {
      double r = x * x;
      double f = 1;
      for (int i = 0; i != list.size(); ++i)
        f += list.get(i) * Math.pow(r, i + 1);
      System.out.printf("%04d: %f\n", x, f);
    }
  }

}
