package segment;

import gaussian.Function;
import gaussian.GaussianIntegral;
import gaussian.Utils;
import general.collections.DefaultComparator;
import general.collections.Pair;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import map.Map;
import map.properties.ImageSize;
import map.properties.StitchStackProperties;

import cache.Cache;
import data.Tools;

import operations.image.ImageOpsDouble;

// TODO: change island representation from TreeSet<Pair> to boundary based form
public class SharpnessEvaluator {
  
  public static final double DEFAULT_SHARPNESS_FILTER_SIGMA = 48;
  public static final double DEFAULT_SHARPNESS_MASK_SHRINK_GROW_DISTANCE = 128;
  public static final double DEFAULT_EDGE_SMOOTH_FILTER_SIGMA = 8;
  
  private static final int[][] NEIGHBOURS_4 = new int[][] { {0, -1}, {-1, 0}, {1, 0}, {0, 1}};
  public static final int RENDER_COLOR_FACTOR = 0x133377;
  
  public static double[][] getSharpness4x(double[][] image, int channel) {
    image = ImageOpsDouble.log(image);
    
    int sx = 2 * image[0].length;
    int sy = 2 * image.length;
    
    int dx = channel % 2;
    int dy = channel / 2;
    
    double[][] result = new double[sy][sx];
    
    for (int y = 0; y != sy / 2; ++y)
      for (int x = 0; x != sx / 2; ++x) {
        if (x != sx / 2 - 1)
          result[2 * y + dy + 0][2 * x + dx + 1] = 2 * Math.abs(image[y][x + 1] - image[y][x]);
        
        if (y != sy / 2 - 1)
          result[2 * y + dy + 1][2 * x + dx + 0] = 2 * Math.abs(image[y + 1][x] - image[y][x]);
      }
    
    return result;
  }
  
  public static double[][] getSharpness2x(double[][] image0, double[][] image1, int parity) {
    image0 = ImageOpsDouble.log(image0);
    image1 = ImageOpsDouble.log(image1);
    
    int sx = 2 * image0[0].length;
    int sy = 2 * image0.length;
    
    double[][] result = new double[sy][sx];
    
    for (int y = 0; y != sy - 1; ++y)
      for (int x = 0; x != sx - 1; ++x)
        if ((x + y) % 2 != parity) {
          double val =
              Math.abs((y % 2 == 0 ? image0 : image1)[y / 2][(x + 1) / 2]
                  - (y % 2 == 0 ? image1 : image0)[(y + 1) / 2][x / 2]);
          
          result[y + 0][x + 1] += val;
          result[y + 1][x + 0] += val;
        }
    
    return result;
  }
  
  public static double[][] getSharpness1x(double[][] image) {
    image = ImageOpsDouble.log(image);
    
    int sx = image[0].length;
    int sy = image.length;
    
    double[][] result = new double[sy][sx];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if (x != sx - 1) {
          double val = Math.abs(image[y][x + 1] - image[y][x]);
          result[y][x] += val / 4;
          result[y][x + 1] += val / 4;
        }
        
        if (y != sy - 1) {
          double val = Math.abs(image[y + 1][x] - image[y][x]);
          result[y][x] += val / 4;
          result[y + 1][x] += val / 4;
        }
      }
    
