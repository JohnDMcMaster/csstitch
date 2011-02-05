package map.properties;

import java.io.Serializable;

public class ImagePosition implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private double[] position;
  
  public ImagePosition(double[] position) {
    if (!(position.length == 2))
      throw new IllegalArgumentException();
    
    this.position = position.clone();
  }
  
  public ImagePosition(double x, double y) {
    this(new double[] {x, y});
  }
  
  public double get(int i) {
    return position[i];
  }
  
  public double getX() {
    return get(0);
  }
  
  public double getY() {
    return get(1);
  }
  
  public String toString() {
    return "(" + getX() + ", " + getY() + ")";
  }
  
}
