package metal2;

import general.Streams;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;

import operations.image.ImageOpsDouble;

import metal.MetalTools;

import distributed.Bootstrap;
import distributed.server.Servers;

public class Segmenter {
  
  public static interface PointCallback {
    public boolean isEnabled(int x, int y);
  }
  
  public static final PointCallback GREEN_ENABLER = new PointCallback() {
    public boolean isEnabled(int x, int y) {
      return (x + y) % 2 == 1;
    }
  };
  
  public static final PointCallback ENABLER = new PointCallback() {
    public boolean isEnabled(int x, int y) {
      return true;
    }
  };
  
  public static int[][] getGreenNeighbours4() {
    return new int[][] { {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
  }
  
  public static int[][] getGreenNeighbours8() {
    return new int[][] { {-2, 0}, {-1, -1}, {-1, 1}, {0, -2}, {0, 2}, {1, -1}, {1, 1}, {2, 0}};
  }
  
  public static int[][] getNeighbours4() {
    return new int[][] { {-1, 0}, {0, -1}, {0, 1}, {1, 0}};
  }
  
  public static int[][] getNeighbours8() {
    return new int[][] { {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
  }
  
  public static class ComponentInfo implements Comparable<ComponentInfo>, Serializable {
    private static final long serialVersionUID = 1L;
    
    public ArrayList<short[]> component = new ArrayList<short[]>();
    public ComponentInfo[] sub;
    public double threshold;
    public ComponentInfo parent;
    public int size;
    
    public int compareTo(ComponentInfo info) {
      short[] a = component.get(0);
      short[] b = info.component.get(0);
      
      int d = a[0] - b[0];
      if (d != 0)
        return d;
      
      return a[1] - b[1];
    }
  }
  
  private static class Point implements Comparable<Point> {
    double value;
    short[] coords;
    
    Point(double value, int x, int y) {
      this.value = value;
      coords = new short[] {(short) x, (short) y};
    }
    
    public int compareTo(Point point) {
      int d = (int) Math.signum(value - point.value);
      if (d != 0)
        return d;
      
      d = coords[0] - point.coords[0];
      if (d != 0)
        return d;
      
      return coords[1] - point.coords[1];
    }
  }
  
  private static ComponentInfo segmentCore(int sx, int sy, NavigableSet<Point> sorted,
      int[][] neighbours) {
    ComponentInfo[][] array = new ComponentInfo[sy][sx];
    
    int counter = 0;
    
    while (!sorted.isEmpty()) {
      if (counter % 1000 == 0)
        System.err.println(counter);
      ++counter;
      
      Point entry = sorted.pollFirst();
      
      int x = entry.coords[0];
      int y = entry.coords[1];
      
      TreeSet<ComponentInfo> neighbouringComponents = new TreeSet<ComponentInfo>();
      for (int[] v : neighbours) {
        int xx = x + v[0];
        int yy = y + v[1];
        if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
          ComponentInfo c = array[yy][xx];
          if (c != null) {
            ComponentInfo parent = c.parent;
            if (parent != null) {
              do {
                c = parent;
                parent = c.parent;
              } while (parent != null);
              array[yy][xx] = c;
            }
            
            neighbouringComponents.add(c);
          }
        }
      }
      
      ComponentInfo info;
      if (neighbouringComponents.size() == 1)
        info = neighbouringComponents.first();
      else {
        info = new ComponentInfo();
        info.threshold = entry.value;
        
        int n = neighbouringComponents.size();
        info.sub = new ComponentInfo[n];
        
        if (n != 0) {
          int i = 0;
          for (ComponentInfo subInfo : neighbouringComponents) {
            subInfo.component.trimToSize();
            subInfo.parent = info;
            
            info.sub[i++] = subInfo;
            info.size += subInfo.size;
          }
          
          /*
          int sizeThreshold = Math.max(3 * info.size / 4, 100);
          for (ComponentInfo subInfo : neighbouringComponents)
            if (subInfo.size >= sizeThreshold) {
              ArrayList<short[]> component = info.component;
              info.component = subInfo.component;
              subInfo.component = component;
              
              ComponentInfo[] sub = info.sub;
              info.sub = subInfo.sub;
              subInfo.sub = sub;
              
              double threshold = info.threshold;
              info.threshold = subInfo.threshold;
              subInfo.threshold = threshold;
              
              int size = info.size;
              info.size = subInfo.size;
              subInfo.size = size;
              
              info.parent = subInfo;
              subInfo.parent = null;
              break;
            }*/
        }
      }
      
      info.component.add(entry.coords);
      array[y][x] = info;
      ++info.size;
    }
    
    TreeSet<ComponentInfo> topLevel = new TreeSet<ComponentInfo>();
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        ComponentInfo info = array[y][x];
        if (info != null && info.parent == null)
          topLevel.add(info);
      }
    
    if (topLevel.size() != 1)
      throw new RuntimeException("pixel graph is not connected");
    
    return topLevel.first();
  }
  
  public static ComponentInfo segment(double[][] image, PointCallback enabler, int[][] neighbours) {
    int sx = image[0].length;
    int sy = image.length;
    
    TreeSet<Point> sorted = new TreeSet<Point>();
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if (enabler.isEnabled(x, y))
          sorted.add(new Point(image[y][x], x, y));
    
    return segmentCore(sx, sy, sorted.descendingSet(), neighbours);
  }
  
  public static ComponentInfo segmentGreen4(double[][] image) {
    return segment(image, GREEN_ENABLER, getGreenNeighbours4());
  }
  
  public static ComponentInfo segmentGreen8(double[][] image) {
    return segment(image, GREEN_ENABLER, getGreenNeighbours8());
  }
  
  public static ComponentInfo segment4(double[][] image) {
    return segment(image, ENABLER, getNeighbours4());
  }
  
  public static ComponentInfo segment8(double[][] image) {
    return segment(image, ENABLER, getNeighbours8());
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    
    ComponentInfo info =
        segmentGreen4(ImageOpsDouble.fromFloat(MetalTools.getImageValues(0, 23)));
    Streams.writeObject("segment-result-4", info);
  }
  
}