    return result;
  }
  
  public static double[][] getSharpnessRGGB(double[][][] components, int color) {
    int channel = color + color / 2;
    
    if (color == 0 || color == 2)
      return getSharpness4x(components[channel], channel);
    
    return getSharpness2x(components[1], components[2], 1);
  }
  
  public static int getSharpnessBoundary(double sigma) throws IOException {
    Integer result = Cache.cache("sharpness-boundary/%f", sigma);
    if (result != null)
      return result;
    
    Function f = new GaussianIntegral(sigma);
    int s = (int) (40 * sigma) + 1;
    
    int[] boundary = new int[1];
    Utils.convolve(new double[s][s], 1, f, f, boundary);
    return boundary[0];
  }
  
  public static double[][] blurSharpness(double[][] sharpness, double sigma) throws IOException {
    int sx = sharpness[0].length;
    int sy = sharpness.length;
    
    Function f = new GaussianIntegral(sigma);
    int k = getSharpnessBoundary(sigma);
    
    sharpness = ImageOpsDouble.section(sharpness, -k, -k, sx + k, sy + k);
    sharpness = Utils.convolve(sharpness, 1, f, f, new int[] {k});
    sharpness = ImageOpsDouble.section(sharpness, k, k, sx + k, sy + k);
    
    return sharpness;
  }
  
  public static double[][][] computeSharpnessRGGB(double[][][][] imageComponents, int color,
      double sigma) throws IOException {
    double[][][] result = new double[imageComponents.length][][];
    for (int image = 0; image != result.length; ++image)
      result[image] = blurSharpness(getSharpnessRGGB(imageComponents[image], color), sigma);
    
    return result;
  }
  
  public static double[][] getSharpness(int stitch, int image, int color, double sigma)
      throws IOException {
    double[][] result = Cache.cache("sharpness-2/%d/%d/%d/%f", stitch, image, color, sigma);
    if (result != null)
      return result;
    
    System.err.println("building sharpness mask for stitch " + stitch + ", image " + image
        + ", color " + color + "...");
    
    //result = blurSharpness(getSharpnessRGGB(components, color), sigma);
    
    System.err.println("sharpness mask built");
    
    return result;
  }
  
  public static double[][][] getSharpnessArray(int stitch, int numImages, int color, double sigma)
      throws IOException {
    System.err.println("building sharpness array...");
    
    double[][][] sharpness = new double[numImages][][];
    for (int image = 0; image != numImages; ++image) {
      System.err.println("image " + image);
      sharpness[image] = getSharpness(stitch, image, color, sigma);
    }
    
    System.err.println("sharpness array built");
    
    return sharpness;
  }
  
  public static Double getSharpnessPoint(int x, int y, double[][] sharpness, ImageSize size,
      Map map, int boundary) {
    double[] in = new double[] {x + 0.5, y + 0.5};
    map.unmap(in, in);
    
    x = (int) Math.round(in[0] - 0.5);
    y = (int) Math.round(in[1] - 0.5);
    
    return x >= boundary && y >= boundary && x < size.getSx() - boundary
        && y < size.getSy() - boundary ? sharpness[y][x] : null;
  }
  
  public static int[][] getSharpnessMask(int[] size, double[][][] sharpness, ImageSize imageSize,
      Map[] maps, int boundary) {
    System.err.println("building sharpness mask...");
    
    int numImages = maps.length;
    
    int[][] boundaries = new int[numImages][4];
    for (int image = 0; image != numImages; ++image) {
      double[] b = map.Utils.getBoundary(imageSize, maps[image]);
      for (int i = 0; i != 4; ++i)
        boundaries[image][i] = (int) Math.ceil(b[i] - 0.5);
    }
    
    int[][] result = new int[size[1]][size[0]];
    
    for (int y = 0; y != size[1]; ++y) {
      if (y % 1000 == 0)
        System.err.println(y);
      
      for (int x = 0; x != size[0]; ++x) {
        double maxSharpness = Double.NEGATIVE_INFINITY;
        int maxImage = -1;
        
        for (int image = 0; image != numImages; ++image) {
          if (x >= boundaries[image][0] && y >= boundaries[image][1] && x <= boundaries[image][2]
              && y <= boundaries[image][3]) {
            Double sharp =
                getSharpnessPoint(x, y, sharpness[image], imageSize, maps[image], boundary);
            if (sharp != null && sharp > maxSharpness) {
              maxSharpness = sharp;
              maxImage = image;
            }
          }
        }
        
        result[y][x] = maxImage;
      }
    }
    
    System.err.println("sharpness mask built");
    
    return result;
  }
  
  public static int[] getDiskPoints(double distance) {
    int k = 2 * (int) Math.ceil(distance);
    
    ArrayList<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer, Integer>>();
    for (int y = -k; y <= k; ++y)
      for (int x = -k; x <= k; ++x)
        if ((x + y & 1) == 1)
          //if (x * x + y * y < 4 * distance * distance)
          points.add(new Pair<Integer, Integer>(x, y));
    
    @SuppressWarnings("unchecked")
    Pair<Integer, Integer>[] array = points.toArray(new Pair[] {});
    Arrays.sort(array, new Comparator<Pair<Integer, Integer>>() {
      public int compare(Pair<Integer, Integer> p0, Pair<Integer, Integer> p1) {
        //int d0 = p0.getA() * p0.getA() + p0.getB() + p0.getB();
        //int d1 = p1.getA() * p1.getA() + p1.getB() + p1.getB();
        int d0 = Math.max(p0.getA(), p0.getB());
        int d1 = Math.max(p1.getA(), p1.getB());
        if (d0 != d1)
          return d0 - d1;
        
        return p0.compareTo(p1);
      }
    });
    
    int[] result = new int[2 * array.length];
    for (int i = 0; i != array.length; ++i) {
      result[2 * i + 0] = array[i].getA();
      result[2 * i + 1] = array[i].getB();
    }
    
    return result;
  }
  
  public static void shrinkOrGrow(int[][] mask, double distance, boolean shrink) {
    System.err.println(shrink ? "shrinking..." : "growing...");
    
    int sx = mask[0].length;
    int sy = mask.length;
    
    @SuppressWarnings("unchecked")
    ArrayList<Integer>[] lists = new ArrayList[2];
    for (int i = 0; i != 2; ++i)
      lists[i] = new ArrayList<Integer>();
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int v = mask[y][x];
        
        if (x != sx - 1 && mask[y][x + 1] != v) {
          lists[0].add(x + 1);
          lists[0].add(y);
          lists[0].add(shrink ? -1 : v == -1 ? mask[y][x + 1] : v);
        }
        
        if (y != sy - 1 && mask[y + 1][x] != v) {
          lists[1].add(x);
          lists[1].add(y + 1);
          lists[1].add(shrink ? -1 : v == -1 ? mask[y + 1][x] : v);
        }
      }
    
    int[] disk = getDiskPoints(distance);
    
    System.err.println("list lengths: " + lists[0].size() / 2 + ", " + lists[1].size() / 2);
    System.err.println("disk size: " + disk.length / 2);
    
    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
    }
    
    for (int i = 0; i != disk.length;) {
      if (i / 2 % 1000 == 0)
        System.err.println(i / 2);
      
      int x = disk[i++];
      int y = disk[i++];
      
      int k = y & 1;
      
      x >>= 1;
      y >>= 1;
      
      for (int j = 0; j != lists[k].size();) {
        int xx = x + lists[k].get(j++);
        int yy = y + lists[k].get(j++);
        int v = lists[k].get(j++);
        
        if (xx >= 0 && yy >= 0 && xx < sx && yy < sy)
          mask[yy][xx] = v;
      }
    }
    
    System.err.println(shrink ? "shrinked" : "grown");
  }
  
  public static void fillVacancies(int[][] mask, double[][][] sharpness, ImageSize size,
      Map[] maps, int boundary) {
    System.err.println("filling vacancies...");
    
    int sx = mask[0].length;
    int sy = mask.length;
    
    TreeSet<Pair<Integer, Integer>> queue = new TreeSet<Pair<Integer, Integer>>();
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if (mask[y][x] == -1) {
          if ((x != 0 && mask[y][x - 1] != -1) || (y != 0 && mask[y - 1][x] != -1)
              || (x != sx - 1 && mask[y][x + 1] != -1) || (y != sy - 1 && mask[y + 1][x] != -1))
            queue.add(new Pair<Integer, Integer>(x, y));
        }
      }
    
    System.err.println("queue initialized");
    
    for (int c = 0;; ++c) {
      System.err.println(c + ": " + queue.size());
      
      TreeMap<Pair<Integer, Integer>, Integer> fill =
          new TreeMap<Pair<Integer, Integer>, Integer>();
      
      outer: for (Pair<Integer, Integer> p : queue) {
        int x = p.getA();
        int y = p.getB();
        
        TreeSet<Integer> neighbourSet = new TreeSet<Integer>();
        
        for (int[] neighbour : NEIGHBOURS_4) {
          int xx = x + neighbour[0];
          int yy = y + neighbour[1];
          
          if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
            int v = mask[yy][xx];
            if (v != -1 && !neighbourSet.add(v)
                && getSharpnessPoint(x, y, sharpness[v], size, maps[v], boundary) != null) {
              fill.put(p, v);
              continue outer;
            }
          }
        }
      }
      
      for (Entry<Pair<Integer, Integer>, Integer> entry : fill.entrySet())
        mask[entry.getKey().getB()][entry.getKey().getA()] = entry.getValue();
      
      for (Pair<Integer, Integer> p : fill.keySet()) {
        int x = p.getA();
        int y = p.getB();
        
        for (int[] neighbour : NEIGHBOURS_4) {
          int xx = x + neighbour[0];
          int yy = y + neighbour[1];
          
          if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
            Pair<Integer, Integer> q = new Pair<Integer, Integer>(xx, yy);
            if (mask[yy][xx] == -1)
              queue.add(q);
          }
        }
      }
      
      if (!queue.removeAll(fill.keySet()))
        break;
    }
    
    System.err.println("vacancies filled");
  }
  
  public static TreeMap<int[], Boolean> getIslands(int[][] mask, ImageSize size, Map[] maps,
      int boundary) {
    System.err.println("getting islands...");
    
    int sx = mask[0].length;
    int sy = mask.length;
    
    TreeMap<int[], Boolean> result =
        new TreeMap<int[], Boolean>(DefaultComparator.getComparator(int[].class));
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if (mask[y][x] == -1) {
          System.err.println("building island " + result.size());
          
          ArrayList<Integer> component = new ArrayList<Integer>();
          boolean border = false;
          
          ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
          
          mask[y][x] = -2;
          queue.add(x);
          queue.add(y);
          
          while (!queue.isEmpty()) {
            int xx = queue.poll();
            int yy = queue.poll();
            
            component.add(xx);
            component.add(yy);
            
            if (Math.random() < 0.001)
              System.err.println(component.size());
            
            for (int[] neighbour : NEIGHBOURS_4) {
              int xxx = xx + neighbour[0];
              int yyy = yy + neighbour[1];
              
              if (xxx >= 0 && yyy >= 0 && xxx < sx && yyy < sy) {
                if (mask[yyy][xxx] == -1) {
                  mask[yyy][xxx] = -2;
                  queue.add(xxx);
                  queue.add(yyy);
                }
              } else
                border = true;
            }
          }
          
          System.err.println("built island with " + component.size() + " pixels");
          
          int[] array = new int[component.size()];
          for (int i = 0; i != array.length; ++i)
            array[i] = component.get(i);
          
          result.put(array, border);
        }
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if (mask[y][x] == -2)
          mask[y][x] = -1;
    
    System.err.println("found " + result.size() + " islands");
    
    return result;
  }
  
  public static void fillIsland(int[][] mask, int[] island, boolean border, double[][][] sharpness,
      ImageSize size, Map[] maps, int boundary) {
    System.err.println("filling island...");
    
    TreeSet<Pair<Double, Integer>> images = new TreeSet<Pair<Double, Integer>>();
    
    for (int image = 0; image != sharpness.length; ++image) {
      double sharp = 0;
      for (int j = 0; j != island.length;) {
        int x = island[j++];
        int y = island[j++];
        
        Double value = getSharpnessPoint(x, y, sharpness[image], size, maps[image], boundary);
        if (value != null)
          sharp += value;
      }
      
      images.add(new Pair<Double, Integer>(sharp, image));
    }
    
    TreeSet<Pair<Double, Integer>> fillers = new TreeSet<Pair<Double, Integer>>();
    
    outer: for (int j = 0; j != island.length;) {
      int x = island[j++];
      int y = island[j++];
      
      for (Pair<Double, Integer> entry : images.descendingSet()) {
        int image = entry.getB();
        if (getSharpnessPoint(x, y, sharpness[image], size, maps[image], boundary) != null) {
          fillers.add(entry);
          continue outer;
        }
      }
      
      if (border)
        return;
      
      throw new RuntimeException("island cannot be filled");
    }
    
    outer: for (int j = 0; j != island.length;) {
      int x = island[j++];
      int y = island[j++];
      
      for (Pair<Double, Integer> entry : fillers.descendingSet()) {
        int image = entry.getB();
        if (getSharpnessPoint(x, y, sharpness[image], size, maps[image], boundary) != null) {
          mask[y][x] = image;
          continue outer;
        }
      }
    }
    
    System.err.println("island filled");
  }
  
  public static TreeMap<Pair<Integer, Integer>, Segment<Pair<Integer, Integer>>[]>
      computeComponentBoundaries(int[][] mask) {
    System.err.println("computing component boundaries...");
    
    TreeMap<Integer, TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>>> boundaries =
        new TreeMap<Integer, TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>>>();
    
    int sx = mask[0].length;
    int sy = mask.length;
    
    int[] v = new int[2];
    
    for (int x = 0; x <= sx; ++x)
      for (int y = 0; y <= sy; ++y) {
        if (x != 0 && y != 0 && x != sx && y != sy)
          if (mask[y - 1][x - 1] == mask[y][x] && mask[y - 1][x] == mask[y][x - 1]
              && mask[y][x] != mask[y][x - 1])
            throw new RuntimeException("evil corner detected");
        
        for (int i = 0; i != 2; ++i)
          if (i == 0 ? x != sx : y != sy) {
            int xx = x - i;
            int yy = y - (1 - i);
            
            v[0] = x != sx && y != sy ? mask[y][x] : -1;
            v[1] = xx != -1 && yy != -1 ? mask[yy][xx] : -1;
            
            if (v[0] != v[1]) {
              boolean right = (v[0] < v[1]) == (i == 0);
              
              if (!(v[0] < v[1])) {
                int tmp = v[0];
                v[0] = v[1];
                v[1] = tmp;
              }
              
              Pair<Integer, Integer> a = new Pair<Integer, Integer>(x, y);
              Pair<Integer, Integer> b = new Pair<Integer, Integer>(xx + 1, yy + 1);
              
              @SuppressWarnings("unchecked")
              Segment<Pair<Integer, Integer>> s =
                  new Segment<Pair<Integer, Integer>>(
                      (Pair<Integer, Integer>[]) new Pair<?, ?>[] {}, right ? a : b, right ? b : a);
              
              TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>> map =
                  boundaries.get(v[0]);
              if (map == null) {
                map = new TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>>();
                boundaries.put(v[0], map);
              }
              
              ArrayList<Segment<Pair<Integer, Integer>>> set = map.get(v[1]);
              if (set == null) {
                set = new ArrayList<Segment<Pair<Integer, Integer>>>();
                map.put(v[1], set);
              }
              
              set.add(s);
            }
          }
      }
    
    TreeMap<Pair<Integer, Integer>, Segment<Pair<Integer, Integer>>[]> result =
        new TreeMap<Pair<Integer, Integer>, Segment<Pair<Integer, Integer>>[]>();
    
    System.err.println("segmenting...");
    
    for (Entry<Integer, TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>>> a : boundaries
        .entrySet()) {
      TreeMap<Integer, ArrayList<Segment<Pair<Integer, Integer>>>> m = a.getValue();
      for (Entry<Integer, ArrayList<Segment<Pair<Integer, Integer>>>> b : m.entrySet()) {
        @SuppressWarnings("unchecked")
        Segment<Pair<Integer, Integer>>[] segments =
            new Segments<Pair<Integer, Integer>>((Pair<Integer, Integer>[]) new Pair<?, ?>[] {},
                b.getValue()).get()
                .toArray((Segment<Pair<Integer, Integer>>[]) new Segment<?>[] {});
        
        result.put(new Pair<Integer, Integer>(a.getKey(), b.getKey()), segments);
      }
    }
    
    System.err.println("component boundaries computed");
    
    return result;
  }
  
  @SuppressWarnings("unchecked")
  public static Segment<Pair<Double, Double>> smoothEdge(Segment<Pair<Integer, Integer>> edge,
      double sigma) {
    final Function f = new GaussianIntegral(sigma);
    final int k = 2 * ((int) f.getWindowSize() + 1);
    
    List<Pair<Integer, Integer>> list = edge.getPath();
    
    double[][] values = new double[2][list.size() + 2 * k];
    for (int i = 0; i != values[0].length; ++i) {
      Pair<Integer, Integer> p = list.get(Math.max(0, Math.min(list.size() - 1, i - k)));
      values[0][i] = p.getA();
      values[1][i] = p.getB();
    }
    
    int[] boundary = new int[1];
    for (int i = 0; i != 2; ++i)
      values[i] = Utils.convolve(values[i], 1, f, boundary);
    
    final int h = boundary[0] / 2;
    
    Pair<Double, Double>[] newList = new Pair[list.size() + 2 * h];
    for (int i = 0; i != newList.length; ++i) {
      int j = i + k - h;
      newList[i] = new Pair<Double, Double>(values[0][j], values[1][j]);
    }
    
    return new Segment<Pair<Double, Double>>((Pair<Double, Double>[]) new Pair<?, ?>[] {},
        Arrays.asList(newList));
  }
  
  @SuppressWarnings("unchecked")
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] splitBoundaries(
      TreeMap<Pair<Integer, Integer>, Segment<Pair<Integer, Integer>>[]> boundaries, double sigma) {
    System.err.println("splitting boundaries...");
    
    ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> result =
        new ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>();
    
    for (Entry<Pair<Integer, Integer>, Segment<Pair<Integer, Integer>>[]> entry : boundaries
        .entrySet()) {
      Pair<Integer, Integer> images = entry.getKey();
      for (Segment<Pair<Integer, Integer>> edge : entry.getValue())
        for (Segment<Pair<Double, Double>> segment : smoothEdge(edge, sigma).split()) {
          boolean swap = !(segment.getStart().swap().compareTo(segment.getEnd().swap()) < 0);
          result.add(new Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>(swap ? segment
              .reverse() : segment, swap ? images.swap() : images));
        }
    }
    
    System.err.println("boundaries split");
    
    return result
        .toArray((Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]) new Pair<?, ?>[] {});
  }
  
  @SuppressWarnings("unchecked")
  public static TreeMap<Double, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]>[]
      buildSortedEdgeMaps(Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    System.err.println("building sorted edge maps...");
    
    TreeMap<Double, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>>[] temp =
        (TreeMap<Double, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>>[]) new TreeMap<?, ?>[2];
    for (int i = 0; i != 2; ++i)
      temp[i] =
          new TreeMap<Double, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>>();
    
    for (Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>> entry : edges) {
      Segment<Pair<Double, Double>> segment = entry.getA();
      
      for (int i = 0; i != 2; ++i) {
        ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> list =
            temp[i].get(segment.getEndpoint(i).getB());
        if (list == null) {
          list = new ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>();
          temp[i].put(segment.getEndpoint(i).getB(), list);
        }
        
        list.add(new Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>(segment, entry
            .getB()));
      }
    }
    
    final Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] empty =
        (Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]) new Pair<?, ?>[] {};
    
    TreeMap<Double, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]>[] result =
        (TreeMap<Double, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]>[]) new TreeMap<?, ?>[2];
    for (int i = 0; i != 2; ++i) {
      result[i] =
          new TreeMap<Double, Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[]>();
      for (Entry<Double, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>> entry : temp[i]
          .entrySet())
        result[i].put(entry.getKey(), entry.getValue().toArray(empty));
    }
    
    System.err.println("sorted edge maps built");
    
    return result;
  }
  
  public static TreeMap<Integer, Integer> computeLineColorMap(
      Collection<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> line, double y,
      int scale) {
    TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Integer>>> changesMap =
        new TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Integer>>>();
    
    for (Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>> edge : line) {
      Segment<Pair<Double, Double>> segment = edge.getA();
      Pair<Integer, Integer> images = edge.getB();
      
      Pair<Double, Double> s = segment.getStart();
      Pair<Double, Double> e = segment.getEnd();
      
      double sx = s.getA();
      double sy = s.getB();
      
      double ex = e.getA();
      double ey = e.getB();
      
      double a = (y - sy) * ex;
      double b = (ey - y) * sx;
      double c = (a + b) / (ey - sy);
      
      int x = (int) Math.ceil(scale * c - 0.5);
      
      TreeMap<Integer, TreeMap<Integer, Integer>> changes = changesMap.get(x);
      if (changes == null) {
        changes = new TreeMap<Integer, TreeMap<Integer, Integer>>();
        changesMap.put(x, changes);
      }
      
      TreeMap<Integer, Integer> change = changes.get(images.getA());
      if (change == null) {
        change = new TreeMap<Integer, Integer>();
        changes.put(images.getA(), change);
      }
      
      Integer count = change.get(images.getB());
      if (count == null)
        count = 0;
      
      change.put(images.getB(), count + 1);
    }
    
    TreeMap<Integer, Integer> result = new TreeMap<Integer, Integer>();
    
    int color = -1;
    result.put(Integer.MIN_VALUE, color);
    
    for (Entry<Integer, TreeMap<Integer, TreeMap<Integer, Integer>>> entry : changesMap.entrySet()) {
      TreeMap<Integer, TreeMap<Integer, Integer>> changes = entry.getValue();
      
      for (;;) {
        TreeMap<Integer, Integer> change = changes.get(color);
        if (change == null)
          break;
        
        Iterator<Entry<Integer, Integer>> it = change.entrySet().iterator();
        Entry<Integer, Integer> e = it.next();
        int newColor = e.getKey();
        
        int value = e.getValue() - 1;
        e.setValue(value);
        if (value == 0) {
          it.remove();
          if (change.isEmpty())
            changes.remove(color);
        }
        
        color = newColor;
      }
      
      result.put(entry.getKey(), color);
    }
    
    return result;
  }
  
  public static TreeMap<Integer, TreeMap<Integer, Integer>> computeLineColorMaps(int scale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    System.err.println("computing line color maps...");
    
    long sa = System.currentTimeMillis();
    
    TreeMap<Integer, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>> lines =
        new TreeMap<Integer, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>>();
    for (Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>> edge : edges) {
      Segment<Pair<Double, Double>> segment = edge.getA();
      
      int a = (int) Math.ceil(scale * segment.getStart().getB() - 0.5);
      int b = (int) Math.ceil(scale * segment.getEnd().getB() - 0.5);
      
      for (int i = a; i != b; ++i) {
        ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>> line = lines.get(i);
        if (line == null) {
          line = new ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>();
          lines.put(i, line);
        }
        
        line.add(edge);
      }
    }
    
    TreeMap<Integer, TreeMap<Integer, Integer>> result =
        new TreeMap<Integer, TreeMap<Integer, Integer>>();
    for (Entry<Integer, ArrayList<Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>>> entry : lines
        .entrySet())
      result.put(entry.getKey(),
          computeLineColorMap(entry.getValue(), (entry.getKey() + 0.5) / scale, scale));
    
    long sb = System.currentTimeMillis();
    
    System.err.println("line color maps computed: " + (sb - sa) + "ms");
    
    return result;
  }
  
  public static int computeBoundary(int sx, int sy, int scale, Map[] maps, ImageSize size,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    System.err.println("computing boundary...");
    
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors = computeLineColorMaps(scale, edges);
    double[] in = new double[2];
    
    int isx = size.getSx();
    int isy = size.getSy();
    
    int boundary = Integer.MAX_VALUE;
    
    for (int y = 0; y != scale * sy; ++y) {
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != scale * sx; ++x) {
        int v = line.floorEntry(x).getValue();
        if (v != -1) {
          in[0] = (x + 0.5) / scale;
          in[1] = (y + 0.5) / scale;
          
          maps[v].unmap(in, in);
          
          double d = Math.min(Math.min(in[0], in[1]), Math.min(isx - in[0], isy - in[1]));
          boundary = Math.min(boundary, (int) (scale * d));
        }
      }
    }
    
    System.err.println("boundary computed");
    
    return boundary;
  }
  
  public static BufferedImage toImage(int[][] mask) {
    BufferedImage image =
        new BufferedImage(mask[0].length, mask.length, BufferedImage.TYPE_BYTE_INDEXED);
    int[] colors = new int[129];
    for (int i = 0; i != colors.length; ++i)
      colors[i] = 0xff000000 | (RENDER_COLOR_FACTOR * i);
    
    for (int y = 0; y != mask.length; ++y)
      for (int x = 0; x != mask[0].length; ++x)
        image.setRGB(x, y, colors[mask[y][x] + 1]);
    
    return image;
  }
  
  public static BufferedImage render(int sx, int sy, int scale,
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges) {
    
    TreeMap<Integer, TreeMap<Integer, Integer>> lineColors = computeLineColorMaps(scale, edges);
    
    BufferedImage image =
        new BufferedImage(scale * sx, scale * sy, BufferedImage.TYPE_BYTE_INDEXED);
    int[] colors = new int[256];
    for (int i = 0; i != colors.length; ++i)
      colors[i] = 0xff000000 | (RENDER_COLOR_FACTOR * i);
    
    for (int y = 0; y != scale * sy; ++y) {
      TreeMap<Integer, Integer> line = lineColors.get(y);
      if (line == null) {
        line = new TreeMap<Integer, Integer>();
        line.put(-1, -1);
      }
      
      for (int x = 0; x != scale * sx; ++x)
        image.setRGB(x, y, colors[line.floorEntry(x).getValue() + 1]);
    }
    
    return image;
  }
  
  public static Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] computeEdges(
      StitchStackProperties stack, int stitch, int channel, double[][][] sharpness, int[] boundary,
      double shrinkGrowDistance, double edgeSmoothSigma, boolean interactive) {
    System.err.println("computing edges...");
    
    ImageSize imageSize = stack.getImageSetProperties(stitch).getSize();
    
    Map[] maps = map.Utils.getMapsFromStack(stack, stitch, channel, true);
    
    ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();
    
    int[][] mask =
        getSharpnessMask(new int[] {stack.getX1() - stack.getX0(), stack.getY1() - stack.getY0()},
            sharpness, imageSize, maps, boundary[0]);
    
    if (interactive)
      images.add(toImage(mask));
    
    final int k = 8;
    
    for (int i = 0; i != k; ++i)
      shrinkOrGrow(mask, shrinkGrowDistance / k, true);
    
    for (int i = 0; i != k; ++i)
      shrinkOrGrow(mask, shrinkGrowDistance / k, false);
    
    fillVacancies(mask, sharpness, imageSize, maps, boundary[0]);
    
    if (interactive)
      images.add(toImage(mask));
    
    for (Entry<int[], Boolean> island : getIslands(mask, imageSize, maps, boundary[0]).entrySet())
      fillIsland(mask, island.getKey(), island.getValue(), sharpness, imageSize, maps, boundary[0]);
    
    System.err.println("islands filled");
    
    if (interactive)
      images.add(toImage(mask));
    
    Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges =
        splitBoundaries(computeComponentBoundaries(mask), edgeSmoothSigma);
    
    if (interactive) {
      images.add(render(stack.getX1() - stack.getX0(), stack.getY1() - stack.getY0(), 1, edges));
      Tools.displayImages(images.toArray(new BufferedImage[] {}));
    }
    
    boundary[0] =
        computeBoundary(stack.getX1() - stack.getX0(), stack.getY1() - stack.getY0(), 1, maps,
            imageSize, edges);
    
    System.err.println("edges computed, real boundary: " + boundary[0]);
    
    return edges;
  }
  
}
