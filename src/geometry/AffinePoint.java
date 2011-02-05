package geometry;

import java.io.Serializable;
import java.math.BigInteger;

import general.numerals.BigRational;

public class AffinePoint implements Comparable<AffinePoint>, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static AffinePoint ZERO = new AffinePoint(0, 0);
  
  private BigRational x, y;
  
  public AffinePoint(BigRational x, BigRational y) {
    this.x = x;
    this.y = y;
  }
  
  public AffinePoint(BigInteger x, BigInteger y) {
    this(new BigRational(x), new BigRational(y));
  }
  
  public AffinePoint(int x, int y) {
    this(new BigRational(x), new BigRational(y));
  }
  
  public AffinePoint(double x, double y) {
    this(new BigRational(x), new BigRational(y));
  }
  
  public BigRational getX() {
    return x;
  }
  
  public BigRational getY() {
    return y;
  }
  
  public BigRational get(int i) {
    return i == 0 ? x : y;
  }
  
  public int compareTo(AffinePoint p) {
    int d = x.compareTo(p.x);
    if (d != 0)
      return d;
    
    return y.compareTo(p.y);
  }
  
  public boolean equals(Object p) {
    return p instanceof AffinePoint && compareTo((AffinePoint) p) == 0;
  }
  
  public String toString() {
    return "(" + x.doubleValue() + ", " + y.doubleValue() + ")";
  }
  
  public AffinePoint add(AffinePoint p) {
    return new AffinePoint(x.add(p.x), y.add(p.y));
  }
  
  public AffinePoint subtract(AffinePoint p) {
    return new AffinePoint(x.subtract(p.x), y.subtract(p.y));
  }
  
  public AffinePoint mul(BigRational r) {
    return new AffinePoint(x.multiply(r), y.multiply(r));
  }
  
  public AffinePoint div(BigRational r) {
    return mul(r.invert());
  }
  
  public BigRational prod(AffinePoint p) {
    return x.multiply(p.x).add(y.multiply(p.y));
  }
  
  public BigRational normSq() {
    return prod(this);
  }
  
  public BigRational distanceSq(AffinePoint p) {
    return subtract(p).normSq();
  }
  
  public AffinePoint rot90() {
    return new AffinePoint(y.negate(), x);
  }
  
}
