package geometry;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import profile.Profiler;

import general.collections.Pair;
import general.numerals.BigRational;

public class CellFinder {
  
  public static TreeSet<AffinePoint> getPoints(Collection<Pair<AffinePoint, AffinePoint>> segments) {
    TreeSet<AffinePoint> result = new TreeSet<AffinePoint>();
    for (Pair<AffinePoint, AffinePoint> segment : segments) {
      result.add(segment.getA());
      result.add(segment.getB());
    }
    
    return result;
  }
  
  public static TreeMap<AffinePoint, Boolean> buildOrientationMap(
      Collection<Pair<AffinePoint, AffinePoint>> segments, AffinePoint a, AffinePoint b) {
    TreeSet<AffinePoint> points = getPoints(segments);
    
    TreeMap<AffinePoint, Boolean> result = new TreeMap<AffinePoint, Boolean>();
    for (AffinePoint point : points)
      result.put(point, GeometryTools.decideOrientation(point, a, b) > 0);
    
    return result;
  }
  
  public static <T> void addChange(TreeMap<T, Integer> change, T point, int n) {
    Integer c = change.remove(point);
    if (c == null)
      c = 0;
    c += n;
    if (c != 0)
      change.put(point, c);
  }
  
  public static void intersectPolygon(Collection<Pair<AffinePoint, AffinePoint>> segments,
      AffinePoint a, AffinePoint b, TreeMap<AffinePoint, Integer> change) {
    AffinePoint dir = b.subtract(a);
    TreeMap<AffinePoint, Boolean> orientations = buildOrientationMap(segments, a, b);
    
    ArrayList<Pair<AffinePoint, AffinePoint>> newSegments =
        new ArrayList<Pair<AffinePoint, AffinePoint>>();
    TreeMap<BigRational, AffinePoint> boundary = new TreeMap<BigRational, AffinePoint>();
    
    for (Iterator<Pair<AffinePoint, AffinePoint>> it = segments.iterator(); it.hasNext();) {
      Pair<AffinePoint, AffinePoint> segment = it.next();
      AffinePoint[] z = new AffinePoint[] {segment.getA(), segment.getB()};
      boolean[] s = new boolean[2];
      
      for (int i = 0; i != 2; ++i) {
        s[i] = orientations.floorEntry(z[i]).getValue();
        if (change != null && !s[i])
          addChange(change, z[i], -1);
      }
      
      if (!(s[0] && s[1])) {
        it.remove();
        
        if (s[0] || s[1]) {
          AffinePoint t = GeometryTools.intersect(a, b, z[0], z[1]);
          
          if (s[0])
            segment = new Pair<AffinePoint, AffinePoint>(z[0], t);
          else
            segment = new Pair<AffinePoint, AffinePoint>(t, z[1]);
          newSegments.add(segment);
          
          if (change != null)
            addChange(change, t, 1);
          
          BigRational d = dir.prod(t);
          if (boundary.remove(d) == null)
            boundary.put(d, t);
        }
      }
    }
    
    segments.addAll(newSegments);
    
    for (Iterator<AffinePoint> it = boundary.values().iterator(); it.hasNext();)
      segments.add(new Pair<AffinePoint, AffinePoint>(it.next(), it.next()));
  }
  
  public static void intersectPolygon(Collection<Pair<AffinePoint, AffinePoint>> segments,
      Collection<Pair<AffinePoint, AffinePoint>> edges) {
    for (Pair<AffinePoint, AffinePoint> edge : edges)
      intersectPolygon(segments, edge.getA(), edge.getB(), null);
  }
  
  public static void addDistance(AffinePoint center,
      TreeMap<Pair<BigRational, AffinePoint>, Integer> distances, AffinePoint point, int n) {
    Pair<BigRational, AffinePoint> key =
        new Pair<BigRational, AffinePoint>(center.distanceSq(point), point);
    Integer c = distances.remove(key);
    if (c == null)
      c = 0;
    c += n;
    if (c != 0)
      distances.put(key, c);
  }
  
