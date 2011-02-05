package interpolation;

import segment.Sharpness;

public class Bilinear implements Sharpness.ContinuousImage {
  
  private double[][] image;
  
  public Bilinear(double[][] image) {
    this.image = image;
  }
  
  public double getBoundary() {
    return 1;
  }
  
  public double getValue(double x, double y) {
    int xx = (int) Math.floor(x);
    int yy = (int) Math.floor(y);
    
    double dx = x - xx;
    double dy = y - yy;
    
    if (!(xx >= 0 && yy >= 0 && xx + 2 <= image[0].length && yy + 2 <= image.length))
      return -1;
    
    double a = image[yy][xx] + dx * (image[yy][xx + 1] - image[yy][xx]);
    double b = image[yy + 1][xx] + dx * (image[yy + 1][xx + 1] - image[yy + 1][xx]);
    return a + dy * (b - a);
  }
}
