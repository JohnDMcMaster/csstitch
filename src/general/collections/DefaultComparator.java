package general.collections;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;

@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultComparator {
  
  private static boolean hasInterface(Class<?> c, String name) {
    for (Class<?> iface : c.getInterfaces())
      if (iface.getName().equals(name))
        return true;
    
    return false;
  }
  
  public static <T> Class<T> raise(Class<T> c) {
    if (c.isPrimitive()) {
      if (c == Boolean.TYPE)
        c = (Class<T>) Boolean.class;
      
      if (c == Character.TYPE)
        c = (Class<T>) Character.class;
      
      if (c == Byte.TYPE)
        c = (Class<T>) Byte.class;
      
      if (c == Short.TYPE)
        c = (Class<T>) Short.class;
      
      if (c == Integer.TYPE)
        c = (Class<T>) Integer.class;
      
      if (c == Long.TYPE)
        c = (Class<T>) Long.class;
      
      if (c == Float.TYPE)
        c = (Class<T>) Float.class;
      
      if (c == Double.TYPE)
        c = (Class<T>) Double.class;
    }
    
    return c;
  }
  
  public static <T> Comparator<T> getComparator(Class<T> c) {
    c = raise(c);
    
    if (c.isArray()) {
      Class<?> d = c.getComponentType();
      final Comparator comparator = getComparator(d);
      return new Comparator<T>() {
        public int compare(T o1, T o2) {
          int l1 = Array.getLength(o1);
          int l2 = Array.getLength(o1);
          
          int d = l1 - l2;
          if (d != 0)
            return d;
          
          for (int i = 0; i != l1; ++i) {
            d = comparator.compare(Array.get(o1, i), Array.get(o2, i));
            if (d != 0)
              return d;
          }
          
          return 0;
        };
      };
    }
    
    if (hasInterface(c, "java.lang.Comparable"))
      return new Comparator<T>() {
        public int compare(T o1, T o2) {
          return ((Comparable) o1).compareTo(o2);
        }
      };
    
    if (hasInterface(c, "java.util.Collection"))
      return new Comparator<T>() {
        public int compare(T o1, T o2) {
          return new ComparableCollection((Collection) o1).compareTo(new ComparableCollection(
              (Collection) o2));
        }
      };
    
    throw new RuntimeException("no default comparator available for class " + c.getName());
  }
  
}
