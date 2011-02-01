package general.median;

import java.util.Comparator;
import java.util.TreeSet;

public class Median {
  
  private Median() {
  }
  
  public static <T> T median3(T a, T b, T c, Comparator<? super T> comp) {
    T r;
    if (comp.compare(a, b) > 0)
      if (comp.compare(b, c) > 0)
        r = b;
      else if (comp.compare(a, c) > 0)
        r = c;
      else
        r = a;
    else if (comp.compare(a, c) > 0)
      r = a;
    else if (comp.compare(b, c) > 0)
      r = c;
    else
      r = b;
    return r;
  }
  
  public static <T> T median5(T a, T b, T c, T d, T e, Comparator<? super T> comp) {
    if (comp.compare(a, b) < 0) {
      T t = a;
      a = b;
      b = t;
    }
    
    if (comp.compare(d, e) < 0) {
      T t = d;
      d = e;
      e = t;
    }
    
    if (comp.compare(a, d) < 0) {
      T t = a;
      a = d;
      d = t;
      t = b;
      b = e;
      e = t;
    }
    
    T r;
    if (comp.compare(b, c) < 0)
      if (comp.compare(d, c) < 0)
        if (comp.compare(b, d) < 0)
          r = d;
        else
          r = b;
      else if (comp.compare(c, e) < 0)
        r = e;
      else
        r = c;
    else if (comp.compare(b, d) < 0)
      if (comp.compare(e, b) < 0)
        r = b;
      else
        r = e;
    else if (comp.compare(c, d) < 0)
      r = d;
    else
      r = c;
    
    return r;
  }
  
  // 2.5x faster than general algorithm below
  private static <T> T[] median3(T[] line, Comparator<? super T> comp) {
    T[] result = line.clone();
    result[0] = line[0];
    for (int i = 1; i != line.length - 1; ++i)
      result[i] = median3(line[i - 1], line[i], line[i + 1], comp);
    result[line.length - 1] = line[line.length - 1];
    return result;
  }
  
  // 2x faster than general algorithm below
  private static <T> T[] median5(T[] line, Comparator<? super T> comp) {
    T[] result = line.clone();
    result[0] = line[0];
    result[1] = median3(line[0], line[1], line[2], comp);
    for (int i = 2; i != line.length - 2; ++i)
      result[i] = median5(line[i - 2], line[i - 1], line[i], line[i + 1], line[i + 2], comp);
    result[line.length - 2] =
        median3(line[line.length - 3], line[line.length - 2], line[line.length - 1], comp);
    result[line.length - 1] = line[line.length - 1];
    return result;
  }
  
  // 2.5x slower on primitive objects than spezialization (see other classes of the package)
  public static <T> T[] median(final T[] line, int n, final Comparator<? super T> comp) {
    if (n == 0)
      return line.clone();
    if (n == 1)
      return median3(line, comp);
    if (n == 2)
      return median5(line, comp);
    
    T[] result = line.clone();
    result[0] = line[0];
    
    Comparator<Integer> comparator = new Comparator<Integer>() {
      public int compare(Integer a, Integer b) {
        int d = comp.compare(line[a], line[b]);
        if (d > 0)
          return 1;
        if (d < 0)
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
