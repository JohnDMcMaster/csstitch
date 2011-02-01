package general.collections;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class MapComparator<Key extends Comparable<Key>, Value extends Comparable<Value>> implements
    Comparator<Map<Key, Value>> {
  
  public int compare(Map<Key, Value> a, Map<Key, Value> b) {
    Iterator<Key> i = a.keySet().iterator();
    Iterator<Key> j = b.keySet().iterator();
    
    while (true) {
      boolean u = i.hasNext();
      boolean v = j.hasNext();
      
      if (!u && !v)
        return 0;
      
      if (!u)
        return -1;
      
      if (!v)
        return 1;
      
      Key x = i.next();
      Key y = j.next();
      
      int d = x.compareTo(y);
      if (d != 0)
        return d;
      
      d = a.get(x).compareTo(b.get(y));
      if (d != 0)
        return d;
    }
  }
}
