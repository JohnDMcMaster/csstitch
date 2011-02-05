package other;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools2 {
  
  public static TreeSet<String> readNodes(String filename) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    
    Pattern pattern = Pattern.compile("\\s*(\\S+)\\s*,\\s*(\\S+)\\s*,\\s*(\\S+)\\s*");
    
    TreeSet<String> result = new TreeSet<String>();
    for (;;) {
      String line = reader.readLine();
      if (line == null)
        break;
      
      line = line.trim();
      if (line.isEmpty() || line.startsWith("//"))
        continue;
      
      if (!result.add(line))
        throw new IOException("double node encountered: " + line);
    }
    
    return result;
  }
  
  public static String[][] readTransistors(String filename) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    
    Pattern pattern = Pattern.compile("\\s*(\\S+)\\s*,\\s*(\\S+)\\s*,\\s*(\\S+)\\s*");
    
    ArrayList<String[]> result = new ArrayList<String[]>();
    for (;;) {
      String line = reader.readLine();
      if (line == null)
        break;
      
      line = line.trim();
      if (line.isEmpty() || line.startsWith("//"))
        continue;
      
      Matcher matcher = pattern.matcher(line);
      if (matcher.matches())
        result.add(new String[] {matcher.group(1), matcher.group(2), matcher.group(3)});
    }
    
    return result.toArray(new String[][] {});
  }
  
  private static <A extends Comparable<? super A>> void trigraphAdd(
      TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph, A a, A b, A c) {
    TreeMap<A, TreeSet<A>> map = trigraph.get(a);
    if (map == null) {
      map = new TreeMap<A, TreeSet<A>>();
      trigraph.put(a, map);
    }
    
    TreeSet<A> set = map.get(b);
    if (set == null) {
      set = new TreeSet<A>();
      map.put(b, set);
    }
    
    set.add(c);
  }
  
  public static <A extends Comparable<? super A>> TreeMap<A, TreeMap<A, TreeSet<A>>>
      trigraphCreate(A[][] input, int pos0, int pos1, int pos2) {
    if (input.length == 0)
      return null;
    
    int qos0 = pos0 == 0 ? 0 : 3 - pos0;
    int qos1 = pos1 == 0 ? 0 : 3 - pos1;
    int qos2 = pos2 == 0 ? 0 : 3 - pos2;
    
    TreeMap<A, TreeMap<A, TreeSet<A>>> result = new TreeMap<A, TreeMap<A, TreeSet<A>>>();
    for (A[] a : input) {
      trigraphAdd(result, a[pos0], a[pos1], a[pos2]);
      trigraphAdd(result, a[qos0], a[qos1], a[qos2]);
    }
    
    return result;
  }
  
  public static <A> TreeMap<A, TreeSet<A>> trigraphGet(TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph,
      A a0) {
    if (trigraph != null) {
      TreeMap<A, TreeSet<A>> map = trigraph.get(a0);
      if (map != null)
        return map;
    }
    
    return new TreeMap<A, TreeSet<A>>();
  }
  
  public static <A> TreeSet<A> trigraphGet(TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph, A a0, A a1) {
    if (trigraph != null) {
      TreeMap<A, TreeSet<A>> map = trigraph.get(a0);
      if (map != null) {
        TreeSet<A> set = map.get(a1);
        if (set != null)
          return set;
      }
    }
    
    return new TreeSet<A>();
  }
  
  public static <A> boolean trigraphGet(TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph, A a0, A a1,
      A a2) {
    if (trigraph != null) {
      TreeMap<A, TreeSet<A>> map = trigraph.get(a0);
      if (map != null) {
        TreeSet<A> set = map.get(a1);
        if (set != null)
          return set.contains(a2);
      }
    }
    
    return false;
  }
  
}
