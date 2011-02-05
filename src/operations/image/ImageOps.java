package operations.image;

import operations.line.LineOperation;

public class ImageOps {
  
  public static ImageOperation chain(final ImageOperation... ops) {
    return new ImageOperation() {
      public double[][] transform(double[][] image) {
        for (ImageOperation op : ops)
          image = op.transform(image);
        return image;
      }
    };
  }
  
  public static ImageOperation applyToLines(final LineOperation lineOp, final int dir) {
    return new ImageOperation() {
      public double[][] transform(double[][] image) {
        double[][] result = ImageOpsDouble.zero(image);
        for (int i = 0; i != ImageOpsDouble.getLimit(image, dir); ++i) {
          double[] line = ImageOpsDouble.getLine(image, i, dir);
          line = lineOp.transform(line);
          ImageOpsDouble.setLine(result, i, dir, line);
        }
        return result;
      }
    };
  }
  
}
