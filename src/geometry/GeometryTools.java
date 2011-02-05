package geometry;

import general.numerals.BigRational;

public class GeometryTools {
  
  // decideOrientation(a, b, c) returns
  //     0 if a, b, c are collinear,
  //     1 if a, b, c are oriented counter-clockwise,
  //    -1 else
  public static int decideOrientation(AffinePoint... points) {
    BigRational sum = BigRational.ZERO;
    for (int i = 0; i != 3; ++i) {
      int j = (i + 1) % 3;
      int k = (i + 2) % 3;
      sum =
          sum.add(points[j].getX().multiply(points[k].getY())
              .subtract(points[k].getX().multiply(points[j].getY())));
    }
    
    return sum.signum();
  }
  
  // for a, b, c counter-clockwise, decideCyclicity(a, b, c, d) returns
  //    0 if quadrangle is cyclic,
  //    1 if circumcircle of a, b, c properly d,
  //   -1 else
  //
  // decideCyclicity(sigma(a), sigma(b), sigma(c), sigma(d)) = (-1)^(sign(sigma)) decideCyclicity(a, b, c, d)
  public static int decideCyclicity(AffinePoint... points) {
    BigRational[][] input = new BigRational[4][2];
    
    for (int i = 0; i != 3; ++i)
      for (int j = 0; j != 2; ++j)
        input[i][j] = points[i].get(j).subtract(input[3][j]);
    
    BigRational sum = BigRational.ZERO;
    
    for (int i = 0; i != 3; ++i) {
      BigRational f = input[i][0].multiply(input[i][0]).add(input[i][1].multiply(input[i][1]));
      
      int j = (i + 1) % 3;
      int k = (i + 2) % 3;
      
      BigRational g = input[j][0].multiply(input[k][1]).subtract(input[k][0].multiply(input[j][1]));
      
      sum = sum.add(f.multiply(g));
    }
    
    return sum.signum();
  }
  
  public static AffinePoint intersect(AffinePoint... points) {
    BigRational[][] t = new BigRational[2][3];
    for (int i = 0; i != 2; ++i) {
      AffinePoint a = points[2 * i + 0];
      AffinePoint b = points[2 * i + 1];
      
      t[i][0] = a.getY().subtract(b.getY());
      t[i][1] = b.getX().subtract(a.getX());
      t[i][2] = a.getX().multiply(b.getY()).subtract(b.getX().multiply(a.getY()));
    }
    
    BigRational[] r = new BigRational[3];
    for (int i = 0; i != 3; ++i) {
      int j = (i + 1) % 3;
      int k = (i + 2) % 3;
      
      r[i] = t[0][j].multiply(t[1][k]).subtract(t[1][j].multiply(t[0][k]));
    }
    
    return new AffinePoint(r[0].divide(r[2]), r[1].divide(r[2]));
  }
  
  public static BigRational getSlope(AffinePoint a, AffinePoint b) {
    return getSlope(b.subtract(a));
  }
  
  // non-euclidean
  public static BigRational getSlope(AffinePoint a) {
    if (a.equals(AffinePoint.ZERO))
      throw new IllegalArgumentException();
    
    BigRational x = a.getX();
    BigRational y = a.getY();
    
    BigRational v = y.divide(x.abs().add(y.abs()));
    
    if (x.signum() >= 0)
      return v;
    
    return new BigRational(2).subtract(v);
  }
  
  public static BigRational getSlopeMod180(BigRational slope) {
    if (!(slope.compareTo(new BigRational(1)) < 0))
      slope = slope.subtract(new BigRational(2));
    
    return slope;
  }
  
  public static BigRational mirrorSlope(BigRational slope) {
    slope = slope.add(new BigRational(2));
    if (slope.compareTo(new BigRational(3)) < 0)
      return slope;
    
    return slope.subtract(new BigRational(2));
  }
  
  public static AffinePoint getSlopeVector(BigRational slope) {
    int numRots = slope.add(BigRational.ONE).intValue();
    slope = slope.subtract(new BigRational(numRots));
    
    AffinePoint result;
    if (slope.equals(new BigRational(-1)))
      result = new AffinePoint(0, -1);
    else
      result = new AffinePoint(BigRational.ONE, slope.invert().add(BigRational.ONE).invert());
    
    for (int i = 0; i != numRots; ++i)
      result = result.rot90();
    
    return result;
  }
  
  public static void test(BigRational slope) {
    if (!slope.equals(getSlope(getSlopeVector(slope))))
      throw new RuntimeException(slope + ", " + getSlope(getSlopeVector(slope)));
  }
  
}