  public static AffinePoint pt(int x, int y) {
    return new AffinePoint(new BigRational(x), new BigRational(y));
  }
  
  public static Pair<AffinePoint, AffinePoint> sg(AffinePoint a, AffinePoint b) {
    return new Pair<AffinePoint, AffinePoint>(a, b);
  }
  
  public static ArrayList<Pair<AffinePoint, AffinePoint>>[] buildVoroniCells(
      Collection<Pair<AffinePoint, AffinePoint>> hull, AffinePoint... points) {
    @SuppressWarnings("unchecked")
    ArrayList<Pair<AffinePoint, AffinePoint>>[] result =
        (ArrayList<Pair<AffinePoint, AffinePoint>>[]) new ArrayList[points.length];
    
    for (int i = 0; i != points.length; ++i) {
      AffinePoint center = points[i];
      
      TreeSet<Pair<BigRational, Integer>> byDistance = new TreeSet<Pair<BigRational, Integer>>();
      for (int j = 0; j != points.length; ++j)
        if (j != i)
          byDistance.add(new Pair<BigRational, Integer>(center.distanceSq(points[j]), j));
      
      ArrayList<Pair<AffinePoint, AffinePoint>> segments =
          new ArrayList<Pair<AffinePoint, AffinePoint>>(hull);
      TreeMap<Pair<BigRational, AffinePoint>, Integer> distances =
          new TreeMap<Pair<BigRational, AffinePoint>, Integer>();
      for (Pair<AffinePoint, AffinePoint> segment : hull) {
        addDistance(center, distances, segment.getA(), 1);
        addDistance(center, distances, segment.getB(), 1);
      }
      
      for (Pair<BigRational, Integer> next : byDistance) {
        if (!(!segments.isEmpty() && next.getA().divide(new BigRational(4))
            .compareTo(distances.lastKey().getA()) < 0))
          break;
        
        int c = next.getB();
        AffinePoint dir = points[c].subtract(center).rot90();
        AffinePoint a = center.add(points[c]).div(new BigRational(2));
        AffinePoint b = a.add(dir);
        
        TreeMap<AffinePoint, Integer> changeMap = new TreeMap<AffinePoint, Integer>();
        intersectPolygon(segments, a, b, changeMap);
        for (Entry<AffinePoint, Integer> entry : changeMap.entrySet())
          addDistance(center, distances, entry.getKey(), entry.getValue());
      }
      
      result[i] = segments;
    }
    
    return result;
  }
  
