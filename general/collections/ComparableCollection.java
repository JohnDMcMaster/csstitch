package general.collections;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Collection;

public class ComparableCollection<T extends Comparable<T>, S extends Collection<T>>
    implements Comparable<ComparableCollection<T, S>>, Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private S collection;
  
  public ComparableCollection(S collection) {
    this.collection = collection;
  }
  
  public S get() {
    return collection;
  }
  
  public int compareTo(ComparableCollection<T, S> object) {
    int d = collection.size() - object.collection.size();
    if (d != 0)
      return d;
    
    Iterator<T> a = collection.iterator();
    Iterator<T> b = object.collection.iterator();
    
    while (a.hasNext()) {
      T m = a.next();
      T n = b.next();
      
      d = m.compareTo(n);
      if (d != 0)
        return d;
    }
    
    return 0;
  }
  
  public String toString() {
    return collection.toString();
  }
  
}
