package general;

import general.collections.Sets;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class Statistics {
  
  public static <T extends Comparable<T>> int numDistinctEntries(List<T> list) {
    TreeSet<T> set = new TreeSet<T>();
    for (T t : list)
      set.add(t);
    return set.size();
  }
  
  public static <T extends Comparable<T>> TreeMap<T, Integer>
      getHistogram(Collection<T> collection) {
    TreeMap<T, Integer> map = new TreeMap<T, Integer>();
    for (T t : collection) {
      if (!map.containsKey(t))
        map.put(t, 0);
      map.put(t, map.get(t) + 1);
    }
    return map;
  }
  
  public static <T extends Comparable<T>> TreeMap<T, Double> normalizeDistribution(
      Map<T, Integer> map) {
    long sum = 0;
    for (Integer value : map.values())
      sum += value;
    
    TreeMap<T, Double> normalized = new TreeMap<T, Double>();
    for (T key : map.keySet())
      normalized.put(key, map.get(key) / (double) sum);
    return normalized;
  }
  
  public static TreeMap<Character, Double> getLetterDistribution() {
    TreeMap<Character, Integer> h = new TreeMap<Character, Integer>();
    h.put('a', 8167);
    h.put('b', 1492);
    h.put('c', 2782);
    h.put('d', 4253);
    h.put('e', 12702);
    h.put('f', 2228);
    h.put('g', 2015);
    h.put('h', 6094);
    h.put('i', 6966);
    h.put('j', 153);
    h.put('k', 772);
    h.put('l', 4025);
    h.put('m', 2406);
    h.put('n', 6749);
    h.put('o', 7507);
    h.put('p', 1929);
    h.put('q', 95);
    h.put('r', 5987);
    h.put('s', 6327);
    h.put('t', 9056);
    h.put('u', 2758);
    h.put('v', 978);
    h.put('w', 2360);
    h.put('x', 150);
    h.put('y', 1974);
    h.put('z', 74);
    return normalizeDistribution(h);
  }
  
  public static <T extends Comparable<T>> TreeMap<T, Double> getDependentDistribution(
      TreeMap<T, Double> a, TreeMap<T, Double> b) {
    TreeMap<T, Double> map = new TreeMap<T, Double>();
    for (T key : b.keySet()) {
      Double valA = a.get(key);
      if (valA == null)
        valA = 0.;
      map.put(key, valA / b.get(key));
    }
    return map;
  }
  
  public static <T extends Comparable<T>> double getDistance(TreeMap<T, Double> a,
      TreeMap<T, Double> b) {
    double diff = 0.0;
    for (T key : Sets.union(a.keySet(), b.keySet())) {
      Double valA = a.get(key);
      if (valA == null)
        valA = 0.;
      
      Double valB = b.get(key);
      if (valB == null)
        valB = 0.;
      
      double d = valA - valB;
      diff += d * d;
    }
    
    return diff;
  }
  
  public static <S, T> void printMap(Map<S, T> map) {
    for (S key : map.keySet())
      System.out.println(key + ": " + map.get(key));
    System.out.println();
  }
  
}
