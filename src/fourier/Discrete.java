package fourier;

import general.numerals.Integers;

import java.math.BigInteger;
import java.util.Set;
import java.util.TreeSet;

public class Discrete {
  
  interface FourierTransform {
    public double[][] transform(double[][] x);
  }
  
  public static void print(double[] a) {
    System.err.println("(" + a[0] + " + " + a[1] + " i)");
  }
  
  public static void transpose(double[] out, double[] a) {
    out[0] = a[0];
    out[1] = -a[1];
  }
  
  public static void transpose(double[] out) {
    out[1] = -out[1];
  }
  
  public static double norm(double[] a) {
    return a[0] * a[0] + a[1] * a[1];
  }
  
  public static void add(double[] out, double[] a, double[] b) {
    out[0] = a[0] + b[0];
    out[1] = a[1] + b[1];
  }
  
  public static void add(double[] out, double[] a) {
    out[0] += a[0];
    out[1] += a[1];
  }
  
  public static void sub(double[] out, double[] a, double[] b) {
    out[0] = a[0] - b[0];
    out[1] = a[1] - b[1];
  }
  
  public static void sub(double[] out, double[] a) {
    out[0] -= a[0];
    out[1] -= a[1];
  }
  
  public static void inv(double[] out, double[] a) {
    double s = a[0] * a[0] + a[1] * a[1];
    out[0] = a[0] / s;
    out[1] = -a[1] / s;
  }
  
  public static void mul(double[] out, double[] a, double[] b) {
    double u = a[0] * b[0] - a[1] * b[1];
    double v = a[0] * b[1] + a[1] * b[0];
    
    out[0] = u;
    out[1] = v;
  }
  
  public static void mul(double[] out, double[] a, double b) {
    out[0] = a[0] * b;
    out[1] = a[1] * b;
  }
  
  public static void mul(double[] out, double b) {
    out[0] *= b;
    out[1] *= b;
  }
  
  public static void div(double[] out, double[] a, double[] b) {
    inv(out, b);
    mul(out, a, out);
  }
  
  public static void div(double[] out, double[] a, double b) {
    out[0] = a[0] / b;
    out[1] = a[1] / b;
  }
  
  public static void div(double[] out, double b) {
    out[0] /= b;
    out[1] /= b;
  }
  
  public static void exp(double[] out, double arg) {
    out[0] = Math.cos(arg);
    out[1] = Math.sin(arg);
  }
  
  public static double[][] transformConst(double[][] x) {
    int n = x.length;
    
    double[][] roots = new double[n][2];
    for (int i = 0; i != roots.length; ++i) {
      if (i < n / 2 + 1)
        exp(roots[i], -2 * Math.PI * i / n);
      else
        transpose(roots[i], roots[n - i]);
    }
    
    double[][] xx = new double[n][2];
    double[] a = new double[2];
    for (int i = 0; i != n; ++i) {
      for (int j = 0; j != n; ++j) {
        mul(a, roots[(i * j) % n], x[j]);
        add(xx[i], a);
      }
    }
    
    return xx;
  }
  
  public static double[][] transformCooleyTurkey(double[][] x, int u, int v, FourierTransform tu,
      FourierTransform tv) {
    if (x.length != u * v)
      throw new RuntimeException();
    
    double[][][] y = new double[v][u][];
    for (int i = 0; i != x.length; ++i)
      y[i % v][i / v] = x[i];
    
    for (int i = 0; i != v; ++i)
      y[i] = tu.transform(y[i]);
    
    double[][][] z = new double[u][v][];
    double[] a = new double[2];
    for (int i = 0; i != v; ++i)
      for (int j = 0; j != u; ++j) {
        double[] b = y[i][j];
        exp(a, -2 * Math.PI * i * j / (u * v));
        mul(b, b, a);
        z[j][i] = b;
      }
    
    for (int j = 0; j != u; ++j)
      z[j] = tv.transform(z[j]);
    
    x = new double[u * v][];
    for (int j = 0; j != u; ++j)
      for (int i = 0; i != v; ++i)
        x[u * i + j] = z[j][i];
    
    return x;
  }
  
  // special case of transformCooleyTurkey for v = 2
  public static double[][] transformCooleyTurkey2(double[][] x, FourierTransform f) {
    if (!(x.length % 2 == 0))
      throw new RuntimeException();
    
    int n = x.length / 2;
    
    double[][] y = new double[n][];
    double[][] z = new double[n][];
    
    for (int i = 0; i != 2 * n; ++i)
      (i % 2 == 0 ? y : z)[i / 2] = x[i];
    
    y = f.transform(y);
    z = f.transform(z);
    
    x = new double[2 * n][2];
    double[] a = new double[2];
    for (int i = 0; i != n; ++i) {
      exp(a, -Math.PI * i / n);
      mul(a, a, z[i]);
      
      add(x[i], y[i], a);
      sub(x[i + n], y[i], a);
    }
    
    return x;
  }
  
  public static double[][] transformCooleyTurkeyPow2(double[][] x) {
    final FourierTransform t = new FourierTransform() {
      public double[][] transform(double[][] x) {
        return x.length < 32 ? transformConst(x) : transformCooleyTurkey2(x, this);
      }
    };
    
    return t.transform(x);
  }
  
  public static double[][] convolve(double[][] y, double[][] z, FourierTransform t) {
    y = t.transform(y);
    z = t.transform(z);
    
    for (int i = 0; i != y.length; ++i)
      mul(y[i], y[i], z[i]);
    
    return invertNormalize(t.transform(y));
  }
  
