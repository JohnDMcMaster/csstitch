package metal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

public class Intervals {
  
  private TreeMap<Double, Integer> segments;
  
  private Intervals(TreeMap<Double, Integer> segments) {
    this.segments = segments;
  }
  
  public Intervals() {
    this(new TreeMap<Double, Integer>());
  }
  
  public Intervals(double from, double to) {
    this();
    add(from, to);
  }
  
  public int getNum() {
    return segments.size() / 2;
  }
  
  public Double getStart() {
    return segments.firstKey();
  }
  
  public Double getEnd() {
    return segments.lastKey();
  }
  
  public void add(double from, double to) {
    if (from >= to)
      return;
    
    segments.keySet().removeAll(
        Arrays.asList(segments.subMap(from, true, to, true).keySet().toArray(new Double[] {})));
    
    Double key;
    
    key = segments.lowerKey(from);
    if (key == null || segments.get(key) == -1)
      segments.put(from, 1);
    
    key = segments.higherKey(to);
    if (key == null || segments.get(key) == 1)
      segments.put(to, -1);
  }
  
  public void remove(double from, double to) {
    if (from >= to)
      return;
    
    segments.keySet().removeAll(segments.subMap(from, true, to, true).keySet());
    
    Double key;
    
    key = segments.lowerKey(from);
    if (key != null && segments.get(key) == 1)
      segments.put(from, -1);
    
    key = segments.higherKey(to);
    if (key != null && segments.get(key) == -1)
      segments.put(to, 1);
  }
  
  public void addAll(Intervals intervals) {
    Iterator<Double> i = intervals.segments.keySet().iterator();
    while (i.hasNext())
      add(i.next(), i.next());
  }
  
  public void removeAll(Intervals intervals) {
    Iterator<Double> i = intervals.segments.keySet().iterator();
    while (i.hasNext())
      add(i.next(), i.next());
  }
  
  public boolean equals(Object object) {
    if (!(object instanceof Intervals))
      return false;
    
    Intervals intervals = (Intervals) object;
    return segments.equals(intervals.segments);
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    Iterator<Double> i = segments.keySet().iterator();
    while (i.hasNext()) {
      if (builder.length() != 0)
        builder.append(", ");
      builder.append("[" + i.next() + ", " + i.next() + "]");
    }
    return builder.toString();
  }
  
}
