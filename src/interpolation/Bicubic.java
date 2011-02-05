package interpolation;

import segment.Sharpness;

public class Bicubic implements Sharpness.ContinuousImage {
  
  private double[][] image;
  private double a;
  
  private double[] wx = new double[4];
  private double[] wy = new double[4];
  
  public Bicubic(double[][] image, double a) {
    this.image = image;
    this.a = a;
  }
  
  public double getBoundary() {
    return 2;
  }
  
  private double cubic01(double x) {
    return ((a + 2) * x - (a + 3)) * x * x + 1;
  }
  
  private double cubic12(double x) {
    return ((a * x - 5 * a) * x + 8 * a) * x - 4 * a;
  }
  
  private void calcWeights(double dx, double[] w) {
    w[0] = cubic12(dx);
    w[1] = cubic01(dx - 1);
    w[2] = cubic01(2 - dx);
    w[3] = cubic12(3 - dx);
  }
  
  // non-reentrant
  public double getValue(double x, double y) {
    int xx = ((int) Math.floor(x)) - 1;
    int yy = ((int) Math.floor(y)) - 1;
    
    if (!(xx >= 0 && yy >= 0 && xx + 4 <= image[0].length && yy + 4 <= image.length))
      return -1;
    
    calcWeights(x - xx, wx);
    calcWeights(y - yy, wy);
    
    double result = 0;
    for (int j = 0; j != 4; ++j) {
      double row = 0;
      for (int i = 0; i != 4; ++i)
        row += wx[i] * image[yy + j][xx + i];
      
      result += wy[j] * row;
    }
    
    return result;
  }
  
}