  public static void simplify(Collection<Pair<AffinePoint, AffinePoint>> segments) {
    TreeMap<Pair<BigRational, BigRational>, ArrayList<Pair<AffinePoint, AffinePoint>>> sorted =
        new TreeMap<Pair<BigRational, BigRational>, ArrayList<Pair<AffinePoint, AffinePoint>>>();
    
    for (Pair<AffinePoint, AffinePoint> segment : segments) {
      BigRational slope =
          GeometryTools.getSlopeMod180(GeometryTools.getSlope(segment.getA(), segment.getB()));
      BigRational height = GeometryTools.getSlopeVector(slope).rot90().prod(segment.getA());
      Pair<BigRational, BigRational> key = new Pair<BigRational, BigRational>(slope, height);
      
      ArrayList<Pair<AffinePoint, AffinePoint>> list = sorted.get(key);
      if (list == null) {
        list = new ArrayList<Pair<AffinePoint, AffinePoint>>();
        sorted.put(key, list);
      }
      
      list.add(segment);
    }
    
    segments.clear();
    
    for (Entry<Pair<BigRational, BigRational>, ArrayList<Pair<AffinePoint, AffinePoint>>> entry : sorted
        .entrySet()) {
      AffinePoint dir = GeometryTools.getSlopeVector(entry.getKey().getA());
      ArrayList<Pair<AffinePoint, AffinePoint>> list = entry.getValue();
      
      TreeMap<Pair<BigRational, AffinePoint>, Integer> map =
          new TreeMap<Pair<BigRational, AffinePoint>, Integer>();
      for (Pair<AffinePoint, AffinePoint> segment : list)
        for (int i = 0; i != 2; ++i) {
          AffinePoint point = i == 0 ? segment.getA() : segment.getB();
          Pair<BigRational, AffinePoint> key =
              new Pair<BigRational, AffinePoint>(dir.prod(point), point);
          addChange(map, key, 2 * i - 1);
        }
      
      if (map.isEmpty())
        continue;

      @SuppressWarnings("unchecked")
      Entry<Pair<BigRational, AffinePoint>, Integer>[] entries =
          (Entry<Pair<BigRational, AffinePoint>, Integer>[]) new Entry[2];
      Iterator<Entry<Pair<BigRational, AffinePoint>, Integer>> it = map.entrySet().iterator();
      entries[1] = it.next();
      
      int a = 0, b = entries[1].getValue();
      while (it.hasNext()) {
        do {
          entries[0] = entries[1];
          a = b;
          
          entries[1] = it.next();
          b = entries[1].getValue();
        } while (a == 0);
        
        if (!(Math.abs(a) == 1 && Integer.signum(b) == -a))
          throw new RuntimeException();
        
        int c = (a + 1) / 2;
        segments.add(new Pair<AffinePoint, AffinePoint>(entries[c].getKey().getB(), entries[1 - c]
            .getKey().getB()));
        b += a;
      }
      
      if (b != 0)
        throw new RuntimeException();
    }
  }
  
  public static void buildCells(Collection<Pair<AffinePoint, AffinePoint>> hull,
      AffinePoint[] points, Collection<Pair<AffinePoint, AffinePoint>>[] regions,
      Collection<Pair<AffinePoint, AffinePoint>>[] results,
      Collection<Pair<AffinePoint, AffinePoint>> container) {
    final int minPointsOutput = 20;
    
    long a = System.currentTimeMillis();
    
    if (points.length >= minPointsOutput)
      System.err.println("called with " + points.length + " points");
    
    ArrayList<Pair<AffinePoint, AffinePoint>> holes =
        new ArrayList<Pair<AffinePoint, AffinePoint>>(hull);
    
    Profiler.start();
    ArrayList<Pair<AffinePoint, AffinePoint>>[] voroni = buildVoroniCells(container, points);
    Profiler.end();
    
    for (int i = 0; i != points.length; ++i) {
      ArrayList<Pair<AffinePoint, AffinePoint>> segments =
          new ArrayList<Pair<AffinePoint, AffinePoint>>(holes);
      
      Profiler.start();
      intersectPolygon(segments, regions[i]);
      intersectPolygon(segments, voroni[i]);
      Profiler.end();
      
      results[i].addAll(segments);
      
      for (Pair<AffinePoint, AffinePoint> segment : segments)
        holes.add(new Pair<AffinePoint, AffinePoint>(segment.getB(), segment.getA()));
    }
    
    Profiler.start();
    simplify(holes);
    Profiler.end();
    
    for (int i = 0; i != points.length; ++i) {
      Profiler.start();
      ArrayList<Pair<AffinePoint, AffinePoint>> segments =
          new ArrayList<Pair<AffinePoint, AffinePoint>>(holes);
      intersectPolygon(segments, voroni[i]);
      Profiler.end();
      
      if (segments.isEmpty())
        continue;
      
      ArrayList<Integer> newIndices = new ArrayList<Integer>();
      for (int j = 0; j != points.length; ++j)
        if (j != i) {
          ArrayList<Pair<AffinePoint, AffinePoint>> intersection =
              new ArrayList<Pair<AffinePoint, AffinePoint>>(segments);
          intersectPolygon(intersection, regions[j]);
          if (!intersection.isEmpty())
            newIndices.add(j);
        }
      
      if (newIndices.isEmpty())
        continue;
      
      if (points.length >= minPointsOutput) {
        System.err.print("point " + i + "... ");
        for (int j = 0; j != newIndices.size(); ++j)
          System.err.print(newIndices.get(j) + ", ");
        System.err.println();
      }
      
      AffinePoint[] newPoints = new AffinePoint[newIndices.size()];
      for (int j = 0; j != newIndices.size(); ++j)
        newPoints[j] = points[newIndices.get(j)];
      
      @SuppressWarnings("unchecked")
      Collection<Pair<AffinePoint, AffinePoint>>[] newRegions =
          (Collection<Pair<AffinePoint, AffinePoint>>[]) new Collection[newIndices.size()];
      for (int j = 0; j != newIndices.size(); ++j)
        newRegions[j] = regions[newIndices.get(j)];
      
      @SuppressWarnings("unchecked")
      Collection<Pair<AffinePoint, AffinePoint>>[] newResults =
          (ArrayList<Pair<AffinePoint, AffinePoint>>[]) new ArrayList[newIndices.size()];
      for (int j = 0; j != newIndices.size(); ++j)
        newResults[j] = results[newIndices.get(j)];
      
      buildCells(segments, newPoints, newRegions, newResults, container);
    }
    
    long b = System.currentTimeMillis();
    
    if (points.length >= minPointsOutput)
      System.err.println("exit: " + (b - a));
  }
  
