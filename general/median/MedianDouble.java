package general.median;

import java.util.Comparator;
import java.util.TreeSet;

public class MedianDouble {
  
  private MedianDouble() {
  }
  
  public static double median3(double a, double b, double c) {
    double r;
    if (a > b)
      if (b > c)
        r = b;
      else if (a > c)
        r = c;
      else
        r = a;
    else if (a > c)
      r = a;
    else if (b > c)
      r = c;
    else
      r = b;
    return r;
  }
  
  public static double median5(double a, double b, double c, double d, double e) {
    if (a < b) {
      double t = a;
      a = b;
      b = t;
    }
    
    if (d < e) {
      double t = d;
      d = e;
      e = t;
    }
    
    if (a < d) {
      double t = a;
      a = d;
      d = t;
      t = b;
      b = e;
      e = t;
    }
    
    double r;
    if (b < c)
      if (d < c)
        if (b < d)
          r = d;
        else
          r = b;
      else if (c < e)
        r = e;
      else
        r = c;
    else if (b < d)
      if (e < b)
        r = b;
      else
        r = e;
    else if (c < d)
      r = d;
    else
      r = c;
    
    return r;
  }
  
  // 15x faster than general algorithm below
  private static double[] median3(double[] line) {
    double[] result = line.clone();
    result[0] = line[0];
    for (int i = 1; i != line.length - 1; ++i)
      result[i] = median3(line[i - 1], line[i], line[i + 1]);
    result[line.length - 1] = line[line.length - 1];
    return result;
  }
  
  // 10x faster than general algorithm below
  private static double[] median5(double[] line) {
    double[] result = line.clone();
    result[0] = line[0];
    result[1] = median3(line[0], line[1], line[2]);
    for (int i = 2; i != line.length - 2; ++i)
      result[i] = median5(line[i - 2], line[i - 1], line[i], line[i + 1], line[i + 2]);
    result[line.length - 2] =
        median3(line[line.length - 3], line[line.length - 2], line[line.length - 1]);
    result[line.length - 1] = line[line.length - 1];
    return result;
  }
  
  public static double[] median(final double[] line, int n) {
    if (n == 0)
      return line.clone();
    if (n == 1)
      return median3(line);
    if (n == 2)
      return median5(line);
    
    double[] result = new double[line.length];
    result[0] = line[0];
    
    Comparator<Integer> comparator = new Comparator<Integer>() {
      public int compare(Integer a, Integer b) {
        double la = line[a];
        double lb = line[b];
        if (la > lb)
          return 1;
        if (la < lb)
          return -1;
        if (a > b)
          return 1;
        if (a < b)
          return -1;
        return 0;
      }
    };
    TreeSet<Integer> set = new TreeSet<Integer>(comparator);
    int index = 0;
    set.add(0);
    
    for (int i = 1; i != line.length; ++i) {
      int d;
      if (i <= n) {
        int u = 2 * i - 1;
        int v = 2 * i;
        
        set.add(u);
        set.add(v);
        
        d = comparator.compare(u, index) + comparator.compare(v, index);
      } else if (i >= line.length - n) {
        int u = 2 * i - line.length - 1;
        int v = 2 * i - line.length;
        
        set.remove(u);
        set.remove(v);
        
        d = -comparator.compare(u, index) - comparator.compare(v, index);
      } else {
        int u = i - n - 1;
        int v = i + n;
        
        set.remove(u);
        set.add(v);
        
        d = -comparator.compare(u, index) + comparator.compare(v, index);
      }
      
      if (d > 0)
        index = set.higher(index);
      else if (d < 0)
        index = set.lower(index);
      
      result[i] = line[index];
    }
    return result;
  }
  
}
