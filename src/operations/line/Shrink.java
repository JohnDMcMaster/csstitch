package operations.line;

import java.io.Serializable;

public class Shrink implements LineOperation, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private int n;
  
  public Shrink(int n) {
    this.n = n;
  }
  
  public double[] transform(double[] line) {
    double[] result = new double[line.length - 2 * n];
    for (int i = 0; i != result.length; ++i)
      result[i] = line[n + i];
    return result;
  }
  
}
