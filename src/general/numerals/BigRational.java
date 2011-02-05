package general.numerals;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class BigRational implements Comparable<BigRational>, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static final BigRational ZERO = new BigRational(0);
  public static final BigRational ONE = new BigRational(1);
  
  private BigInteger a, b;
  
  private void correctSign() {
    int s = b.signum();
    if (s == 0)
      throw new RuntimeException("denominator is zero");
    
    if (s < 0) {
      a = a.negate();
      b = b.negate();
    }
  }
  
  private void reduce() {
    BigInteger d = a.gcd(b);
    if (d.compareTo(BigInteger.ONE) != 0) {
      a = a.divide(d);
      b = b.divide(d);
    }
  }
  
  private BigRational(BigInteger a, BigInteger b, boolean check) {
    this.a = a;
    this.b = b;
    
    if (check) {
      correctSign();
      reduce();
    }
  }
  
  public BigRational(BigInteger a, BigInteger b) {
    this(a, b, true);
  }
  
  public BigRational(long a, long b) {
    this(BigInteger.valueOf(a), BigInteger.valueOf(b));
  }
  
  public BigRational(int a, int b) {
    this(BigInteger.valueOf(a), BigInteger.valueOf(b));
  }
  
  public BigRational(BigInteger a) {
    this(a, BigInteger.ONE, false);
  }
  
  public BigRational(int a) {
    this(BigInteger.valueOf(a));
  }
  
  public BigRational(String[] s) {
    this(new BigInteger(s[0]), new BigInteger(s[1]));
  }
  
  public BigRational(String s) {
    this(s.split("[^+\\-0-9]+"));
  }
  
  public BigRational(double d) {
    this(d == 0 ? BigInteger.ZERO : BigInteger.valueOf((long) Math.scalb(d,
        52 - Math.getExponent(d))), d == 0 ? BigInteger.ONE : BigInteger.valueOf(2).pow(
        52 - Math.getExponent(d)));
  }
  
  public BigRational negate() {
    return new BigRational(a.negate(), b, false);
  }
  
  public BigRational add(BigRational r) {
    return new BigRational(a.multiply(r.getDenominator()).add(b.multiply(r.getNominator())),
        b.multiply(r.getDenominator()));
  }
  
  public BigRational subtract(BigRational r) {
    return add(r.negate());
  }
  
  public BigRational invert() {
    BigRational r = new BigRational(b, a, false);
    r.correctSign();
    return r;
  }
  
  public BigRational multiply(BigRational r) {
    return new BigRational(a.multiply(r.getNominator()), b.multiply(r.getDenominator()));
  }
  
  public BigRational divide(BigRational r) {
    return multiply(r.invert());
  }
  
  public BigInteger getNominator() {
    return a;
  }
  
  public BigInteger getDenominator() {
    return b;
  }
  
  public BigInteger integerValue() {
    return a.divide(b);
  }
  
  public int intValue() {
    return integerValue().intValue();
  }
  
  public BigDecimal decimalValue(MathContext context) {
    return new BigDecimal(a).divide(new BigDecimal(b), context);
  }
  
  public double doubleValue() {
    return a.doubleValue() / b.doubleValue();
  }
  
  public String toString() {
    return a + "/" + b;
  }
  
  public boolean equals(Object o) {
    if (!(o instanceof BigRational))
      return false;
    
    return compareTo((BigRational) o) == 0;
  }
  
  public int compareTo(BigRational r) {
    return subtract(r).getNominator().compareTo(BigInteger.ZERO);
  }
  
  public BigRational abs() {
    return new BigRational(a.abs(), b);
  }
  
  public int signum() {
    return a.signum();
  }
  
  public BigRational min(BigRational r) {
    return compareTo(r) < 0 ? this : r;
  }
  
  public BigRational max(BigRational r) {
    return compareTo(r) > 0 ? this : r;
  }
  
}
