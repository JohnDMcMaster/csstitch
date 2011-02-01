package hm;

import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import cache.Cache;
import data.Tools;

import realmetal.BinaryImage;
import realmetal.BinaryImageTools;
import realmetal.Images;
import realmetal.PolyDirt;

public class AClass {
  
  private static final int[][] NEIGHBOURS_4 = new int[][] { {0, -1}, {-1, 0}, {1, 0}, {0, 1}};
  
  private static final int[][] NEIGHBOURS_8 = new int[][] { {-1, -1}, {0, -1}, {1, -1}, {-1, 0},
      {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
  
  public TreeSet<Pair<Integer, Integer>> getComponent(BinaryImage mask, int x, int y,
      boolean diagonal) {
    TreeSet<Pair<Integer, Integer>> result = new TreeSet<Pair<Integer, Integer>>();
    
    int sx = mask.getSx();
    int sy = mask.getSy();
    
    boolean v = mask.get(x, y);
    
    ArrayDeque<Pair<Integer, Integer>> queue = new ArrayDeque<Pair<Integer, Integer>>();
    queue.add(new Pair<Integer, Integer>(x, y));
    
    int[][] neighbours = diagonal ? NEIGHBOURS_8 : NEIGHBOURS_4;
    
    while (!queue.isEmpty()) {
      Pair<Integer, Integer> point = queue.pollFirst();
      
      if (result.add(point)) {
        int xx = point.getA();
        int yy = point.getB();
        
        for (int[] neighbour : neighbours) {
          int xxx = xx + neighbour[0];
          int yyy = yy + neighbour[1];
          
          if (xxx >= 0 && yyy >= 0 && xxx < sx && yyy < sy)
            if (mask.get(xxx, yyy) == v)
              queue.add(new Pair<Integer, Integer>(xxx, yyy));
        }
      }
    }
    
    return result;
  }
  
  public TreeSet<Pair<Integer, Integer>> getComponent(BinaryImage mask, int x, int y) {
    return getComponent(mask, x, y, mask.get(x, y));
  }
  
  public static class Component implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int parent;
    private boolean type;
    private int size;
    private int x0, y0, x1, y1;
    
    public Component(int parent, boolean type, int size, int x0, int y0, int x1, int y1) {
      this.parent = parent;
      this.type = type;
      this.size = size;
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
    }
    
    public int getParent() {
      return parent;
    }
    
    public boolean getType() {
      return type;
    }
    
    public int getSize() {
      return size;
    }
    
    public int getX0() {
      return x0;
    }
    
    public int getY0() {
      return y0;
    }
    
    public int getX1() {
      return x1;
    }
    
    public int getY1() {
      return y1;
    }
  }
  
  public static Component[] computeComponentTree(BinaryImage mask) {
    int sx = mask.getSx();
    int sy = mask.getSy();
    
    ArrayList<Component> components = new ArrayList<AClass.Component>();
    
    TreeMap<Long, Integer> borders = new TreeMap<Long, Integer>();
    
    BinaryImage check = new BinaryImage(sx, sy);
    
    for (int y = 0; y != sy; ++y) {
      System.err.println(y);
      
      for (int x = 0; x != sx; ++x) {
        if (!check.get(x, y)) {
          check.check(x, y);
          
          int parent = -1;
          boolean type = mask.get(x, y);
          int size = 0;
          
          int x0 = Integer.MAX_VALUE;
          int y0 = Integer.MAX_VALUE;
          
          int x1 = Integer.MIN_VALUE;
          int y1 = Integer.MIN_VALUE;
          
          int[][] neighbours = type ? NEIGHBOURS_8 : NEIGHBOURS_4;
          
          ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
          queue.add(x | (y << 16));
          
          while (!queue.isEmpty()) {
            int w = queue.poll();
            
            int xx = w & 0xffff;
            int yy = w >>> 16;
            
            ++size;
            
            if (size % 1000000 == 0)
              System.err.println(size + ": " + queue.size());
            
            x0 = Math.min(x0, xx);
            y0 = Math.min(y0, yy);
            
            x1 = Math.max(x1, xx);
            y1 = Math.max(y1, yy);
            
            for (int[] neighbour : neighbours) {
              int xxx = xx + neighbour[0];
              int yyy = yy + neighbour[1];
              
              if (xxx >= 0 && yyy >= 0 && xxx < sx && yyy < sy) {
                boolean m = mask.get(xxx, yyy);
                boolean c = check.get(xxx, yyy);
                
                if (m == type) {
                  if (!c) {
                    check.check(xxx, yyy);
                    queue.add(xxx | (yyy << 16));
                  }
                } else if (((xx + xxx) & (yy + yyy) & 1) == 0) {
                  long borderPoint = (xx + xxx) + ((long) (yy + yyy) << 32);
                  if (c)
                    parent = borders.remove(borderPoint);
                  else
                    borders.put(borderPoint, components.size());
                }
              }
            }
          }
          
          components.add(new Component(parent, type, size, x0, y0, x1 + 1, y1 + 1));
        }
      }
    }
    
    return components.toArray(new Component[] {});
  }
  
  public static Component[] getComponentTree(double sigma, int scale, double threshold)
      throws IOException {
    Component[] result = Cache.cache("component-tree/%f/%d/%f", sigma, scale, threshold);
    if (result != null)
      return result;
    
    return computeComponentTree(BClass.computeLogLvLvLvvAdjustedStitch(0, 1, sigma, scale, threshold));
  }
  
  public static void main(String[] args) throws IOException {
    BinaryImage mask = BClass.computeLogLvLvLvvAdjustedStitch(0, 1, 1, 2, 0.000002);
    Tools.displayImages(mask.toImage());
  }
}
