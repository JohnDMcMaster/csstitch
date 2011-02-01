package general.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class CollectionComparator<Type extends Comparable<Type>> implements
    Comparator<Collection<Type>> {
  
  @SuppressWarnings("rawtypes")
  private static final CollectionComparator COMPARATOR = new CollectionComparator();
  
  @SuppressWarnings("unchecked")
  public static <Type extends Comparable<Type>> CollectionComparator<Type> get() {
    return COMPARATOR;
  }
  
  public int compare(Collection<Type> a, Collection<Type> b) {
    int d = a.size() - b.size();
    if (d != 0)
      return d;
    
    Iterator<Type> i = a.iterator();
    Iterator<Type> j = b.iterator();
    
    while (i.hasNext()) {
      d = i.next().compareTo(j.next());
      if (d != 0)
        return d;
    }
    
    return 0;
  }
  
}
