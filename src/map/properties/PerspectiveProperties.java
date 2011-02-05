package map.properties;

import java.io.Serializable;

public class PerspectiveProperties implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static PerspectiveProperties IDENTITY = new PerspectiveProperties(0, 0);
  
  private double[] coefs;
  
  public PerspectiveProperties(double[] coefs) {
    if (!(coefs.length == 2))
      throw new IllegalArgumentException();
    
    this.coefs = coefs.clone();
  }
  
  public PerspectiveProperties(double px, double py) {
    this(new double[] {px, py});
  }
  
  public double getCoef(int i) {
    return coefs[i];
  }
  
  public double getCoefX() {
    return getCoef(0);
  }
  
  public double getCoefY() {
    return getCoef(1);
  }
  
  public String toString() {
    return "(" + getCoefX() + ", " + getCoefY() + ")";
  }
  
}
