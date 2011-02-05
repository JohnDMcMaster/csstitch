package realmetal;

import general.collections.ComparableCollection;
import general.collections.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import metal.Intervals;
import metal.MetalTools;

import cache.Cache;
import distributed.Bootstrap;
import distributed.server.Servers;
import distributed.slaves.SlavePool;
import distributed.tunnel.Tunnel;

public final class CircleShapeGenerator {
  
  private static final int[][] VERTICES = { {-1, 0}, {1, 0}, {0, -1}, {0, 1}};
  
  private CircleShapeGenerator() {
  }
  
  public static
      TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>>
      computeCirclesLine(final float radiusLow, final float radiusHigh, final int depth,
          final float cy) {
    final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> result =
        new TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>>();
    
    final Intervals circle = new Intervals(0, 4);
    
    System.err.println("cy = " + cy);
    for (float cx = -1f; cx != 1f; cx += 1f / (1 << depth)) {
      final TreeMap<Double, TreeSet<Pair<Integer, Integer>>> points =
          new TreeMap<Double, TreeSet<Pair<Integer, Integer>>>();
      final TreeMap<Pair<Integer, Integer>, Intervals> angles =
          new TreeMap<Pair<Integer, Integer>, Intervals>();
      
      for (int y = (int) -(radiusHigh + 1); y <= radiusHigh + 1; ++y)
        for (int x = (int) -(radiusHigh + 1); x <= radiusHigh + 1; ++x) {
          if ((x + y) % 2 == 0)
            continue;
          
          final float dx = x - cx;
          final float dy = y - cy;
          final double d = Math.sqrt(dx * dx + dy * dy);
          if (!(d >= radiusLow - 1 && d <= radiusHigh + 1))
            continue;
          
          TreeSet<Pair<Integer, Integer>> set = points.get(d);
          if (set == null) {
            set = new TreeSet<Pair<Integer, Integer>>();
            points.put(d, set);
          }
          Pair<Integer, Integer> point = new Pair<Integer, Integer>(x, y);
          set.add(point);
          
          TreeSet<Double> angleSet = new TreeSet<Double>();
          for (int i = 0; i != 4; ++i) {
            final double rx = dx + VERTICES[i][0];
            final double ry = dy + VERTICES[i][1];
            final double val = rx / (Math.abs(rx) + Math.abs(ry));
            angleSet.add(ry >= 0 ? 1 - val : 3 + val);
          }
          
          double from = angleSet.first();
          double to = angleSet.last();
          final Iterator<Double> i = angleSet.iterator();
          double last = i.next();
          while (i.hasNext()) {
            double next = i.next();
            if (next - last > 2) {
              from = next;
              to = last;
            }
            last = next;
          }
          
          final Intervals intervals = new Intervals();
          if (from < to)
            intervals.add(from, to);
          else {
            intervals.add(0, to);
            intervals.add(from, 4);
          }
          angles.put(point, intervals);
        }
      
      loop: for (float r = radiusLow; r <= radiusHigh; r += 1f / (1 << depth)) {
        final TreeMap<Double, TreeSet<Pair<Integer, Integer>>> sortedPoints =
            new TreeMap<Double, TreeSet<Pair<Integer, Integer>>>();
        for (final double d : points.keySet())
          sortedPoints.put(Math.abs(d - r), points.get(d));
        
        final TreeSet<Pair<Integer, Integer>> shape = new TreeSet<Pair<Integer, Integer>>();
        
        final Intervals intervals = new Intervals();
        for (final double d : sortedPoints.keySet()) {
          TreeSet<Pair<Integer, Integer>> pp = sortedPoints.get(d);
          shape.addAll(pp);
          for (final Pair<Integer, Integer> p : pp)
            intervals.addAll(angles.get(p));
          
          if (intervals.equals(circle)) {
            final ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>> temp =
                new ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>(
                    shape);
            Pair<Integer, Pair<Float, Float>> center = result.get(temp);
            if (center == null)
              center = new Pair<Integer, Pair<Float, Float>>(0, new Pair<Float, Float>(0f, 0f));
            
            Pair<Float, Float> sum = center.getB();
            sum = new Pair<Float, Float>(sum.getA() + cx, sum.getB() + cy);
            center = new Pair<Integer, Pair<Float, Float>>(center.getA() + 1, sum);
            result.put(temp, center);
            continue loop;
          }
        }
      }
    }
    
    return result;
  }
  