  public static double[][] transformRader(double[][] x, int g, boolean expand, FourierTransform t) {
    final int p = x.length;
    final int n = Integer.highestOneBit(2 * (p - 1) + 1) << 1;
    
    double[][] y = new double[expand ? n : p - 1][];
    double[][] z = new double[expand ? n : p - 1][2];
    
    long j = 1;
    for (int i = 0; i != p - 1; ++i) {
      int jj = (int) j;
      
      y[(p - 1 - i) % (p - 1)] = x[jj];
      exp(z[i], -2 * Math.PI * jj / p);
      
      j = (j * g) % p;
    }
    
    if (expand) {
      double[] zero = new double[2];
      for (int i = p - 1; i != n - (p - 1); ++i)
        y[i] = zero;
      
      for (int i = 0; i != p - 1; ++i)
        y[n - (p - 1) + i] = y[i];
    }
    
    y = convolve(y, z, t);
    
    double[][] xx = new double[p][2];
    for (int i = 0; i != p; ++i)
      add(xx[0], x[i]);
    
    j = 1;
    for (int i = 0; i != p - 1; ++i) {
      int jj = (int) j;
      
      xx[jj] = y[i];
      add(xx[jj], x[0]);
      
      j = (j * g) % p;
    }
    
    return xx;
  }
  
  public static int getGenerator(int p) {
    Set<Long> primes = Integers.factorize(p - 1).keySet();
    outer: for (int g = 1;; ++g) {
      for (long q : primes)
        if (Integers.powMod(g, (p - 1) / q, p) == 1)
          continue outer;
      
      return g;
    }
  }
  
  public static final FourierTransform TRANSFORM_CONST = new FourierTransform() {
    public double[][] transform(double[][] x) {
      return transformConst(x);
    }
  };
  
  public static final FourierTransform TRANSFORM_COOLEY_TURKEY_POW_2 = new FourierTransform() {
    public double[][] transform(double[][] x) {
      return transformCooleyTurkeyPow2(x);
    }
  };
  
  public static FourierTransform getRaderTransform(int p) {
    final int g = getGenerator(p);
    
    return new FourierTransform() {
      public double[][] transform(double[][] x) {
        return transformRader(x, g, true, TRANSFORM_COOLEY_TURKEY_POW_2);
      }
    };
  }
  
  public static FourierTransform getTransform(final int n) {
    if (n < 30)
      return TRANSFORM_CONST;
    
    if (n % 2 == 0) {
      final FourierTransform t = getTransform(n / 2);
      return new FourierTransform() {
        public double[][] transform(double[][] x) {
          return transformCooleyTurkey2(x, t);
        }
      };
    }
    
    final int p = (int) (long) Integers.factorize(n).descendingKeySet().iterator().next();
    final int q = n / p;
    final FourierTransform tp = p < 800 ? TRANSFORM_CONST : getRaderTransform(p);
    final FourierTransform tq = getTransform(n / p);
    return new FourierTransform() {
      public double[][] transform(double[][] x) {
        return transformCooleyTurkey(x, p, q, tp, tq);
      }
    };
  }
  
  public static double[][] copy(double[][] x) {
    double[][] y = new double[x.length][2];
    for (int i = 0; i != x.length; ++i)
      for (int j = 0; j != 2; ++j)
        y[i][j] = x[i][j];
    
    return y;
  }
  
  public static double var(double[][] x, double[][] y) {
    double var = 0;
    for (int i = 0; i != x.length; ++i)
      for (int j = 0; j != 2; ++j) {
        double d = x[i][j] - y[i][j];
        var += d * d;
      }
    
    return var;
  }
  
  public static void invert(double[][] x) {
    for (int i = 1; i != x.length / 2 + 1; ++i) {
      int j = x.length - i;
      double[] t = x[i];
      x[i] = x[j];
      x[j] = t;
    }
  }
  
  public static void normalize(double[][] x) {
    for (double[] y : x)
      div(y, x.length);
  }
  
  public static double[][] invertNormalize(double[][] x) {
    double[][] y = copy(x);
    normalize(y);
    invert(y);
    return y;
  }
  
  public static double getTransformError(double[][] x, FourierTransform t) {
    return Math.sqrt(var(x, invertNormalize(t.transform(t.transform(x)))));
  }
  
  public static double[][] getRandom(int n) {
    double[][] x = new double[n][2];
    for (int i = 0; i != n; ++i)
      for (int j = 0; j != 2; ++j)
        x[i][j] = Math.random();
    
    return x;
  }
  
  public static double testTransform(FourierTransform t, int n, int k) {
    double maxError = 0;
    for (int i = 0; i != 2; ++i)
      for (int j = 0; j != k; ++j)
        maxError = Math.max(maxError, getTransformError(getRandom(n), t));
    
    return maxError;
  }
  
  public static long timeTransform(FourierTransform t, int n, int k) {
    long a = System.currentTimeMillis();
    double r = testTransform(t, n, k);
    long b = System.currentTimeMillis();
    return r == -1 ? 0 : b - a;
  }
  
  public static void main(String[] args) {
    final int n = 1021;
    final FourierTransform e = getRaderTransform(n);
    
    System.err.println(timeTransform(e, n, 128));
    System.err.println(timeTransform(TRANSFORM_CONST, n, 128));
    System.err.println(timeTransform(TRANSFORM_CONST, n, 128));
    System.err.println(timeTransform(e, n, 128));
  }
}
