package general.numerals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Iterator;

public class Decimals {
  
  private Decimals() {
  }
  
  public static BigDecimal sqrt(BigDecimal y, int n, MathContext c) {
    BigDecimal x = y.add(BigDecimal.ONE, c).divide(new BigDecimal(n), c);
    BigDecimal power = new BigDecimal(0.1).pow(c.getPrecision() - 2);
    BigDecimal k = new BigDecimal(n - 1);
    BigDecimal d = new BigDecimal(n);
    for (int i = 5; i != 0;) {
      BigDecimal t = x.multiply(k, c).add(y.divide(x.pow(n - 1, c), c), c).divide(d, c);
      if (t.subtract(x, c).divide(t, c).abs().compareTo(power) < 0)
        --i;
      x = t;
    }
    return x;
  }
  
  public static BigDecimal exp(BigDecimal x, MathContext context) {
    BigDecimal t = BigDecimal.ONE;
    BigDecimal sum = BigDecimal.ZERO;
    for (int i = 1; i != 300; ++i) {
      sum = sum.add(t, context);
      t = t.multiply(x, context).divide(BigDecimal.valueOf(i), context);
    }
    return sum;
  }
  
  public static BigDecimal log(BigDecimal x, MathContext context) {
    BigDecimal y = BigDecimal.ZERO;
    while (exp(y, context).compareTo(x) <= 0)
      y = y.add(BigDecimal.ONE, context);
    
    for (int i = 0; i != 100; ++i)
      y = y.subtract(BigDecimal.ONE, context).add(exp(y.negate(), context).multiply(x, context));
    return y;
  }
  
  public static BigDecimal log(BigDecimal x, BigDecimal base, MathContext context) {
    return log(x, context).divide(log(base, context), context);
  }
  
  public static int compare(BigDecimal x, BigRational r, int precision) {
    BigInteger y = x.scaleByPowerOfTen(precision).toBigInteger();
    BigInteger power = BigInteger.valueOf(10).pow(precision);
    
    int c0 =
        r.getDenominator().multiply(y.subtract(BigInteger.ONE))
            .compareTo(r.getNominator().multiply(power));
    int c1 =
        r.getDenominator().multiply(y.add(BigInteger.ONE))
            .compareTo(r.getNominator().multiply(power));
    
    if (c0 == c1)
      return c0;
    
    return 0;
  }
  
  // (ax + b) / (cx + d)
  /*
   * (ax + b) / (cx + d) > n <=> ax + b > ncx + nd <=> (a - nc)x > nd - b <=> x > (nd - b) / (a - nc)
   */

  public static BigRational getFraction(BigInteger n, BigInteger a, BigInteger b, BigInteger c,
      BigInteger d) {
    return new BigRational(n.multiply(d).subtract(b), a.subtract(n.multiply(c)));
  }
  
  public static int getDecision(BigDecimal x, BigInteger n, BigInteger a, BigInteger b,
      BigInteger c, BigInteger d, int precision) {
    if (a.equals(n.multiply(c)))
      return n.multiply(d).compareTo(b);
    
    BigRational r = getFraction(n, a, b, c, d);
    int com = compare(x, r, precision);
    if (a.compareTo(n.multiply(c)) < 0)
      com = -com;
    return -com;
  }
  
  public static BigInteger findLowerBound(BigDecimal x, BigInteger a, BigInteger b, BigInteger c,
      BigInteger d, int precision) {
    BigInteger pow = BigInteger.ONE;
    while (true) {
      int com = getDecision(x, pow, a, b, c, d, precision);
      if (com == 0)
        return null;
      
      if (com == 1)
        break;
      
      pow = pow.multiply(BigInteger.valueOf(2));
    }
    
    BigInteger n = pow;
    while (!pow.equals(BigInteger.ZERO)) {
      pow = pow.divide(BigInteger.valueOf(2));
      BigInteger m = n.subtract(pow);
      int com = getDecision(x, m, a, b, c, d, precision);
      if (com == 0)
        return null;
      
      if (com == 1)
        n = m;
    }
    return n.subtract(BigInteger.ONE);
  }
  
  public static ArrayList<BigInteger> getContinuedFraction(BigDecimal x, int precision) {
    BigInteger a = BigInteger.ONE;
    BigInteger b = BigInteger.ZERO;
    BigInteger c = BigInteger.ZERO;
    BigInteger d = BigInteger.ONE;
    
    ArrayList<BigInteger> list = new ArrayList<BigInteger>();
    while (true) {
      BigInteger coef = findLowerBound(x, a, b, c, d, precision);
      if (coef == null)
        break;
      list.add(coef);
      
      BigInteger temp = a;
      a = c;
      c = temp.subtract(coef.multiply(c));
      
      temp = b;
      b = d;
      d = temp.subtract(coef.multiply(d));
    }
    return list;
  }
  
  // 0, 1, 2, 3, 8, 
  // 1, 0, 1, 1, 
  
  public static BigRational getConvergent(ArrayList<BigInteger> list, int n) {
    BigInteger a = BigInteger.ONE;
    BigInteger b = BigInteger.ZERO;
    BigInteger c = BigInteger.ZERO;
    BigInteger d = BigInteger.ONE;
    
    Iterator<BigInteger> it = list.iterator();
    for (; n >= 0; --n) {
      if (!it.hasNext())
        throw new RuntimeException("list of coefficients is too small");
      
      BigInteger coef = it.next();
      
      BigInteger temp = c;
      c = a;
      a = temp.add(coef.multiply(a));
      
      temp = d;
      d = b;
      b = temp.add(coef.multiply(b));
    }
    return new BigRational(a, b);
  }
  
  public static ArrayList<BigRational> getApproximations(ArrayList<BigInteger> list, int n) {
    ArrayList<BigRational> results = new ArrayList<BigRational>();
    
    for (int k = 1; k < n; ++k) {
      BigInteger orig = list.get(k);
      
      while (list.get(k).signum() > 0) {
        results.add(getConvergent(list, k));
        
        list.set(k, list.get(k).subtract(BigInteger.ONE));
      }
      
      list.set(k, orig);
    }
    
    return results;
  }
  
  public static void main(String[] args) {
    final int precision = 128;
    MathContext context = new MathContext(precision);
    
    BigDecimal x = exp(BigDecimal.valueOf(1), context);
    System.out.println("x = " + x);
    
    ArrayList<BigInteger> list = getContinuedFraction(x, precision - 2);
    System.out.println(list);
    
    for (int n = 0; n != list.size(); ++n) {
      BigRational r = getConvergent(list, n);
      BigDecimal y = r.decimalValue(context);
      System.out.println(y);
    }
  }
  
}
