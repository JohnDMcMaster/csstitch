package map.properties;

import java.io.Serializable;

public class OpticalProperties implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private double[] coefs;
  
  public OpticalProperties(double[] coefs) {
    this.coefs = coefs.clone();
  }
  
  public OpticalProperties(double a, double b, double c) {
    this(new double[] {a, b, c});
  }
  
  public int getNumCoefs() {
    return coefs.length;
  }
  
  public double getCoef(int i) {
    return coefs[i];
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    
    for (int i = 0; i != getNumCoefs(); ++i) {
      if (builder.length() != 1)
        builder.append(", ");
      
      builder.append(getCoef(i));
    }
    
    builder.append("]");
    return builder.toString();
  }
  
}
