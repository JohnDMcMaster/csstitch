package polygonization;

import general.Streams;
import general.collections.ComparableCollection;
import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import metal.Intervals;

import cache.Cache;

import segment.Segment;
import segment.Segments;

public class Blobs {
  
  public static boolean[][] loadImage(String filename) throws IOException {
    BufferedImage image = ImageIO.read(new File(filename));
    System.err.println("image loaded");
    
    WritableRaster raster = image.getRaster();
    
    boolean[][] result = new boolean[image.getHeight()][image.getWidth()];
    System.err.println("matrix created");
    
    int[] pixel = new int[1];
    for (int y = 0; y != result.length; ++y)
      for (int x = 0; x != result[0].length; ++x) {
        raster.getPixel(x, y, pixel);
        result[y][x] = pixel[0] != 0;
      }
    
    System.err.println("matrix filled");
    return result;
  }
  
  public static void saveImage(String filename, boolean[][] matrix) throws IOException {
    BufferedImage image =
        new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_BYTE_BINARY);
    WritableRaster raster = image.getRaster();
    
    int[] pixel = new int[1];
    for (int y = 0; y != matrix.length; ++y)
      for (int x = 0; x != matrix[0].length; ++x) {
        pixel[0] = matrix[y][x] ? 1 : 0;
        raster.setPixel(x, y, pixel);
      }
    
