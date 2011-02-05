package general.collections;

import java.io.Serializable;

public class Pair<S extends Comparable<? super S>, T extends Comparable<? super T>> implements
    Comparable<Pair<S, T>>, Serializable {
  private static final long serialVersionUID = 1L;
  
  private S a;
  private T b;
  
  public Pair(S a, T b) {
    this.a = a;
    this.b = b;
  }
  
  public S getA() {
    return a;
  }
  
  public T getB() {
    return b;
  }
  
  public Pair<T, S> swap() {
    return new Pair<T, S>(b, a);
  }
  
  public int compareTo(Pair<S, T> pair) {
    int c = a.compareTo(pair.getA());
    if (c != 0)
      return c;
    
    return b.compareTo(pair.getB());
  }
  
  @SuppressWarnings("unchecked")
  public boolean equals(Object obj) {
    if (!(obj instanceof Pair))
      return false;
    
    return compareTo((Pair<S, T>) obj) == 0;
  }
  
  public String toString() {
    return "(" + a + ", " + b + ")";
  }
  
}
