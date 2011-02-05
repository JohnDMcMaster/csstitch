package operations.line;

import java.io.Serializable;

import general.median.MedianDouble;

public class MedianFilter implements LineOperation, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static final int BOUNDARY_IGNORE = 0;
  public static final int BOUNDARY_ADJUST = 1;
  public static final int BOUNDARY_COPY = 2;
  public static final int BOUNDARY_CLIP = 3;
  
  private int n;
  private LineOperation chain;
  
  public MedianFilter(int n, int boundary) {
    this.n = n;
    
    if (boundary == BOUNDARY_COPY)
      chain =
          LineOps.chain(new Expand(n, true), new MedianFilter(n, BOUNDARY_IGNORE),
              new Shrink(n));
    else if (boundary == BOUNDARY_CLIP)
      chain = LineOps.chain(new MedianFilter(n, BOUNDARY_IGNORE), new Shrink(n));
  }
  
  public double[] transform(double[] line) {
    if (chain != null)
      return chain.transform(line);
    
    return MedianDouble.median(line, n);
  }
  
}
