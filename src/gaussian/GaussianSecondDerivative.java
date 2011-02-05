package gaussian;

public class GaussianSecondDerivative implements Function {
  
  private double sigma;
  
  public GaussianSecondDerivative(double sigma) {
    this.sigma = sigma;
  }
  
  public double getWindowSize() {
    return 8 * sigma;
  }
  
  public double eval(double x) {
    return Utils.gaussianSecondDerivative(x / sigma) / (sigma * sigma * sigma);
  }
  
}
