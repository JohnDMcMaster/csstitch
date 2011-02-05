package interpolation;

import segment.Sharpness;

public class NearestNeighbour implements Sharpness.ContinuousImage {

  private double[][] image;
  
  public NearestNeighbour(double[][] image) {
    this.image = image;
  }
  
  public double getBoundary() {
    return 0;
  }
  
  public double getValue(double x, double y) {
    int xx = (int) Math.floor(x);
    int yy = (int) Math.floor(y);
    
    if (!(xx >= 0 && yy >= 0 && xx + 1 <= image[0].length && yy + 1 <= image.length))
      return -1;
    
    return image[yy][xx];
  }
  
}
