package gaussian;

public class GaussianDerivative implements Function {
  
  private double sigma;
  
  public GaussianDerivative(double sigma) {
    this.sigma = sigma;
  }
  
  public double getWindowSize() {
    return 8 * sigma;
  }
  
  public double eval(double x) {
    return Utils.gaussianDerivative(x / sigma) / (sigma * sigma);
  }
  
}
