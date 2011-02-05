package realmetal;

public class Blockarizer {
  
  private int maxSegmentLength;
  
  private double[][] squares;
  private double[][] distances;
  private int[][] last;
  private double[][] cumulative;
  
  private double[] line;
  
  public Blockarizer(int maxSegmentLength, int maxLineLength, int maxNumSegments) {
    this.maxSegmentLength = maxSegmentLength;
    
    squares = new double[maxLineLength + 1][maxLineLength + 1];
    distances = new double[maxLineLength + 1][maxLineLength + 1];
    last = new int[maxNumSegments + 1][maxLineLength + 1];
    cumulative = new double[maxNumSegments + 1][maxLineLength + 1];
  }
  
  public double getDistance(int numSegments) {
    return cumulative[numSegments][line.length];
  }
  
  public int[] getDecomposition(int numSegments) {
    int[] decomposition = new int[numSegments + 1];
    decomposition[numSegments] = line.length;
    for (int a = numSegments; a != 0; --a)
      decomposition[a - 1] = last[a][decomposition[a]];
    return decomposition;
  }
  
  public double[] getDecompositionValues(int numSegments) {
    int[] decomposition = getDecomposition(numSegments);
    
    double[] values = new double[numSegments];
    for (int i = 0; i != numSegments; ++i) {
      double sum = 0;
      for (int j = decomposition[i]; j != decomposition[i + 1]; ++j)
        sum += line[j];
      values[i] = sum / (decomposition[i + 1] - decomposition[i]);
    }
    return values;
  }
  
  public double[] getApproximation(int numSegments) {
    int[] decomposition = getDecomposition(numSegments);
    double[] values = getDecompositionValues(numSegments);
    
    double[] result = new double[line.length];
    for (int i = 0; i != numSegments; ++i)
      for (int j = decomposition[i]; j != decomposition[i + 1]; ++j)
        result[j] = values[i];
    return result;
  }
  
  public void decompose(double[] line, int numSegments) {
    decompose(line, numSegments, Float.POSITIVE_INFINITY);
  }

  public int decompose(double[] line, double threshold) {
    return decompose(line, cumulative.length - 1, threshold);
  }

  public int decompose(double[] line, int numSegments, double threshold) {
    this.line = line;
    
    for (int i = 0; i <= line.length; ++i) {
      double sum = 0;
      for (int j = i + 1; j <= line.length; ++j) {
        double d = line[j - 1] - line[i];
        sum += d * d;
        squares[i][j] = sum;
      }
    }
    
    for (int i = 0; i <= line.length; ++i) {
      double sum = 0;
      for (int j = i + 1; j <= line.length; ++j) {
        sum += squares[i][j];
        distances[i][j] = sum / (j - i);
      }
    }
    
    for (int a = 0; a <= numSegments; ++a)
      for (int i = 0; i <= line.length; ++i)
        cumulative[a][i] = Float.POSITIVE_INFINITY;
    cumulative[0][0] = 0;
    
    for (int a = 0; a != numSegments; ++a) {
      for (int i = 0; i <= line.length; ++i) {
        int limit = Math.min(i + maxSegmentLength, line.length);
        for (int j = i + 1; j <= limit; ++j) {
          double val = cumulative[a][i] + distances[i][j];
          if (val < cumulative[a + 1][j]) {
            cumulative[a + 1][j] = val;
            last[a + 1][j] = i;
          }
        }
      }
      
      if (cumulative[a + 1][line.length] / line.length < threshold * threshold)
        return a + 1;
    }
    
    return -1;
  }
  
  public static void main(String[] args) {
  }
  
}
