package general.collections;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class Maps {
  
  public static <A extends Comparable<A>, B extends Comparable<B>> TreeMap<B, A> invert(
      TreeMap<A, B> map) {
    TreeMap<B, A> result = new TreeMap<B, A>();
    for (Entry<A, B> entry : map.entrySet())
      result.put(entry.getValue(), entry.getKey());
    
    return result;
  }
  
  public static <A extends Comparable<A>> TreeMap<A, TreeSet<A>> invertGraph(
      TreeMap<A, TreeSet<A>> map) {
    TreeMap<A, TreeSet<A>> result = new TreeMap<A, TreeSet<A>>();
    for (A a : map.keySet())
      result.put(a, new TreeSet<A>());
    
    for (Entry<A, TreeSet<A>> entry : map.entrySet())
      for (A b : entry.getValue())
        result.get(b).add(entry.getKey());
    
    return result;
  }
  
}
