package realmetal;

import general.collections.Pair;
import general.median.MedianDouble;

import ids.TaggedObjects;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class Edge implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  public static class EdgeException extends Exception {
    private static final long serialVersionUID = 1L;
  }
  
  private TaggedObjects<Edge> edges;
  private long id;
  
  private int dir;
  private TreeMap<Integer, TreeSet<Integer>> segments;
  
  private double[] endIndices = new double[2];
  private double[] ends = new double[2];
  
  private long[] next = new long[2];
  private int[] nextEnds = new int[2];
  
  private static TreeSet<Integer> get(TreeMap<Integer, TreeSet<Integer>> map, int index) {
    TreeSet<Integer> set = map.get(index);
    if (set == null) {
      set = new TreeSet<Integer>();
      map.put(index, set);
    }
    
    return set;
  }
  
  public static boolean match(Edge a, int aEnd, Edge b, int bEnd) {
    final int maxDifference = 4;
    
    if (a.dir == b.dir)
      throw new RuntimeException("wrong directions");
    
    double x = b.getEndIndex(bEnd);
    double y = a.getEndIndex(aEnd);
    
    double xx = a.getEnd(aEnd);
    double yy = b.getEnd(bEnd);
    
    return Math.abs(x - xx) <= maxDifference && Math.abs(y - yy) <= maxDifference;
  }
  
  public static boolean link(long aId, Edge a, int aEnd, long bId, Edge b, int bEnd) {
    if (!(a.next[aEnd] == -1 && b.next[bEnd] == -1))
      throw new RuntimeException("edges already linked");
    
    if (!match(a, aEnd, b, bEnd))
      return false;
    
    a.next[aEnd] = bId;
    a.nextEnds[aEnd] = bEnd;
    
    b.next[bEnd] = aId;
    b.nextEnds[bEnd] = aEnd;
    
    return true;
  }
  
  public Edge(TaggedObjects<Edge> edges, int dir) {
    this.edges = edges;
    this.dir = dir;
    segments = new TreeMap<Integer, TreeSet<Integer>>();
  }
  
  public void add() {
    id = edges.add(this);
  }
  
  public void remote() {
    if (!(next[0] != -1 && next[1] != -1))
      throw new RuntimeException("cannot untag edge");
    
    edges.remove(id);
    id = -1;
  }
  
  public long getId() {
    return id;
  }
  
  public int getDir() {
    return dir;
  }
  
  public Set<Integer> getIndices() {
    return Collections.unmodifiableSet(segments.keySet());
  }
  
  public Set<Integer> getSegments(int index) {
    return Collections.unmodifiableSet(segments.get(index));
  }
  
  public double getEnd(int i) {
    return ends[i];
  }
  
  public double getEndIndex(int i) {
    return endIndices[i];
  }
  
  public long getNext(int i) {
    return next[i];
  }
  
  public int getNextEnd(int i) {
    return nextEnds[i];
  }
  
  private TreeSet<Integer> getLine(int index) {
    return get(segments, index);
  }
  
  public void addSegment(int index, int from, int to) {
    TreeSet<Integer> set = getLine(index);
    for (int i = from; i != to; ++i)
      set.add(i);
  }
  
  public void addEdge(Edge edge) throws EdgeException {
    if (dir != edge.dir)
      throw new RuntimeException("direction do not match");
    
    for (int i = 0; i != 2; ++i) {
      if (next[i] != -1 && edge.next[i] != -1)
        throw new EdgeException();
      
      if (edge.next[i] != -1) {
        next[i] = edge.next[i];
        nextEnds[i] = edge.nextEnds[i];
      }
    }
    
    for (Entry<Integer, TreeSet<Integer>> entry : edge.segments.entrySet())
      getLine(entry.getKey()).addAll(entry.getValue());
  }
  
  public void computeInformation() throws EdgeException {
    final int median = 2;
    final int maxEdgeWidth = 8;
    final int windowSize = 32;
    final double maxSpan = 2;
    final int backStep = 8;
    
    if (id == -1)
      throw new RuntimeException("edge not tagged");
    
    TreeMap<Integer, TreeSet<Integer>> remapped = new TreeMap<Integer, TreeSet<Integer>>();
    for (Entry<Integer, TreeSet<Integer>> entry : segments.entrySet())
      for (int value : entry.getValue())
        get(remapped, value).add(entry.getKey());
    
    int from = remapped.firstKey();
    int to = remapped.lastKey();
    if (remapped.size() != to - from + 1)
      throw new EdgeException();
    
    double[] means = new double[to - from + 1];
    for (int i = from; i <= to; ++i) {
      TreeSet<Integer> set = remapped.get(i);
      int a = set.first();
      int b = set.last();
      if (set.size() != a - b + 1)
        throw new EdgeException();
      
      if (b - a >= maxEdgeWidth)
        throw new EdgeException();
      
      means[i - from] = 0.5 * (a + b + 1);
    }
    
    means = MedianDouble.median(means, median);
    
    @SuppressWarnings("unchecked")
    Pair<Double, Integer>[] pairs = new Pair[to - from + 1];
    for (int i = from; i <= to; ++i)
      pairs[i - from] = new Pair<Double, Integer>(means[i - from], i);
    
    TreeSet<Pair<Double, Integer>> set = new TreeSet<Pair<Double, Integer>>();
    for (int i = from; i <= to; ++i) {
      set.add(pairs[i - from]);
      if (i >= from + windowSize)
        set.remove(pairs[i - windowSize - from]);
      
      double span = set.last().getA() - set.first().getA();
      if (span > maxSpan)
        throw new EdgeException();
    }
    
    int back = Math.min(backStep, (to - from) / 2);
    
    endIndices[0] = means[back];
    endIndices[1] = means[to - from - back];
    
    for (int i = 0; i != 2; ++i) {
      int sum = 0;
      TreeSet<Integer> indices = remapped.get(i == 0 ? from + back : to - back);
      for (int index : indices)
        sum += segments.get(index).first();
      ends[i] = (double) sum / indices.size() + 0.5 + (i == 0 ? -1 : 1);
    }
    
    Edge[] nextEdges = new Edge[2];
    for (int i = 0; i != 2; ++i)
      if (next[i] != -1) {
        nextEdges[i] = edges.get(next[i]);
        if (!match(this, i, nextEdges[i], nextEnds[i]))
          throw new EdgeException();
      }
    
    id = edges.add(this);
    for (int i = 0; i != 2; ++i)
      if (next[i] != -1)
        nextEdges[i].next[nextEnds[i]] = id;
  }
  
}
