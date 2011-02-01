package segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Segments<T extends Comparable<T>> {
  
  private T[] type;
  private TreeSet<Segment<T>> segments = new TreeSet<Segment<T>>();
  
  private TreeMap<T, Segment<T>> byStart = new TreeMap<T, Segment<T>>();
  private TreeMap<T, Segment<T>> byEnd = new TreeMap<T, Segment<T>>();
  
  public Segments(T[] type) {
    this.type = type;
  }
  
  public Segments(T[] type, Collection<Segment<T>> segments) {
    this(type);
    addAll(segments);
  }
  
  public void addAll(Collection<Segment<T>> segments) {
    for (Segment<T> segment : segments)
      add(segment);
  }
  
  public Set<Segment<T>> get() {
    return Collections.unmodifiableSet(segments);
  }
  
  public Map<T, Segment<T>> byStart() {
    return Collections.unmodifiableMap(byStart);
  }
  
  public Map<T, Segment<T>> byEnd() {
    return Collections.unmodifiableMap(byEnd);
  }
  
  public TreeSet<Segment<T>> split() {
    TreeSet<Segment<T>> result = new TreeSet<Segment<T>>();
    for (Segment<T> segment : segments)
      result.addAll(Arrays.asList(segment.split()));
    return result;
  }
  
  public void remove(Segment<T> segment) {
    if (segments.remove(segment)) {
      byStart.remove(segment.getStart());
      byEnd.remove(segment.getEnd());
    }
  }
  
  private Segment<T> merge(Segment<T> a, Segment<T> b) {
    ArrayList<T> list = new ArrayList<T>();
    list.addAll(a.getPath());
    list.remove(list.size() - 1);
    list.addAll(b.getPath());
    return new Segment<T>(type, list);
  }
  
  public void add(Segment<T> segment) {
    Segment<T> a = byEnd.get(segment.getStart());
    if (a != null) {
      remove(a);
      segment = merge(a, segment);
    }
    
    Segment<T> b = byStart.get(segment.getEnd());
    if (b != null) {
      remove(b);
      segment = merge(segment, b);
    }
    
    segments.add(segment);
    if (segment.getStart().compareTo(segment.getEnd()) != 0) {
      byStart.put(segment.getStart(), segment);
      byEnd.put(segment.getEnd(), segment);
    }
  }
  
}