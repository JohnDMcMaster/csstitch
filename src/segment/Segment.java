package segment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Segment<T extends Comparable<T>> implements Comparable<Segment<T>>, Serializable {
  private static final long serialVersionUID = 1L;
  

  private T[] type;
  private T[] path;
  private T[] ordered;

  public Segment(T[] type, List<T> path) {
    this.type = type;
    this.path = path.toArray(type);

    ArrayList<T> list = new ArrayList<T>(path);
    Collections.reverse(list);
    T[] temp = list.toArray(type);
    ordered = compareTo(this.path, temp) < 0 ? this.path : temp;
  }

  @SuppressWarnings("unchecked")
  public Segment(T[] type, T a, T b) {
    this(type, Arrays.asList(a, b));
  }

  public T getStart() {
    return path[0];
  }

  public T getEnd() {
    return path[path.length - 1];
  }

  public T getEndpoint(int i) {
    return i == 0 ? getStart() : getEnd();
  }

  public List<T> getPath() {
    return Collections.unmodifiableList(Arrays.asList(path));
  }

  public Segment<T> reverse() {
    ArrayList<T> list = new ArrayList<T>(Arrays.asList(path));
    Collections.reverse(list);
    return new Segment<T>(type, list);
  }

  public Segment<T> asStart(T point) {
    return getStart().equals(point) ? this : reverse();
  }

  public Segment<T> asEnd(T point) {
    return getEnd().equals(point) ? this : reverse();
  }

  public Segment<T> asEndpoint(T point, int i) {
    return getEndpoint(i).equals(point) ? this : reverse();
  }
  
  public boolean isEndpoint(T point) {
    return getStart().equals(point) || getEnd().equals(point);
  }

  public Segment<T>[] split() {
    @SuppressWarnings("unchecked")
    Segment<T>[] result = new Segment[path.length - 1];
    for (int i = 0; i != result.length; ++i)
      result[i] = new Segment<T>(type, path[i], path[i + 1]);
    return result;
  }
  
  private int compareTo(T[] a, T[] b) {
    int d = a.length - b.length;
    if (d != 0)
      return d;

    for (int i = 0; i != a.length; ++i) {
      d = a[i].compareTo(b[i]);
      if (d != 0)
        return d;
    }

    return 0;
  }

  public int compareTo(Segment<T> segment) {
    return compareTo(ordered, segment.ordered);
  }
  
  public String toString() {
    return getPath().toString();
  }

}