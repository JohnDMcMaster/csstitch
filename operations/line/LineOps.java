package operations.line;

public class LineOps {
  
  public static LineOperation chain(final LineOperation... ops) {
    return new LineOperation() {
      public double[] transform(double[] line) {
        for (LineOperation op : ops)
          line = op.transform(line);
        return line;
      }
    };
  }
  
}
