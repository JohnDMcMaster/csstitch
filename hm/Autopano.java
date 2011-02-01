package hm;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import map.AffineTransform;
import map.Utils;
import map.properties.ImageSetProperties;
import map.properties.ImageSize;
import map.properties.StitchStackProperties;

import operations.image.ImageOpsDouble;

import data.Tools;

import realmetal.Images;
import segment.Segment;
import segment.Sharpness;
import segment.Sharpness.Interpolator;
import segment.SharpnessEvaluator;
import segment.Sharpness.ContinuousImage;
import test.Tagger;
import general.collections.Pair;
import general.execution.Command;

public class Autopano {
  
  public static final String OTHER_DIR = "/home/noname/di/other";
  
  private static ArrayList<Long> times = new ArrayList<Long>();
  
  public static void addTime(long x) {
    synchronized (times) {
      times.add(x);
      
      long sum = 0;
      for (Long y : times)
        sum += y;
      
      sum /= times.size();
      System.err.println(sum);
    }
  }
  
  public static boolean match(String line, String regex) {
    return line != null && line.matches(regex);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames(String identifier,
      String description, String scan) throws IOException {
    TreeMap<Pair<Integer, Integer>, String> names = new TreeMap<Pair<Integer, Integer>, String>();
    
    Pattern pattern = Pattern.compile("step \\(([0-9]+), ([0-9]+)\\)");
    
    TreeMap<String, String>[] tags = Tagger.load("/home/noname/di/tags.txt");
    for (TreeMap<String, String> map : tags) {
      if (!match(map.get("chip-identifier"), identifier))
        continue;
      
      if (!match(map.get("description-chip"), description))
        continue;
      
      if (!match(map.get("description-group"), scan))
        continue;
      
      Matcher matcher = pattern.matcher(map.get("description"));
      if (!matcher.matches())
        continue;
      
      int a = Integer.parseInt(matcher.group(1));
      int b = Integer.parseInt(matcher.group(2));
      names.put(new Pair<Integer, Integer>(a, b), map.get("name"));
    }
    
    return names;
  }
  
  public static TreeSet<String> readDir(String dir) throws IOException {
    TreeSet<String> result = new TreeSet<String>(Arrays.asList(new File(dir).list()));
    result.remove(".");
    result.remove("..");
    return result;
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesAlternating(String dir,
      int[] widths, boolean down, boolean right, String... bad) throws IOException {
    TreeSet<String> fileSet = readDir(dir);
    fileSet.removeAll(Arrays.asList(bad));
    String[] files = fileSet.toArray(new String[] {});
    
    TreeMap<Pair<Integer, Integer>, String> result = new TreeMap<Pair<Integer, Integer>, String>();
    
    int image = 0;
    
    int row = 0;
    int column = 0;
    
    while (row != widths.length) {
      if (column == widths[row]) {
        column = 0;
        ++row;
      } else {
        int realColumn = row % 2 == 0 != right ? column : widths[row] - 1 - column;
        result.put(new Pair<Integer, Integer>(down ? widths.length - 1 - row : row, realColumn),
            files[image]);
        
        ++image;
        ++column;
      }
    }
    
    if (image != files.length)
      throw new RuntimeException();
    
    return result;
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesAlternating(String dir,
      int[] widths, boolean[] right, String... bad) throws IOException {
    TreeSet<String> fileSet = readDir(dir);
    fileSet.removeAll(Arrays.asList(bad));
    String[] files = fileSet.toArray(new String[] {});
    
    TreeMap<Pair<Integer, Integer>, String> result = new TreeMap<Pair<Integer, Integer>, String>();
    
    int image = 0;
    
    int row = 0;
    int column = 0;
    
    while (row != widths.length) {
      if (column == widths[row]) {
        column = 0;
        ++row;
      } else {
        int realColumn = !right[row] ? column : widths[row] - 1 - column;
        result.put(new Pair<Integer, Integer>(row, realColumn), files[image]);
        
        ++image;
        ++column;
      }
    }
    
    if (image != files.length)
      throw new RuntimeException();
    
    return result;
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesStandardScan(String dir,
      int width, int height, boolean down, boolean right, boolean mirror) throws IOException {
    TreeSet<String> fileSet = readDir(dir);
    String[] files = fileSet.toArray(new String[] {});
    
    TreeMap<Pair<Integer, Integer>, String> result = new TreeMap<Pair<Integer, Integer>, String>();
    
    if (mirror) {
      boolean bb = right;
      right = down;
      down = bb;
    }
    
    int image = 0;
    
    for (int y = 0; y != (mirror ? width : height); ++y)
      for (int x = 0; x != (mirror ? height : width); ++x) {
        int xx = x;
        int yy = y;
        
        if (mirror) {
          int tt = xx;
          xx = yy;
          yy = tt;
        }
        
        xx = right ? width - 1 - xx : xx;
        yy = down ? height - 1 - yy : yy;
        
        result.put(new Pair<Integer, Integer>(yy, xx), files[image]);
        
        ++image;
      }
    
    if (image != files.length)
      throw new RuntimeException();
    
    return result;
  }
  
  public static TreeMap<String, String> prepareNamesRing(String dir) throws IOException {
    TreeSet<String> fileSet = readDir(dir);
    String[] files = fileSet.toArray(new String[] {});
    
    TreeMap<String, String> result = new TreeMap<String, String>();
    for (String key : files)
      result.put(key, key);
    return result;
  }
  
  public static int getHeight(Set<Pair<Integer, Integer>> set) {
    int a = 0;
    while (set.contains(new Pair<Integer, Integer>(a, 0)))
      ++a;
    return a;
  }
  
  public static int getWidth(Set<Pair<Integer, Integer>> set, int a) {
    int b = 0;
    while (set.contains(new Pair<Integer, Integer>(a, b)))
      ++b;
    return b;
  }
  
  public static double findLevel(double[][] image, double quantil) {
    int sx = image[0].length;
    int sy = image.length;
    
    TreeSet<Pair<Double, Integer>> set = new TreeSet<Pair<Double, Integer>>();
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        set.add(new Pair<Double, Integer>(image[y][x], sx * y + x));
    
    int index = (int) (quantil * set.size());
    for (Pair<Double, Integer> entry : set)
      if (index-- == 0)
        return entry.getA();
    
    return ImageOpsDouble.max(image);
  }
  
  public static void prepareMonoImages(TreeMap<Pair<Integer, Integer>, String> names, String out,
      String tmp, String data, String mono, double quantil, double[][] light, int channel)
      throws IOException {
    for (Entry<Pair<Integer, Integer>, String> entry : names.entrySet()) {
      long start = System.currentTimeMillis();
      
      System.err.println(channel + ": " + entry.getKey());
      
      String name = entry.getValue();
      
      double[][] matrix =
          Tools.getMatrixFromImage(ImageIO
              .read(new File(data + "/" + name + "/" + channel + ".png")));
      for (int y = 0; y != matrix.length; ++y)
        for (int x = 0; x != matrix[y].length; ++x)
          matrix[y][x] = (matrix[y][x] + 0.5) / light[2 * y][2 * x];
      
      Images.fixDeadPixels(matrix, Images.getDeadPixelsInComponent(channel));
      
      double m = findLevel(matrix, quantil);
      System.err.println(m);
      
      for (int y = 0; y != matrix.length; ++y)
        for (int x = 0; x != matrix[y].length; ++x)
          matrix[y][x] = (matrix[y][x] / (m + 0.5)) * 65536;
      
      Tools.ensurePath(tmp);
      ImageIO.write(Tools.getGreyscaleImageFromMatrix(matrix, 65536), "png", new File(tmp));
      
      String outName = out + "/" + channel + "/" + name + ".png";
      Tools.ensurePath(outName);
      new Command(new String[] {mono, tmp, outName}).executeChecked();
      
      long end = System.currentTimeMillis();
      
      addTime(end - start);
    }
  }
  
  public static int[] getSize(String name) throws IOException {
    BufferedImage image = ImageIO.read(new File(name));
    return new int[] {image.getWidth(), image.getHeight()};
  }
  
  public static TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> findNeighbours(
      Set<Pair<Integer, Integer>> set, double maxDiff, boolean onlyDirect) {
    TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> result =
        new TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();
    
    int height = getHeight(set);
    
    for (int a = 0; a != height; ++a) {
      int width = getWidth(set, a);
      for (int b = 0; b != width - 1; ++b)
        result.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(
            new Pair<Integer, Integer>(a, b), new Pair<Integer, Integer>(a, b + 1)));
    }
    
    for (int a = 0; a != height - 1; ++a) {
      int w0 = getWidth(set, a);
      int w1 = getWidth(set, a + 1);
      
      for (int b0 = 0; b0 != w0; ++b0)
        for (int b1 = 0; b1 != w1; ++b1)
          if (!onlyDirect || b0 == b1)
            if (Math.abs((double) b0 / (w0 - 1) - (double) b1 / (w1 - 1)) < maxDiff
                / Math.sqrt((w0 - 1) * (w1 - 1)))
              result.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(
                  new Pair<Integer, Integer>(a, b0), new Pair<Integer, Integer>(a + 1, b1)));
    }
    
    return result;
  }
  
  public static TreeSet<Pair<String, String>>
      findNeighboursRing(Set<String> set, String... corners) {
    TreeSet<Pair<String, String>> result = new TreeSet<Pair<String, String>>();
    
    TreeSet<String> cornerSet = new TreeSet<String>();
    cornerSet.addAll(Arrays.asList(corners));
    
    for (String c : corners)
      if (!set.contains(c))
        throw new RuntimeException(c);
    
    String[] keys = set.toArray(new String[] {});
    for (int i = 0; i != keys.length; ++i) {
      int a = i;
      int b = (i + 1) % keys.length;
      int c = (i + 2) % keys.length;
      
      result.add(new Pair<String, String>(keys[a], keys[b]));
      if (cornerSet.contains(keys[b]))
        result.add(new Pair<String, String>(keys[a], keys[c]));
    }
    
    return result;
  }
  
  public static Command autopano(String out, String... in) {
    ArrayList<String> tokens = new ArrayList<String>();
    tokens.add("autopano-sift-c");
    tokens.add("--maxmatches");
    tokens.add("0");
    tokens.add("--maxdim");
    tokens.add("10000");
    /*
    tokens.add("--ransac");
    tokens.add("off");
    tokens.add("--refine");
    tokens.add("--keep-unrefinable");
    tokens.add("off");*/
    tokens.add(out);
    tokens.addAll(Arrays.asList(in));
    return new Command(tokens);
  }
  
  public static <A extends Comparable<A>> void findKeypoints(TreeMap<A, String> names,
      Pair<A, A> entry, String in, String out) throws IOException {
    long start = System.currentTimeMillis();
    
    System.err.println(entry);
    
    A a = entry.getA();
    A b = entry.getB();
    
    String inA = in + "/" + names.get(a);
    String inB = in + "/" + names.get(b);
    
    String outName = out + "/" + names.get(a) + "-" + names.get(b) + ".pto";
    Tools.ensurePath(outName);
    if (new File(outName).exists())
      return;
    
    autopano(outName, inA, inB).executeChecked();
    
    long end = System.currentTimeMillis();
    
    addTime(end - start);
  }
  
  public static <A extends Comparable<A>> void findKeypoints(TreeMap<A, String> names,
      Pair<A, A> entry, String in, String out, int dx, int dy) throws IOException {
    long start = System.currentTimeMillis();
    
    System.err.println(entry);
    
    A a = entry.getA();
    A b = entry.getB();
    
    String inA = in + "/" + names.get(a);
    String inB = in + "/" + names.get(b);
    
    String outName = out + "/" + names.get(a) + "-" + names.get(b) + ".pto";
    Tools.ensurePath(outName);
    if (new File(outName).exists())
      return;
    
    if (dx != 0 || dy != 0) {
      int[] dd = new int[2];
      dd[0] = ((Pair<Integer, Integer>) a).getB() - ((Pair<Integer, Integer>) b).getB();
      dd[1] = ((Pair<Integer, Integer>) a).getA() - ((Pair<Integer, Integer>) b).getA();
      
      if ((Math.abs(dd[0]) | Math.abs(dd[1])) != 1)
        throw new RuntimeException();
      
      BufferedImage[] images = new BufferedImage[2];
      images[0] = ImageIO.read(new File(inA));
      images[1] = ImageIO.read(new File(inB));
      
      int[][] bb = new int[2][4];
      for (int i = 0; i != 2; ++i) {
        bb[i][0] = 0;
        bb[i][1] = 0;
        bb[i][2] = images[i].getWidth();
        bb[i][3] = images[i].getHeight();
        
        int sign = i == 0 ? 1 : -1;
        
        if (sign * dd[0] == 1)
          bb[i][2] = bb[i][0] + dx;
        else if (sign * dd[0] == -1)
          bb[i][0] = bb[i][2] - dx;
        
        if (sign * dd[1] == 1)
          bb[i][3] = bb[i][1] + dy;
        else if (sign * dd[1] == -1)
          bb[i][1] = bb[i][3] - dy;
      }
      
      String[] rand = new String[3];
      for (int i = 0; i != rand.length; ++i)
        rand[i] = "/tmp/" + Math.random() + (i == 2 ? ".pto" : ".png");
      
      for (int i = 0; i != 2; ++i)
        ImageIO.write(
            images[i].getSubimage(bb[i][0], bb[i][1], bb[i][2] - bb[i][0], bb[i][3] - bb[i][1]),
            "png", new File(rand[i]));
      
      // HACK: autopano crashes sometimes (when it doesn't find control points on strangely sized images?)
      int r = autopano(rand[2], rand[0], rand[1]).execute();
      if (r == 0) {
        ArrayList<double[]> cp = StitchTools.readControlPoints(new File(rand[2]));
        PrintStream print = new PrintStream(outName);
        for (double[] p : cp) {
          for (int i = 0; i != 2; ++i) {
            p[0] += bb[i][0];
            p[1] += bb[i][1];
          }
          
          print.println("c n0 N1 x" + p[0] + " y" + p[1] + " X" + p[2] + " Y" + p[3]);
        }
        print.close();
      } else
        new PrintStream(outName).close();
    } else
      autopano(outName, inA, inB).executeChecked();
    
    long end = System.currentTimeMillis();
    
    addTime(end - start);
  }
  
  public static <A extends Comparable<A>> void findKeypointsMulti(final TreeMap<A, String> names,
      final Collection<Pair<A, A>> neighbours, final String in, final String out) {
    findKeypointsMulti(names, neighbours, in, out, 4);
  }
  
  public static <A extends Comparable<A>> void findKeypointsMulti(final TreeMap<A, String> names,
      final Collection<Pair<A, A>> neighbours, final String in, final String out, int numThreads) {
    ArrayList<Runnable> commands = new ArrayList<Runnable>();
    for (Pair<A, A> pair : neighbours) {
      final Pair<A, A> p = pair;
      commands.add(new Runnable() {
        public void run() {
          try {
            findKeypoints(names, p, in, out);
          } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
          }
        }
      });
    }
    
    executeMultithreaded(commands, numThreads);
  }
  
  public static <A extends Comparable<A>> void findKeypointsMulti(final TreeMap<A, String> names,
      final Collection<Pair<A, A>> neighbours, final String in, final String out, final int dx,
      final int dy, int numThreads) {
    ArrayList<Runnable> commands = new ArrayList<Runnable>();
    for (Pair<A, A> pair : neighbours) {
      final Pair<A, A> p = pair;
      commands.add(new Runnable() {
        public void run() {
          try {
            findKeypoints(names, p, in, out, dx, dy);
          } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
          }
        }
      });
    }
    
    executeMultithreaded(commands, numThreads);
  }
  
