package metal2;

public class Canny {
  
  public static double[] getGaussianFilter(double sigma, double t) {
    int k = (int) Math.ceil(t * sigma);
    double[] filter = new double[2 * k + 1];
    
    for (int x = -k; x <= k; ++x)
      filter[x + k] = 1 / (Math.sqrt(2 * Math.PI) * sigma) * Math.exp(-x * x / (2 * sigma * sigma));
    
    return filter;
  }
  
  //public static double[][] computeGradients(double[][] image, double[][] filterX, )
  
  public static void main(String[] args) {
    double[] filter = getGaussianFilter(2.0, 8);
    for (int i = 0; i != filter.length; ++i)
      System.out.print(filter[i] + ", ");
    System.out.println();
    
    double sum = 0;
    for (double v : filter)
      sum += v;
    System.out.println(sum);
    
    
  }
  
}
