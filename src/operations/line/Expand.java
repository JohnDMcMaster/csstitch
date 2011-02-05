package operations.line;

import java.io.Serializable;

public class Expand implements LineOperation, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int n;
  private boolean copy;
  
  public Expand(int n) {
    this(n, false);
  }
  
  public Expand(int n, boolean copy) {
    this.n = n;
    this.copy = copy;
  }
  
  public double[] transform(double[] line) {
    double[] result = new double[line.length + 2 * n];
    for (int i = 0; i != line.length; ++i)
      result[n + i] = line[i];
    if (copy) {
      for (int i = 0; i != n; ++i)
        result[i] = line[0];
      for (int i = 0; i != n; ++i)
        result[result.length - 1 - i] = line[line.length - 1];
    }
    return result;
  }
}
