package general.collections;

import java.util.Collection;
import java.util.TreeSet;

public class Sets {
  
  private Sets() {
  }
  
  public static<T extends Comparable<T>> TreeSet<T> difference(Collection<T> a, Collection<T> b) {
    TreeSet<T> set = new TreeSet<T>(a);
    set.removeAll(b);
    return set;
  }
  
  public static <T extends Comparable<T>> TreeSet<T> union(Collection<T> a, Collection<T> b) {
    TreeSet<T> set = new TreeSet<T>();
    set.addAll(a);
    set.addAll(b);
    return set;
  }
  
  public static <T extends Comparable<T>> TreeSet<T> intersection(Collection<T> a, Collection<T> b) {
    TreeSet<T> set = new TreeSet<T>(a);
    set.retainAll(b);
    return set;
  }
  
  public static <T extends Comparable<T>> TreeSet<T> symmetricDifference(Collection<T> a,
      Collection<T> b) {
    TreeSet<T> set = union(a, b);
    set.removeAll(intersection(a, b));
    return set;
  }
  
}