  public static BigRational[] getBoundary(Collection<AffinePoint> points) {
    BigRational[] boundary = new BigRational[4];
    AffinePoint first = points.iterator().next();
    for (int i = 0; i != 4; ++i)
      boundary[i] = first.get(i % 2);
    
    for (AffinePoint point : points) {
      for (int i = 0; i != 2; ++i) {
        boundary[0 + i] = boundary[0 + i].min(point.get(i));
        boundary[2 + i] = boundary[2 + i].max(point.get(i));
      }
    }
    
    return boundary;
  }
  
  public static ArrayList<Pair<AffinePoint, AffinePoint>> getRectangle(BigRational[] boundary) {
    AffinePoint[] points = new AffinePoint[4];
    for (int i = 0; i != 4; ++i) {
      BigRational[] coords = new BigRational[2];
      for (int j = 0; j != 2; ++j)
        coords[j] = boundary[((i + 1 - j) % 4) / 2 * 2 + j];
      
      points[i] = new AffinePoint(coords[0], coords[1]);
    }
    
    ArrayList<Pair<AffinePoint, AffinePoint>> result =
        new ArrayList<Pair<AffinePoint, AffinePoint>>();
    for (int i = 0; i != 4; ++i)
      result.add(new Pair<AffinePoint, AffinePoint>(points[i], points[(i + 1) % 4]));
    
    return result;
  }
  
  // O(N^2 log(N)...) for localized data
  public static ArrayList<Pair<AffinePoint, AffinePoint>>[] buildCells(
      Collection<Pair<AffinePoint, AffinePoint>> hull, AffinePoint[] points,
      Collection<Pair<AffinePoint, AffinePoint>>[] regions) {
    ArrayList<Pair<AffinePoint, AffinePoint>> container =
        getRectangle(getBoundary(getPoints(hull)));
    
    @SuppressWarnings("unchecked")
    ArrayList<Pair<AffinePoint, AffinePoint>>[] results =
        (ArrayList<Pair<AffinePoint, AffinePoint>>[]) new ArrayList[points.length];
    for (int i = 0; i != points.length; ++i)
      results[i] = new ArrayList<Pair<AffinePoint, AffinePoint>>();
    
    buildCells(hull, points, regions, results, container);
    
    for (Collection<Pair<AffinePoint, AffinePoint>> entry : results)
      simplify(entry);
    
    return results;
  }
  
}
