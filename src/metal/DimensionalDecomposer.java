package metal;

import general.Streams;
import general.Statistics;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import operations.image.ImageOpsDouble;
import realmetal.Blockarizer;

import data.Tools;
import distributed.Bootstrap;
import distributed.server.Servers;
import distributed.slaves.SlavePool;
import distributed.tunnel.Tunnel;

public class DimensionalDecomposer {
  
  public static double[][][] mutualDiff(double[][] image, double[][][] components) {
    return new double[][][] {ImageOpsDouble.subtract(image, components[1]),
        ImageOpsDouble.subtract(image, components[0])};
  }
  
  public static class WorkUnit implements Serializable {
    private static final long serialVersionUID = 1L;
    
    double[] line;
    int dir;
    int index;
    double threshold;
    
    int[] decomposition;
    double[] values;
  }
  
  public static ArrayList<WorkUnit> doStuff(ArrayList<WorkUnit> units) {
    System.err.println(Servers.HOSTNAME);
    Blockarizer blockarizer = new Blockarizer(128, 3250 / 2, 1024);
    
    int sum = 0;
    for (WorkUnit unit : units) {
      int numSegments = blockarizer.decompose(unit.line, unit.threshold);
      sum += numSegments;
      
      unit.line = blockarizer.getApproximation(numSegments);
      unit.decomposition = blockarizer.getDecomposition(numSegments);
      unit.values = blockarizer.getDecompositionValues(numSegments);
    }
    System.err.println("mean num segments: " + sum / units.size());
    
    return units;
  }
  
  public static void computeRemotely(SlavePool pool, final ArrayList<WorkUnit> workUnits) {
    final int loadLimit = 100;
    int index = 0;
    
    ArrayList<ArrayList<WorkUnit>> workload = new ArrayList<ArrayList<WorkUnit>>();
    while (index != workUnits.size()) {
      ArrayList<WorkUnit> units = new ArrayList<WorkUnit>();
      int load = 0;
      while (index != workUnits.size() && load < loadLimit) {
        WorkUnit unit = workUnits.get(index++);
        load += (unit.line.length / 1000) * (unit.line.length / 1000);
        units.add(unit);
      }
      workload.add(units);
    }
    
    workUnits.clear();
    
    final Semaphore semaphore = new Semaphore(1 - workload.size());
    
    for (ArrayList<WorkUnit> units : workload) {
      pool.submit(new SlavePool.SimpleCallback<ArrayList<WorkUnit>>() {
        public void callback(ArrayList<WorkUnit> units) {
          synchronized (workUnits) {
            workUnits.addAll(units);
          }
          semaphore.release();
        }
      }, Tunnel.getMethod("doStuff"), units);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
    try {
      semaphore.acquire();
    } catch (InterruptedException e) {
    }
  }
  
  private static SlavePool pool;
  private static int sx, sy;
  private static int step = 0;
  
  public static ArrayList<WorkUnit> prepareWorkUnits(double[][] image, int dir, double threshold) {
    ArrayList<WorkUnit> result = new ArrayList<WorkUnit>();
    int limit = ImageOpsDouble.getLimit(image, dir);
    for (int i = 0; i != limit; ++i) {
      WorkUnit unit = new WorkUnit();
      unit.line = ImageOpsDouble.getLine(image, i, dir);
      unit.dir = dir;
      unit.index = i;
      unit.threshold = threshold;
      result.add(unit);
    }
    return result;
  }
  
  public static double[][][] doStep(double[][] image, final double[][][] components,
      double threshold) throws IOException {
    ArrayList<WorkUnit> workload = new ArrayList<WorkUnit>();
    double[][][] images = mutualDiff(image, components);
    workload.addAll(prepareWorkUnits(images[0], 0, threshold));
    workload.addAll(prepareWorkUnits(images[1], 1, threshold));
    
    computeRemotely(pool, workload);
    
    final double[][][] result = new double[2][image.length][image[0].length];
    for (WorkUnit unit : workload) {
      ImageOpsDouble.setLine(result[unit.dir], unit.index, unit.dir, unit.line);
    }
    
    ImageOpsDouble.writeExp(result[0], "step-" + step + "-comp-0.png");
    ImageOpsDouble.writeExp(result[1], "step-" + step + "-comp-1.png");
    ImageOpsDouble.writeExp(ImageOpsDouble.add(result[0], result[1]), "out-" + step + ".png");
    ++step;
    return result;
  }
  
  public static void decomposeImage(double[][] image, double threshold) throws Exception {
    image = ImageOpsDouble.log(image);
    
    double[][][] components = new double[2][sy][sx];
    for (int y = 0; y != sy; ++y)
      for (int x = (y + 1) % 2; x < sx; ++x) {
        components[0][y][x] = image[y][x] / 2;
        components[1][y][x] = image[y][x] / 2;
      }
    
    for (int k = 0; k != 100; ++k) {
      System.out.println(step);
      components = doStep(image, components, (double) (0.2 * Math.exp(-0.02 * k)));
    }
    
    pool.waitTillFinished();
  }
  
  public static void main(String[] args) throws Exception {
    Bootstrap.bootstrap(Servers.CIP_91);
    double[][] image = Streams.readObject("image");
    
    pool = new SlavePool(new String[] {"-server", "-Xmx1G"}, -1);
    
    sx = image[0].length;
    sy = image.length;
    
    image = ImageOpsDouble.log(image);
    ImageOpsDouble.writeExp(image, "image.png");
    
    ArrayList<WorkUnit> units = prepareWorkUnits(image, 0, 0.01f);
    
    //computeRemotely(units);
    
    TreeMap<Integer, Integer> lengths = new TreeMap<Integer, Integer>();
    
    int counter = 0;
    for (WorkUnit unit : units) {
      if (counter == 1225)
        break;
      System.out.println(counter++);
      
      double[] line = unit.line;
      unit.line = new double[line.length];
      unit.line[0] = line[0];
      unit.line[line.length - 1] = line[line.length - 1];
      TreeSet<Double> set = new TreeSet<Double>();
      for (int i = 1; i != line.length - 1; ++i) {
        set.add(line[i - 1]);
        set.add(line[i + 0]);
        set.add(line[i + 1]);
        unit.line[i] = set.toArray(new Double[] {})[1];
      }
      
      ArrayList<WorkUnit> workload = new ArrayList<WorkUnit>();
      workload.add(unit);
      doStuff(workload);
      for (int i = 0; i != unit.decomposition.length - 1; ++i) {
        int length = unit.decomposition[i + 1] - unit.decomposition[i];
        Integer n = lengths.get(length);
        if (n == null)
          n = 0;
        lengths.put(length, n + 1);
      }
    }
    
    Statistics.printMap(lengths);
    
    pool.waitTillFinished();
  }
}
