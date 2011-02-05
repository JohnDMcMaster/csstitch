package gaussian;

public class Gaussian implements Function {
  
  private double sigma;
  
  public Gaussian(double sigma) {
    this.sigma = sigma;
  }
  
  public double getWindowSize() {
    return 8 * sigma;
  }
  
  public double eval(double x) {
    return Utils.gaussian(x / sigma) / sigma;
  }
  
}