  private static
      void
      join(
          TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> acc,
          final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> arg) {
    for (final ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>> shape : arg
        .keySet()) {
      Pair<Integer, Pair<Float, Float>> center = acc.get(shape);
      if (center == null)
        center = new Pair<Integer, Pair<Float, Float>>(0, new Pair<Float, Float>(0f, 0f));
      
      final Pair<Integer, Pair<Float, Float>> newCenter = arg.get(shape);
      
      final int n0 = center.getA();
      final int n1 = newCenter.getA();
      
      final Pair<Float, Float> c0 = center.getB();
      final Pair<Float, Float> c1 = newCenter.getB();
      
      final float x = (n0 * c0.getA() + n1 * c1.getA()) / (n0 + n1);
      final float y = (n0 * c0.getB() + n1 * c1.getB()) / (n0 + n1);
      
      acc.put(shape, new Pair<Integer, Pair<Float, Float>>(n0 + n1, new Pair<Float, Float>(x, y)));
    }
  }
  
  private static
      TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>>
      flattenCenters(
          final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> acc) {
    final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>> result =
        new TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>>();
    for (final ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>> shape : acc
        .keySet()) {
      final Pair<Integer, Pair<Float, Float>> center = acc.get(shape);
      
      final int n = center.getA();
      final Pair<Float, Float> c = center.getB();
      
      result.put(shape, new Pair<Float, Float>(c.getA() / n, c.getB() / n));
    }
    
    return result;
  }
  
  private static boolean DISTRIBUTED = true;
  
  private static
      TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>>
      computeCircles(final float radiusLow, final float radiusHigh, final int depth) {
    final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> acc =
        new TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>>();
    
    if (!DISTRIBUTED) {
      for (float cy = -0.5f; cy != 0.5f; cy += 1f / (1 << depth))
        join(acc, computeCirclesLine(radiusLow, radiusHigh, depth, cy));
    } else {
      try {
        SlavePool pool = new SlavePool(new String[] {"-server", "-Xmx2G"}, -1);
        
        for (float cy = -0.5f; cy != 0.5f; cy += 1f / (1 << depth)) {
          Thread.sleep(100);
          
          final float c = cy;
          
          pool.submit(
              new SlavePool.SimpleCallback<TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>>>() {
                public
                    void
                    callback(
                        TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Integer, Pair<Float, Float>>> result) {
                  synchronized (acc) {
                    join(acc, result);
                    System.err.println("job done: " + c);
                  }
                }
              }, Tunnel.getMethod("computeCirclesLine"), radiusLow, radiusHigh, depth, cy);
        }
        
        pool.waitTillFinished();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    System.err.println(acc.size() + " shapes");
    return flattenCenters(acc);
  }
  
  private static
      int[][][]
      getShapes(
          final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>> circles) {
    final int[][][] shapes = new int[circles.size()][][];
    int i = 0;
    for (final ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>> shape : circles
        .keySet()) {
      final TreeSet<Pair<Integer, Integer>> set = shape.get();
      shapes[i] = new int[set.size()][2];
      int j = 0;
      for (final Pair<Integer, Integer> point : set) {
        shapes[i][j][0] = point.getA();
        shapes[i][j][1] = point.getB();
        ++j;
      }
      ++i;
    }
    
    return shapes;
  }
  
  private static
      float[][]
      getCenters(
          final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>> circles) {
    final float[][] centers = new float[circles.size()][2];
    int i = 0;
    for (final ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>> shape : circles
        .keySet()) {
      final Pair<Float, Float> c = circles.get(shape);
      centers[i][0] = c.getA();
      centers[i][1] = c.getB();
      ++i;
    }
    
    return centers;
  }
  
  private static
      Object[]
      computeData(
          final TreeMap<ComparableCollection<Pair<Integer, Integer>, TreeSet<Pair<Integer, Integer>>>, Pair<Float, Float>> circles) {
    final int[][][] shapes = getShapes(circles);
    final float[][] centers = getCenters(circles);
    
    final int[] boundaries = MetalTools.computeBoundaries(shapes);
    final BinaryImage[] renderedShapes = new BinaryImage[shapes.length];
    for (int i = 0; i != shapes.length; ++i) {
      renderedShapes[i] = new BinaryImage(MetalTools.render(shapes[i], boundaries));
      for (int j = 0; j != shapes[i].length; ++j) {
        shapes[i][j][0] -= boundaries[0];
        shapes[i][j][1] -= boundaries[1];
      }
      
      centers[i][0] -= boundaries[0];
      centers[i][1] -= boundaries[1];
    }
    
    final int[] anchor = Arrays.copyOfRange(boundaries, 0, 2);
    final int[] size = Arrays.copyOfRange(boundaries, 2, 4);
    for (int i = 0; i != 2; ++i)
      size[i] -= anchor[i];
    
    return new Object[] {anchor, size, shapes, centers};
  }
  
  public static Object[] computeCircleData(final float radiusLow, final float radiusHigh,
      final int depth) throws IOException {
    final Object[] result = Cache.cache("circle-shapes/%f-%f/%d", radiusLow, radiusHigh, depth);
    if (result != null)
      return result;
    
    return computeData(computeCircles(radiusLow, radiusHigh, depth));
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    
    System.out.println(((int[][][]) computeCircleData(4.5f, 4.75f, 12)[2]).length);
  }
  
}