  public static <A> void printLookup(TreeMap<A, String> names, String filename) throws IOException {
    TreeMap<Integer, A> indexLookup = StitchTools.getIndexLookup(names.keySet());
    
    PrintStream out = new PrintStream(filename);
    for (int index : indexLookup.keySet())
      out.println("(" + index + ", " + names.get(indexLookup.get(index)) + ")");
    out.close();
  }
  
  public static void executeMultithreaded(Collection<Runnable> commands) {
    executeMultithreaded(commands, 4);
  }
  
  public static void executeMultithreaded(Collection<Runnable> commands, int numThreads) {
    ExecutorService pool = Executors.newFixedThreadPool(numThreads);
    
    for (Runnable command : commands)
      pool.submit(command);
    
    pool.shutdown();
    
    try {
      pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  public static int[][] getTestSets(int n) {
    int p = 2;
    while (!(n <= p * p + p + 1))
      p = BigInteger.valueOf(p).nextProbablePrime().intValue();
    
    System.err.println("projective plane prime is " + p);
    
    int[][] projective = new int[p * p + p + 1][3];
    int index = 0;
    
    for (int a = 0; a != 2; ++a)
      for (int b = 0; b != p; ++b)
        for (int c = 0; c != p; ++c) {
          if (a == 0) {
            if (b > 1)
              continue;
            
            if (b == 0)
              if (c != 1)
                continue;
          }
          
          int[] point = projective[index++];
          point[0] = a;
          point[1] = b;
          point[2] = c;
        }
    
    ArrayList<int[]> result = new ArrayList<int[]>();
    for (int i = 0; i != projective.length; ++i) {
      ArrayList<Integer> list = new ArrayList<Integer>();
      for (int j = 0; j != n; ++j) {
        int[] u = projective[i];
        int[] v = projective[j];
        if ((u[0] * v[0] + u[1] * v[1] + u[2] * v[2]) % p == 0)
          list.add(j);
      }
      
      int[] set = new int[list.size()];
      for (int k = 0; k != set.length; ++k)
        set[k] = list.get(k);
      
      result.add(set);
    }
    
    return result.toArray(new int[][] {});
  }
  
  public static <A extends Comparable<? super A>> ArrayList<TreeSet<A>> findComponents(
      TreeMap<A, String> names, TreeSet<Pair<A, A>> neighbours, String matches, int minNumPoints)
      throws IOException {
    ArrayList<TreeSet<A>> components = new ArrayList<TreeSet<A>>();
    
    TreeMap<A, TreeSet<A>> edges = new TreeMap<A, TreeSet<A>>();
    
    for (Pair<A, A> key : neighbours) {
      LinkedList<double[]> list =
          new LinkedList<double[]>(StitchTools.readControlPoints(new File(matches + "/"
              + names.get(key.getA()) + "-" + names.get(key.getB()) + ".pto")));
      if (list.size() >= minNumPoints) {
        TreeSet<A> set = edges.get(key.getA());
        if (set == null) {
          set = new TreeSet<A>();
          edges.put(key.getA(), set);
        }
        
        set.add(key.getB());
        
        set = edges.get(key.getB());
        if (set == null) {
          set = new TreeSet<A>();
          edges.put(key.getB(), set);
        }
        
        set.add(key.getA());
      }
    }
    
    TreeSet<A> remaining = new TreeSet<A>(names.keySet());
    while (!remaining.isEmpty()) {
      TreeSet<A> component = new TreeSet<A>();
      TreeSet<A> queue = new TreeSet<A>();
      queue.add(remaining.first());
      while (!queue.isEmpty()) {
        A a = queue.pollFirst();
        if (component.add(a)) {
          TreeSet<A> set = edges.get(a);
          if (set != null)
            queue.addAll(set);
        }
      }
      
      remaining.removeAll(component);
      components.add(component);
    }
    
    return components;
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesZ80_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 7, 6, 6, 6, 6, 6}, true, false, "018.png");
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesZ80_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {10, 11, 10, 11, 10, 10, 10, 10, 9, 10, 10, 11},
        false, false, "000022.png", "000102.png");
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesZ80_df_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {8, 8, 7, 7, 7, 7, 8, 8, 8, 8}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesZ80_t1_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 8, 7, 8, 8, 8, 7, 8, 7, 8}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6800_p_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8080_t_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {4, 4, 5, 4, 4}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8080_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {4, 4, 5, 4, 4, 4}, false, true, "11.png",
        "27.png");
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8080_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {11, 11, 11, 11, 11, 11, 11, 11, 10, 11, 11, 11,
        11, 12}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6532_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 6, 6, 6, 6, 6, 5, 6}, true, false, "002.jpg",
        "022.jpg", "027.jpg", "037.jpg");
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6800_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {6, 6, 6, 6, 6, 6, 6}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6800_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {10, 10, 10, 10, 9, 9, 9, 10, 9, 9, 9, 10},
        false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6800_df_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {10, 10, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9}, false,
        false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6800_p1_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {9, 10, 9, 9, 9, 9, 9, 10, 10, 10, 9}, false,
        false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8085_t_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {3, 3, 3}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8085_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {10, 10, 10, 10, 11, 10, 10, 10, 10, 10}, false,
        false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames68000_t_bf_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {6, 7, 6, 7, 6, 6, 6, 6, 6, 7}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String>
      prepareNames68000_tCrossedPolarizers_bf_10x(String dir) throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 6, 6, 6, 6, 6, 6, 7, 6, 7}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6507_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6507_t_df_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {8, 8, 7, 8, 8, 8, 8, 8, 8, 8, 8}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6507_t_bf_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {4, 4, 4, 4, 4, 4}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames68000_t1_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {3, 3, 3, 3, 3}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames68000_t1_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {11, 12, 11, 12, 12, 12, 12, 12, 12, 12, 12, 13,
        12, 12, 12, 12, 12, 12}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames68000L_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
        12, 13, 12, 12, 12, 13}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String>
      prepareNames68000L_t_str1_bf_20x(String dir) throws IOException {
    return prepareNamesAlternating(dir, new int[] {12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
        12, 12, 12, 12, 12}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String>
      prepareNames68000L_wirebond_bf_5x(String dir) throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 8, 9, 8, 8, 8, 8, 8, 8, 8}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames68000L_t_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {6, 6, 6, 7, 7, 6, 6, 7}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames8086_t_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {3, 3, 3, 3}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesX_mobo(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {4, 4, 4}, false, true);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesX_cray(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {2, 2, 2}, true, true);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesULA(String dir)
      throws IOException {
    return prepareNamesStandardScan(dir, 4, 6, false, true, true);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames2650_t_bf_5x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {5, 5, 5, 5, 5, 5, 5}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames2650_t_bf_10x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {4, 4, 4, 4, 4}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames1802_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {11, 11, 11, 11, 10, 11, 10, 11, 11, 11, 11},
        false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames1802_t_clean_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {11, 11, 11, 11, 11, 11, 11, 11, 12, 11, 12},
        new boolean[] {false, true, false, true, false, true, false, false, true, false, true});
  }
  
  // map 72.png to the place of 62.png
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6522_t_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 7, 7, 7, 7, 7, 7, 7, 7, 0, 1}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNames6522_t_clean_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {7, 7, 7, 7, 7, 7, 7, 7, 7}, false, false);
  }
  
  public static TreeMap<Pair<Integer, Integer>, String> prepareNamesZ80_strp1_bf_20x(String dir)
      throws IOException {
    return prepareNamesAlternating(dir, new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, false, false);
  }
  
  public static void main(String[] args) throws IOException {
    final String[] identifiers = new String[] {"6522/t-clean/bf/20x"};
    final String version = "";
    
    final String[] data = new String[identifiers.length];
    final String[] matches = new String[identifiers.length];
    final String[] parameters = new String[identifiers.length];
    final String[] table = new String[identifiers.length];
    final String[] mapImage = new String[identifiers.length];
    final String[] stitchImage = new String[identifiers.length];
    
    for (int i = 0; i != identifiers.length; ++i) {
      data[i] = OTHER_DIR + "/" + identifiers[i] + "/data";
      matches[i] = OTHER_DIR + "/" + identifiers[i] + "/matches";
      parameters[i] = OTHER_DIR + "/" + identifiers[i] + "/params" + version + ".txt";
      table[i] = OTHER_DIR + "/" + identifiers[i] + "/lookup" + version + ".txt";
      mapImage[i] = OTHER_DIR + "/" + identifiers[i] + "/map" + version + ".png";
      stitchImage[i] = OTHER_DIR + "/" + identifiers[i] + "/stitch" + version + ".png";
    }
    
    final TreeMap<Pair<Integer, Integer>, String>[] names = new TreeMap[identifiers.length];
    names[0] = prepareNames6522_t_clean_bf_20x(data[0]);
    
    for (Entry<Pair<Integer, Integer>, String> entry : names[0].entrySet())
      System.err.println(entry);
    
    final TreeSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>[] neighbours =
        new TreeSet[identifiers.length];
    neighbours[0] = findNeighbours(names[0].keySet(), 1.0, false);
    
    for (int i = 0; i != identifiers.length; ++i)
      findKeypointsMulti(names[i], neighbours[i], data[i], matches[i], 800, 600, 4);
    
    for (int i = 0; i != identifiers.length; ++i) {
      System.err.println("layer " + i + ":");
      
      ArrayList<TreeSet<Pair<Integer, Integer>>> components =
          findComponents(names[i], neighbours[i], matches[i], 1);
      TreeSet<Pair<Integer, Integer>> comp = null;
      for (TreeSet<Pair<Integer, Integer>> c : components) {
        System.err.println("component of size " + c.size());
        if (comp == null || c.size() > comp.size())
          comp = c;
      }
      
      System.err.println(names[i].size() + " images in layer " + i + ", " + comp.size()
          + " in largest component");
      
      for (Iterator it = names[i].keySet().iterator(); it.hasNext();)
        if (!comp.contains(it.next()))
          it.remove();
      
      for (Iterator it = neighbours[i].iterator(); it.hasNext();) {
        Pair key = (Pair) it.next();
        if (!(comp.contains(key.getA()) && comp.contains(key.getB())))
          it.remove();
      }
      
      printLookup(names[i], table[i]);
    }
    
    int[] size = getSize(data[0] + "/" + names[0].firstEntry().getValue());
    System.err.println(size[0] + ", " + size[1]);
    
    //*
    StatisticalSolver stitcher =
        new StatisticalSolver<Pair<Integer, Integer>>(size[0], size[1], 2, names, neighbours,
            matches, new double[] {1}, 500, 400);
    
    ImageSetProperties[] sets = stitcher.analyze(true);
    for (int i = 0; i != identifiers.length; ++i) {
      sets[i] = StitchTools.normalizeImageSetProperties(sets[i]);
      sets[i].print(parameters[i]);
    }
    
    /*/

    ImageSetProperties[] sets = new ImageSetProperties[identifiers.length];
    for (int i = 0; i != identifiers.length; ++i)
      sets[i] = new ImageSetProperties(parameters[i]);
    //*/

    AffineTransform[] transforms = new AffineTransform[identifiers.length];
    for (int i = 0; i != identifiers.length; ++i)
      transforms[i] = AffineTransform.ID;
    
    // transform for 68000, layer t1: 563, 228, 15615, 17377
    //transforms[0] = transforms[0].after(AffineTransform.getRotation(2 * Math.PI / 360 * 0.068));
    
    // transform for 6800, layer t
    //transforms[0] = transforms[0].after(AffineTransform.getRotation(2 * Math.PI / 360 * -0.37));
    //transforms[0] = transforms[0].after(AffineTransform.getScaling(1 - 0.00088));
    //transforms[0] = transforms[0].after(AffineTransform.getTranslation(0, 0));
    
    // transform for 6800, layer p
    //transforms[0] = transforms[0].after(AffineTransform.getRotation(2 * Math.PI / 360 * 0.666));
    
    for (int i = 0; i != identifiers.length; ++i) {
      StitchStackProperties stack =
          new StitchStackProperties(new ImageSetProperties[] {sets[i]},
              new AffineTransform[] {transforms[i]}, 0, 0, 0, 0);
      double[] boundary = Utils.getBoundary(stack);
      System.err
          .println(boundary[0] + ", " + boundary[1] + ", " + boundary[2] + ", " + boundary[3]);
      
      int sx = (int) (boundary[2] - boundary[0]) + 1;
      int sy = (int) (boundary[3] - boundary[1]) + 1;
      System.err.println(sx + ", " + sy);
      
      stack =
          new StitchStackProperties(new ImageSetProperties[] {sets[i]},
              new AffineTransform[] {transforms[i]}, (int) boundary[0], (int) boundary[1],
              (int) boundary[2] + 1, (int) boundary[3] + 1);
      
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges =
          CellFinder.computeVoroniEdges(stack, 0, 0, 2);
      BufferedImage colorMap = SharpnessEvaluator.render(sx, sy, 1, edges);
      {
        Tools.displayImages(colorMap);
        ImageIO.write(colorMap, "png", new File(mapImage[i]));
      }
      
      TreeMap<Integer, Pair<Integer, Integer>> indexLookup =
          StitchTools.getIndexLookup(names[i].keySet());
      
      int outScale = 1;
      int inScale = 1;
      Interpolator interpolator = Sharpness.BICUBIC_HOMOGENEOUS;
      
      //*/
      BufferedImage[] images = new BufferedImage[sets[i].getNumImages()];
      for (int j = 0; j != images.length; ++j) {
        System.err.println("loading image " + j + "...");
        images[j] = ImageIO.read(new File(data[i] + "/" + names[i].get(indexLookup.get(j))));
      }
      
      BufferedImage result =
          Sharpness.render(outScale, stack, new int[] {0, 0, 0}, 0, images, interpolator, inScale,
              edges);
      
      /*/// for RAM-expensive image sets
      for (int j = 0; j != 3; ++j) {
        ImageSize imageSize = sets[i].getSize();
        int numImages = sets[i].getNumImages();
        
        double[][][] matrices = new double[numImages][imageSize.getSy()][imageSize.getSx()];
        ContinuousImage[] contImages = new ContinuousImage[numImages];
        
        for (int k = 0; k != numImages; ++k) {
          System.err.println("loading image " + k + "...");
          Sharpness.getChannel(
              ImageIO.read(new File(data[i] + "/" + names[i].get(indexLookup.get(k)))),
              matrices[k], j);
          contImages[k] = interpolator.getContinuousImage(matrices[k]);
        }
        
        DataOutputStream out =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(OTHER_DIR + "/"
                + identifiers[i] + "/c" + j + ".dat")));
        Sharpness.render(outScale, stack, 0, 0, contImages, inScale, edges, out);
        out.close();
      }

      BufferedImage result =
          new BufferedImage(outScale * (stack.getX1() - stack.getX0()), outScale
              * (stack.getY1() - stack.getY0()), BufferedImage.TYPE_INT_RGB);
      double[][] channel = new double[result.getHeight()][result.getWidth()];
      for (int j = 0; j != 3; ++j) {
        System.err.println("loading channel " + j + "...");
        DataInputStream in =
            new DataInputStream(new BufferedInputStream(new FileInputStream(OTHER_DIR + "/"
                + identifiers[i] + "/c" + j + ".dat")));
        for (int y = 0; y != result.getHeight(); ++y)
          for (int x = 0; x != result.getWidth(); ++x)
            channel[y][x] = in.readDouble();
        in.close();
        
        Sharpness.setChannel(result, channel, j);
      }
      
      //*/

      //Tools.displayImages(result, colorMap);
      System.err.println("writing result...");
      ImageIO.write(result, "png", new File(stitchImage[i]));
    }
  }
}
