package operations.line;

import java.io.Serializable;

public class Abs implements LineOperation, Serializable {
  
  private static final long serialVersionUID = 1L;
  public static final Abs INSTANCE = new Abs();
  
  private Abs() {
  }
  
  public double[] transform(double[] line) {
    double[] result = new double[line.length];
    for (int i = 0; i != line.length; ++i)
      result[i] = Math.abs(line[i]);
    return result;
  }
  
}