    ImageIO.write(image, "png", new File(filename));
  }
  
  @SuppressWarnings("unchecked")
  public static Segment<Long>[] getComponents(boolean[][] matrix, boolean value) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    Long[] type = new Long[] {};
    Segments<Long> segments = new Segments<Long>(type);
    
    boolean[] v = new boolean[4];
    
    int[][] table = new int[][] { {0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    
    for (int y = 0; y <= sy; ++y) {
      System.err.println(y);
      for (int x = 0; x <= sx; ++x) {
        v[0] = x != 0 && y != 0 && matrix[y - 1][x - 1] == value;
        v[1] = x != sx && y != 0 && matrix[y - 1][x] == value;
        v[2] = x != sx && y != sy && matrix[y][x] == value;
        v[3] = x != 0 && y != sy && matrix[y][x - 1] == value;
        
        for (int i = 0; i != 4; ++i) {
          if (!v[i] && v[(i + 1) % 4]) {
            for (int j = (i + 1) % 4; j != i; j = (j + 1) % 4)
              if (v[j] && !v[(j + 1) % 4]) {
                int xx0 = 2 * x + table[i][0];
                int yy0 = 2 * y + table[i][1];
                
                int xx1 = 2 * x + table[j][0];
                int yy1 = 2 * y + table[j][1];
                
                long p = xx0 | ((long) yy0 << 32);
                long q = xx1 | ((long) yy1 << 32);
                
                segments.add(new Segment<Long>(type, p, q));
                
                if (j > i)
                  i = j;
                break;
              }
          }
        }
      }
    }
    
    System.err.println(segments.get().size());
    
    return segments.get().toArray(new Segment[] {});
  }
  
  public static Object[] getComponentsBoundaries(String filename, boolean value) throws IOException {
    Object[] result = Cache.cache("blobs-comp-bound/%s/%b", filename, value);
    if (result != null)
      return result;
    
    boolean[][] matrix = loadImage(filename);
    Segment<Long>[] segments = getComponents(matrix, value);
    for (int i = 0; i != segments.length; ++i)
      segments[i] = midpointsToEndpoints(segments[i]);
    
    boolean[] type = new boolean[segments.length];
    for (int i = 0; i != segments.length; ++i)
      type[i] = getType(matrix, getRepresentation(segments[i]));
    
    return new Object[] {segments, type};
  }
  
  public static void getBoundingBox(Segment<Long> segment, int[] b) {
    int xMin = Integer.MAX_VALUE;
    int yMin = Integer.MAX_VALUE;
    
    int xMax = Integer.MIN_VALUE;
    int yMax = Integer.MIN_VALUE;
    
    for (long p : segment.getPath()) {
      int x = (int) p;
      int y = (int) (p >> 32);
      
      if (x < xMin)
        xMin = x;
      else if (x > xMax)
        xMax = x;
      
      if (y < yMin)
        yMin = y;
      else if (y > yMax)
        yMax = y;
    }
    
    b[0] = xMin / 2;
    b[1] = yMin / 2;
    
    b[2] = xMax / 2;
    b[3] = yMax / 2;
  }
  
  public static Segment<Long> midpointsToEndpoints(Segment<Long> segment) {
    ArrayList<Long> list = new ArrayList<Long>();
    
    long p0 = segment.getPath().get(segment.getPath().size() - 2);
    int x0 = (int) p0;
    int y0 = (int) (p0 >> 32);
    
    for (long p1 : segment.getPath()) {
      int x1 = (int) p1;
      int y1 = (int) (p1 >> 32);
      
      int x = (x0 + x1 + 2) / 4;
      int y = (y0 + y1 + 2) / 4;
      list.add(x + ((long) y << 32));
      
      x0 = x1;
      y0 = y1;
    }
    
    return new Segment<Long>(new Long[] {}, list);
  }
  
  public static TreeMap<Integer, TreeMap<Integer, Boolean>>
      getRepresentation(Segment<Long> segment) {
    long p0 = segment.getPath().get(segment.getPath().size() - 2);
    int x0 = (int) p0;
    int y0 = (int) (p0 >> 32);
    
    TreeMap<Integer, TreeMap<Integer, Boolean>> result =
        new TreeMap<Integer, TreeMap<Integer, Boolean>>();
    
    for (long p1 : segment.getPath()) {
      int x1 = (int) p1;
      int y1 = (int) (p1 >> 32);
      
      if (Math.abs(x0 - x1) + Math.abs(y0 - y1) != 1)
        throw new RuntimeException();
      
      if (y0 != y1) {
        int x = x0;
        int y = Math.min(y0, y1);
        
        TreeMap<Integer, Boolean> map = result.get(y);
        if (map == null) {
          map = new TreeMap<Integer, Boolean>();
          result.put(y, map);
        }
        
        map.put(x, false);
      }
      
      x0 = x1;
      y0 = y1;
    }
    
    for (TreeMap<Integer, Boolean> map : result.values()) {
      boolean value = true;
      for (int key : map.keySet().toArray(new Integer[] {})) {
        map.put(key, value);
        value = !value;
      }
    }
    
    return result;
  }
  
  public static boolean
      getType(boolean[][] matrix, TreeMap<Integer, TreeMap<Integer, Boolean>> repr) {
    Entry<Integer, TreeMap<Integer, Boolean>> entry = repr.firstEntry();
    int x = entry.getValue().firstKey();
    int y = entry.getKey();
    return matrix[y][x];
  }
  
  public static long getArea(TreeMap<Integer, TreeMap<Integer, Boolean>> repr) {
    long result = 0;
    for (TreeMap<Integer, Boolean> map : repr.values())
      for (Entry<Integer, Boolean> entry : map.entrySet())
        if (entry.getValue())
          result -= entry.getKey();
        else
          result += entry.getKey();
    
    return result;
  }
  
  public static int[][] getCircle(final float radius) {
    final Intervals circle = new Intervals(0, 4);
    
    final int[][] vertices = { {-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    
    final TreeMap<Double, TreeSet<Pair<Integer, Integer>>> points =
        new TreeMap<Double, TreeSet<Pair<Integer, Integer>>>();
    final TreeMap<Pair<Integer, Integer>, Intervals> angles =
        new TreeMap<Pair<Integer, Integer>, Intervals>();
    
    for (int y = (int) -(radius + 1); y <= radius + 1; ++y)
      for (int x = (int) -(radius + 1); x <= radius + 1; ++x) {
        if ((x + y) % 2 == 0)
          continue;
        
        final double d = Math.sqrt(x * x + y * y);
        if (!(d >= radius - 1 && d <= radius + 1))
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
          final double rx = x + vertices[i][0];
          final double ry = y + vertices[i][1];
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
    
    final TreeMap<Double, TreeSet<Pair<Integer, Integer>>> sortedPoints =
        new TreeMap<Double, TreeSet<Pair<Integer, Integer>>>();
    for (final double d : points.keySet())
      sortedPoints.put(Math.abs(d - radius), points.get(d));
    
    ArrayList<int[]> result = new ArrayList<int[]>();
    
    final Intervals intervals = new Intervals();
    for (final double d : sortedPoints.keySet()) {
      TreeSet<Pair<Integer, Integer>> pp = sortedPoints.get(d);
      for (Pair<Integer, Integer> p : pp) {
        result.add(new int[] {p.getA(), p.getB()});
        intervals.addAll(angles.get(p));
      }
      
      if (intervals.equals(circle))
        break;
    }
    
    return result.toArray(new int[][] {});
  }
  
  public static void main(String[] args) throws IOException {
    String filename = "/home/noname/Downloads/M1-metal-2.png";
    
    int sx, sy;
    {
      BufferedImage image = ImageIO.read(new File(filename));
      sx = image.getWidth();
      sy = image.getHeight();
    }
    
    Object[] objects = getComponentsBoundaries(filename, true);
    Segment<Long>[] segments = (Segment<Long>[]) objects[0];
    boolean[] type = (boolean[]) objects[1];
    final int n = segments.length;
    
    TreeMap<Integer, TreeMap<Integer, Boolean>>[] repr = new TreeMap[n];
    for (int i = 0; i != n; ++i)
      repr[i] = getRepresentation(segments[i]);
    
    /*boolean[][] matrix = loadImage(filename);
    
    for (int i = 0; i != n; ++i) {
      long a = getArea(repr[i]);
      if (!type[i] && a <= 50) {
        for (Entry<Integer, TreeMap<Integer, Boolean>> entry : repr[i].entrySet()) {
          int y = entry.getKey();
          Iterator<Integer> it = entry.getValue().keySet().iterator();
          while (it.hasNext()) {
            int x0 = it.next();
            int x1 = it.next();
            for (int x = x0; x != x1; ++x)
              matrix[y][x] = true;
          }
        }
      }
    }
    for (int i = 0; i != n; ++i) {
      long a = getArea(repr[i]);
      if (type[i] && a <= 50) {
        for (Entry<Integer, TreeMap<Integer, Boolean>> entry : repr[i].entrySet()) {
          int y = entry.getKey();
          Iterator<Integer> it = entry.getValue().keySet().iterator();
          while (it.hasNext()) {
            int x0 = it.next();
            int x1 = it.next();
            for (int x = x0; x != x1; ++x)
              matrix[y][x] = false;
          }
        }
      }
    }

    for (int i = 0; i != n; ++i) {
      long a = getArea(repr[i]);
      if (!type[i] && a >= 50 && a <= 300) {
        for (Entry<Integer, TreeMap<Integer, Boolean>> entry : repr[i].entrySet()) {
          int y = entry.getKey();
          Iterator<Integer> it = entry.getValue().keySet().iterator();
          while (it.hasNext()) {
            int x0 = it.next();
            int x1 = it.next();
            for (int x = x0; x != x1; ++x)
              matrix[y][x] = true;
          }
        }
      }
    }
    
    saveImage("/home/noname/Downloads/M1-metal-2.png", matrix);*/

    System.exit(-1);
    
    final int d = 4;
    
    int k = 0;
    
    int[][] boxes = new int[n][4];
    for (int i = 0; i != n; ++i) {
      getBoundingBox(segments[i], boxes[i]);
      k += segments[i].getPath().size();
    }
    
    System.err.println(k);
    
    int[][] circle = getCircle(0.5f * d);
    System.err.println(circle.length);
    
    long g = 0;
    TreeSet<Pair<Integer, Integer>> cc = new TreeSet<Pair<Integer, Integer>>();
    
    int[][] array = new int[sy + 1][sx + 1];
    for (int y = 0; y <= sy; ++y)
      for (int x = 0; x <= sx; ++x)
        array[y][x] = -1;
    
    for (int i = 0; i != n; ++i)
      if (type[i]) {
        for (long p : segments[i].getPath()) {
          int x = (int) p;
          int y = (int) (p >> 32);
          
          for (int[] c : circle) {
            int xx = x + c[0];
            int yy = y + c[1];
            if (xx >= 0 && yy >= 0 && xx <= sx && yy <= sy) {
              int j = array[yy][xx];
              if (j != -1) {
                if (j != i) {
                  ++g;
                  if (cc.add(new Pair<Integer, Integer>(Math.min(i, j), Math.max(i, j))))
                    System.err.println(x + ", " + y);
                }
              } else
                array[yy][xx] = i;
            }
          }
        }
      }
    
    System.err.println(g);
    System.err.println(cc.size());
    System.exit(-1);
    
    TreeMap<Integer, TreeSet<Integer>> from = new TreeMap<Integer, TreeSet<Integer>>();
    TreeMap<Integer, TreeSet<Integer>> to = new TreeMap<Integer, TreeSet<Integer>>();
    
    for (int i = 0; i != n; ++i) {
      TreeSet<Integer> s = from.get(boxes[i][1]);
      if (s == null) {
        s = new TreeSet<Integer>();
        from.put(boxes[i][1], s);
      }
      
      s.add(i);
      
      s = from.get(boxes[i][3] + d);
      if (s == null) {
        s = new TreeSet<Integer>();
        from.put(boxes[i][3] + d, s);
      }
      
      s.add(i);
    }
    
    TreeMap<Integer, TreeSet<Integer>> boxCollisions = new TreeMap<Integer, TreeSet<Integer>>();
    
    TreeSet<Integer> set = new TreeSet<Integer>();
    for (int y = 0; y != sy; ++y) {
      System.err.println(y);
      
      TreeSet<Integer> s = from.get(y);
      if (s != null)
        set.addAll(s);
      
      TreeSet<Integer> t = to.get(y);
      if (t != null)
        set.removeAll(t);
      
      if (s != null)
        for (int a : s)
          for (int b : set)
            if (a != b) {
              if (boxes[a][0] < boxes[b][2] + d && boxes[b][0] < boxes[a][2] + d) {
                TreeSet<Integer> u = boxCollisions.get(a);
                if (u == null) {
                  u = new TreeSet<Integer>();
                  boxCollisions.put(a, u);
                }
                
                u.add(b);
              }
            }
    }
    
    int kk = 0;
    for (TreeSet<Integer> s : boxCollisions.values())
      kk += s.size();
    System.err.println(kk);
    
    int c = 0;
    long f = 0;
    
    for (Entry<Integer, TreeSet<Integer>> entry : boxCollisions.entrySet()) {
      int a = entry.getKey();
      
      ArrayList<Long>[][] field = new ArrayList[sy / d + 1][sx / 2 + 1];
      
      for (long p : segments[a].getPath()) {
        int x = ((int) p) / 2 / d;
        int y = ((int) (p >> 32)) / 2 / d;
        for (int yy = y == 0 ? 0 : y - 1; yy <= y + 1 && yy < field.length; ++yy)
          for (int xx = x == 0 ? 0 : x - 1; xx <= x + 1 && xx < field[0].length; ++xx) {
            if (field[yy][xx] == null)
              field[yy][xx] = new ArrayList<Long>();
            field[yy][xx].add(p);
          }
      }
      
      for (int b : entry.getValue()) {
        System.err.println(c++);
        
        TreeMap<Integer, ArrayList<Long>> collisions = new TreeMap<Integer, ArrayList<Long>>();
        
        for (long p : segments[b].getPath()) {
          int x = ((int) p) / 2 / d;
          int y = ((int) (p >> 32)) / 2 / d;
          if (field[y][x] != null) {
            int x0 = (int) p;
            int y0 = (int) (p >> 32);
            
            for (long p1 : field[y][x]) {
              int x1 = (int) p1;
              int y1 = (int) (p1 >> 32);
              
              int dx = x0 - x1;
              int dy = y0 - y1;
              
              int dist = dx * dx + dy * dy;
              if (dist < 4 * d * d) {
                ArrayList<Long> list = collisions.get(dist);
                if (list == null) {
                  list = new ArrayList<Long>();
                  collisions.put(dist, list);
                  ++f;
                }
                
                list.add(p);
                list.add(p1);
              }
            }
          }
        }
      }
    }
    
    System.err.println(f);
  }
}
