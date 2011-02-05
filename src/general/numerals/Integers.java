package general.numerals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Integers {
  
  private Integers() {
  }
  
  public static boolean isSquare(int x) {
    int t = (int) (Math.sqrt(x) + 0.5);
    return t * t == x;
  }
  
  public static boolean isSquare(long x) {
    long r = getRoot(x);
    return r * r == x;
  }
  
  public static int getRoot(long x) {
    int a = 1 << 30;
    long s = 0;
    while (a != 0) {
      if ((s + a) * (s + a) <= x)
        s += a;
      
      a >>= 1;
    }
    
    return (int) s;
  }
  
  public static BigInteger square(BigInteger a) {
    return a.multiply(a);
  }
  
  public static BigRational square(BigRational a) {
    return a.multiply(a);
  }
  
  public static BigInteger sqrt(BigInteger a) {
    BigInteger c = BigInteger.ZERO;
    BigInteger b = BigInteger.valueOf(2).pow(a.bitLength() / 2 + 1);
    
    while (b.signum() != 0) {
      if (square(c.add(b)).compareTo(a) <= 0)
        c = c.add(b);
      
      b = b.divide(BigInteger.valueOf(2));
    }
    
    return c;
  }
  
  public static BigRational sqrt(BigRational a) {
    BigInteger b = sqrt(a.getNominator());
    BigInteger c = sqrt(a.getDenominator());
    
    if (!square(b).equals(a.getNominator()) || !square(c).equals(a.getDenominator()))
      return null;
    
    return new BigRational(b, c);
  }
  
  public static long getGcd(long a, long b) {
    while (b != 0) {
      long temp = a % b;
      a = b;
      b = temp;
    }
    return a;
  }
  
  public static long powMod(long a, long e, int m) {
    long factor = 1;
    a = a % m;
    while (e != 0)
      if (e % 2 == 1) {
        factor = (factor * a) % m;
        --e;
      } else {
        a = (a * a) % m;
        e /= 2;
      }
    return factor;
  }
  
  public static BigInteger factorial(int n) {
    if (n == 0)
      return BigInteger.ONE;
    return factorial(n - 1).multiply(BigInteger.valueOf(n));
  }
  
  public static BigInteger choose(int n, int k) {
    if (k < 0 || k > n)
      return BigInteger.ZERO;
    
    if (2 * k > n)
      k = n - k;
    
    if (k == 0)
      return BigInteger.ONE;
    
    return choose(n, k - 1).multiply(BigInteger.valueOf(n - k + 1)).divide(BigInteger.valueOf(k));
  }
  
  public static long getDivisor(long s) {
    long t = (long) Math.sqrt(s) + 10;
    for (long i = 2; i != t; ++i)
      if (s % i == 0)
        return i;
    return s;
  }
  
  public static TreeMap<Long, Integer> factorize(long s) {
    TreeMap<Long, Integer> map = new TreeMap<Long, Integer>();
    while (s != 1) {
      long d = getDivisor(s);
      if (map.containsKey(d))
        map.put(d, map.get(d) + 1);
      else
        map.put(d, 1);
      s /= d;
    }
    return map;
  }
  
  public static long getDivisor(long s, int[] primes) {
    long t = (long) Math.sqrt(s) + 10;
    for (int p : primes) {
      if (p > t)
        return s;
      
      if (s % p == 0)
        return p;
    }
    
    throw new RuntimeException("prime table not large enough");
  }
  
  public static TreeMap<Long, Integer> factorize(long s, int[] primes) {
    TreeMap<Long, Integer> map = new TreeMap<Long, Integer>();
    while (s != 1) {
      long d = getDivisor(s, primes);
      if (map.containsKey(d))
        map.put(d, map.get(d) + 1);
      else
        map.put(d, 1);
      s /= d;
    }
    return map;
  }
  
  public static TreeMap<Long, Integer> factorize(int s, int[] factors) {
    TreeMap<Long, Integer> map = new TreeMap<Long, Integer>();
    while (s != 1) {
      long d = factors[s];
      if (map.containsKey(d))
        map.put(d, map.get(d) + 1);
      else
        map.put(d, 1);
      s /= d;
    }
    return map;
  }
  
  public static long phi(long n) {
    TreeMap<Long, Integer> map = Integers.factorize(n);
    for (Iterator<Long> i = map.keySet().iterator(); i.hasNext();) {
      long a = i.next();
      n = n / a * (a - 1);
    }
    return n;
  }
  
  public static TreeSet<Long> getDivisors(TreeMap<Long, Integer> map) {
    TreeSet<Long> set = new TreeSet<Long>();
    Long[] keys = map.keySet().toArray(new Long[0]);
    int[] k = new int[keys.length];
    int a = -1;
    while (a != k.length) {
      long p = 1;
      for (a = 0; a != k.length; ++a)
        for (int i = 0; i != k[a]; ++i)
          p *= keys[a];
      set.add(p);
      
      for (a = 0; a != k.length; ++a)
        if (++k[a] > map.get(keys[a]))
          k[a] = 0;
        else
          break;
    }
    return set;
  }
  
  public static int getNumDivisors(TreeMap<Long, Integer> map) {
    int p = 1;
    for (Iterator<Long> i = map.keySet().iterator(); i.hasNext();)
      p *= map.get(i.next()) + 1;
    return p;
  }
  
  public static long getSumOfDivisors(TreeMap<Long, Integer> map) {
    long p = 1;
    for (Iterator<Long> i = map.keySet().iterator(); i.hasNext();) {
      long l = i.next();
      int k = map.get(l);
      long t = 1;
      for (int j = 0; j <= k; ++j)
        t *= l;
      p *= (t - 1) / (l - 1);
    }
    return p;
  }
  
  public static TreeSet<Integer> getPrimes(int limit) {
    TreeSet<Integer> set = new TreeSet<Integer>();
    BitSet sieve = new BitSet(limit);
    for (int i = 2; i != limit; ++i) {
      if (i % 1000000 == 0)
        System.out.println(i);
      if (!sieve.get(i)) {
        set.add(i);
        for (int j = 2 * i; j < limit; j += i)
          sieve.set(j);
      }
    }
    return set;
  }
  
  public static int[] convertCollection(Collection<Integer> c) {
    int[] a = new int[c.size()];
    Iterator<Integer> i = c.iterator();
    for (int j = 0; j != a.length; ++j)
      a[j] = i.next();
    
    return a;
  }
  
  public static boolean isSquareFree(TreeMap<Long, Integer> map) {
    for (Iterator<Long> i = map.keySet().iterator(); i.hasNext();)
      if (map.get(i.next()) > 1)
        return false;
    return true;
  }
  
  public static int[] getFactorTable(int limit, Set<Integer> set) {
    int[] sieve = new int[limit];
    for (int i = 2; i != limit; ++i) {
      if (i % 100000 == 0)
        System.out.println(i);
      if (sieve[i] == 0) {
        if (set != null)
          set.add(i);
        for (int j = i; j < limit; j += i)
          sieve[j] = i;
      }
    }
    return sieve;
  }
  
  public static int isSquareFree(int[] factors, int n) {
    TreeSet<Integer> set = new TreeSet<Integer>();
    int count = 0;
    while (n != 1) {
      if (!set.add(factors[n]))
        return -1;
      n /= factors[n];
      ++count;
    }
    return count;
  }
  
  public static BigRational simplify(BigRational a, int scale) {
    int m = Math.min(a.getNominator().bitLength(), a.getDenominator().bitLength());
    if (m <= scale)
      return a;
    
    BigInteger power = BigInteger.valueOf(2).pow(m - scale);
    return new BigRational(a.getNominator().divide(power), a.getDenominator().divide(power));
  }
  
  public static String format(BigRational a, MathContext context) {
    return Double.toString(new BigDecimal(a.getNominator()).divide(
        new BigDecimal(a.getDenominator()), context).doubleValue());
  }
  
  public static long phi(long n, TreeMap<Long, Integer> map) {
    for (long p : map.keySet())
      n = n / p * (p - 1);
    
    return n;
  }
  
}
