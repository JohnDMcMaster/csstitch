package operations.line;

import java.io.Serializable;

public class Negate implements LineOperation, Serializable {

  private static final long serialVersionUID = 1L;
  public static final Negate INSTANCE = new Negate();
  
  private Negate() {
  }

  public double[] transform(double[] line) {
    double[] result = new double[line.length];
    for (int i = 0; i != line.length; ++i)
      result[i] = -line[i];
    return result;
  }
  
}
