package profile;

import general.collections.Pair;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class Profiler {
  
  private static TreeMap<Pair<Pair<String, String>, Pair<Integer, Integer>>, Long> measurements =
      new TreeMap<Pair<Pair<String, String>, Pair<Integer, Integer>>, Long>();
  
  private static int lineStart, lineEnd;
  
  private static long start, end;
  
  public static void start() {
    StackTraceElement x = Thread.currentThread().getStackTrace()[2];
    lineStart = x.getLineNumber() + 1;
    
    start = System.currentTimeMillis();
  }
  
  public static void end() {
    end = System.currentTimeMillis();
    
    StackTraceElement x = Thread.currentThread().getStackTrace()[2];
    lineEnd = x.getLineNumber();
    
    String method = x.getClassName() + "." + x.getMethodName();
    String file = x.getFileName();
    
    Pair<Integer, Integer> lines = new Pair<Integer, Integer>(lineStart, lineEnd);
    Pair<Pair<String, String>, Pair<Integer, Integer>> key =
        new Pair<Pair<String, String>, Pair<Integer, Integer>>(new Pair<String, String>(method,
            file), lines);
    Long time = measurements.get(key);
    if (time == null)
      time = 0l;
    time += end - start;
    measurements.put(key, time);
  }
  
  public static void report() {
    TreeSet<Pair<Long, Pair<Pair<String, String>, Pair<Integer, Integer>>>> set =
        new TreeSet<Pair<Long, Pair<Pair<String, String>, Pair<Integer, Integer>>>>();
    for (Entry<Pair<Pair<String, String>, Pair<Integer, Integer>>, Long> entry : measurements
        .entrySet())
      set.add(new Pair<Long, Pair<Pair<String, String>, Pair<Integer, Integer>>>(entry.getValue(),
          entry.getKey()));
    
    for (Pair<Long, Pair<Pair<String, String>, Pair<Integer, Integer>>> entry : set.descendingSet()) {
      long time = entry.getA();
      Pair<Pair<String, String>, Pair<Integer, Integer>> key = entry.getB();
      Pair<String, String> id = key.getA();
      Pair<Integer, Integer> lines = key.getB();
      System.err.println(time + ": " + id.getA() + "(" + id.getB() + ":" + lines.getA() + ") -"
          + lines.getB());
    }
  }
  
}
