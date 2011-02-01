package operations.line;

import java.io.Serializable;

public class FlattenFilter implements LineOperation, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int n;
  private boolean down;
  
  public FlattenFilter(int n, boolean down) {
    this.n = n;
    this.down = down;
  }
  
  private double[] transformDown(double[] line) {
    double[] result = new double[line.length];
    
    double[][] minima = new double[n + 1][line.length];
    minima[0] = line;
    for (int i = 0; i != n; ++i)
      for (int j = 0; j != line.length - i - 1; ++j)
        minima[i + 1][j] = Math.min(minima[i][j], line[j + i + 1]);
    
    for (int i = 0; i != line.length; ++i) {
      double min = Double.POSITIVE_INFINITY;
      for (int j = 0; j <= n; ++j)
        if (i - j >= 0 && i + (n - j) < line.length)
          min = Math.min(min, Math.max(minima[j][i - j], minima[n - j][i]));
      result[i] = min;
    }
    
    return result;
  }
  
  private double[] transformUp(double[] line) {
    double[] result = new double[line.length];
    
    double[][] maxima = new double[n + 1][line.length];
    maxima[0] = line;
    for (int i = 0; i != n; ++i)
      for (int j = 0; j != line.length - i - 1; ++j)
        maxima[i + 1][j] = Math.max(maxima[i][j], line[j + i + 1]);
    
    for (int i = 0; i != line.length; ++i) {
      double max = Double.NEGATIVE_INFINITY;
      for (int j = 0; j <= n; ++j)
        if (i - j >= 0 && i + (n - j) < line.length)
          max = Math.max(max, Math.min(maxima[j][i - j], maxima[n - j][i]));
      result[i] = max;
    }
    
    return result;
  }
  
  public double[] transform(double[] line) {
    return down ? transformDown(line) : transformUp(line);
  }
  
}
