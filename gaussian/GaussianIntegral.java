package gaussian;

public class GaussianIntegral implements Function {
  
  private double sigma;
  
  public GaussianIntegral(double sigma) {
    this.sigma = sigma;
  }
  
  public double getWindowSize() {
    return 8 * sigma;
  }
  
  public double eval(double x) {
    return Utils.gaussianIntegral(x / sigma);
  }
  
}
