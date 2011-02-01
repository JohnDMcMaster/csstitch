package other;

import general.Statistics;
import general.collections.Maps;
import general.collections.Sets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

import cache.Cache;

public class Eval {
  
  public static TreeMap<Integer, TreeSet<Integer>> getNodeToTransistorsMap() throws IOException {
    TreeMap<Integer, TreeSet<Integer>> result = Cache.cache("other-node-to-transistors");
    if (result != null)
      return result;
    
    int[][] segments = Tools.readSegments();
    int[][] transistors = Tools.readTransistors();
    
    TreeSet<Integer> nodes = Tools.getNodes(segments);
    
    result = new TreeMap<Integer, TreeSet<Integer>>();
    for (int n : nodes)
      result.put(n, new TreeSet<Integer>());
    
    for (int i = 0; i != transistors.length; ++i)
      for (int j = 0; j != 3; ++j)
        result.get(transistors[i][j]).add(i);
    
    return result;
  }
  
  public static boolean isGndVccTransistor(int[] t) {
    for (int i = 1; i != 3; ++i)
      if (t[i] == Tools.GND || t[i] == Tools.VCC)
        return true;
    
    return false;
  }
  
  @SuppressWarnings("unchecked")
  public static TreeSet<Integer>[] getCircuits() throws IOException {
    TreeSet<Integer>[] result = Cache.cache("other-circuits");
    if (result != null)
      return result;
    
    int[][] segments = Tools.readSegments();
    int[][] transistors = Tools.readTransistors();
    
    TreeSet<Integer> nodes = Tools.getNodes(segments);
    TreeMap<Integer, TreeSet<Integer>> nodeToTransistors = getNodeToTransistorsMap();
    
    nodes.remove(Tools.GND);
    nodes.remove(Tools.VCC);
    
    ArrayList<TreeSet<Integer>> circuits = new ArrayList<TreeSet<Integer>>();
    while (!nodes.isEmpty()) {
      int n = nodes.pollFirst();
      TreeSet<Integer> circuit = new TreeSet<Integer>();
      Stack<Integer> queue = new Stack<Integer>();
      queue.add(n);
      while (!queue.isEmpty()) {
        n = queue.pop();
        if (circuit.add(n))
          for (int i : nodeToTransistors.get(n)) {
            int[] t = transistors[i];
            if (!isGndVccTransistor(t)) {
              if (t[1] == n)
                queue.add(t[2]);
              if (t[2] == n)
                queue.add(t[1]);
            }
          }
      }
      
      nodes.removeAll(circuit);
      circuits.add(circuit);
    }
    
    return circuits.toArray(new TreeSet[] {});
  }
  
  public static TreeMap<Integer, TreeSet<Integer>> getCircuitGraph() throws IOException {
    TreeMap<Integer, TreeSet<Integer>> result = Cache.cache("other-circuit-graph");
    if (result != null)
      return result;
    
    int[][] transistors = Tools.readTransistors();
    
    TreeSet<Integer>[] circuits = getCircuits();
    TreeMap<Integer, Integer> circuitMap = getComponentMap(circuits);
    
    result = new TreeMap<Integer, TreeSet<Integer>>();
    for (int i = 0; i != circuits.length; ++i)
      result.put(i, new TreeSet<Integer>());
    
    for (int[] t : transistors)
      if (!(t[0] == Tools.GND || t[0] == Tools.VCC))
        for (int i = 1; i != 3; ++i)
          if (!(t[i] == Tools.GND || t[i] == Tools.VCC))
            result.get(circuitMap.get(t[0])).add(circuitMap.get(t[i]));
    
    return result;
  }
  
  public static <A extends Comparable<A>> TreeSet<A> findReachable(TreeMap<A, TreeSet<A>> map,
      TreeSet<A> vertices, A u) {
    TreeSet<A> result = new TreeSet<A>();
    Stack<A> queue = new Stack<A>();
    queue.add(u);
    while (!queue.isEmpty()) {
      u = queue.pop();
      if (vertices.contains(u) && result.add(u))
        queue.addAll(map.get(u));
    }
    
    return result;
  }
  
  public static <A extends Comparable<A>> TreeSet<A> findComponent(TreeMap<A, TreeSet<A>> map,
      TreeMap<A, TreeSet<A>> reverseMap, TreeSet<A> vertices, A u) {
    return Sets.intersection(findReachable(map, vertices, u),
        findReachable(reverseMap, vertices, u));
  }
  
  @SuppressWarnings("unchecked")
  public static <A extends Comparable<A>> TreeSet<A>[] findComponents(TreeMap<A, TreeSet<A>> map) {
    ArrayList<TreeSet<A>> result = new ArrayList<TreeSet<A>>();
    
    TreeMap<A, TreeSet<A>> reverseMap = Maps.invertGraph(map);
    TreeSet<A> vertices = new TreeSet<A>(map.keySet());
    
    while (!vertices.isEmpty()) {
      A u = Math.random() < 0.5 ? vertices.first() : vertices.last();
      TreeSet<A> component = findComponent(map, reverseMap, vertices, u);
      vertices.removeAll(component);
      result.add(component);
    }
    
    return result.toArray(new TreeSet[] {});
  }
  
  public static <A extends Comparable<A>> TreeMap<A, Integer> getComponentMap(
      TreeSet<A>[] components) {
    TreeMap<A, Integer> result = new TreeMap<A, Integer>();
    for (int i = 0; i != components.length; ++i)
      for (A u : components[i])
        result.put(u, i);
    
    return result;
  }
  
  public static <A extends Comparable<A>> TreeMap<Integer, TreeSet<Integer>> getComponentGraph(
      TreeMap<A, TreeSet<A>> map, TreeSet<A>[] components, TreeMap<A, Integer> componentMap) {
    TreeMap<Integer, TreeSet<Integer>> result = new TreeMap<Integer, TreeSet<Integer>>();
    for (int i = 0; i != components.length; ++i)
      result.put(i, new TreeSet<Integer>());
    
    for (Entry<A, TreeSet<A>> entry : map.entrySet())
      for (A v : entry.getValue())
        result.get(componentMap.get(v)).add(componentMap.get(entry.getKey()));
    
    return result;
  }
  
  public static void main(String[] args) throws IOException {
    int[][] segments = Tools.readSegments();
    int[][] transistors = Tools.readTransistors();
    
    TreeSet<Integer>[] circuit = getCircuits();
    boolean[] down = new boolean[circuit.length];
    boolean[] up = new boolean[circuit.length];
    
    int[] hist = new int[4];
    int[] counts = new int[4];
    
    for (int i = 0; i != circuit.length; ++i) {
      for (int[] t : transistors)
        if (circuit[i].contains(t[1]) || circuit[i].contains(t[2])) {
          down[i] |= t[1] == Tools.GND || t[2] == Tools.GND;
          up[i] |= t[1] == Tools.VCC || t[2] == Tools.VCC;
        }
      
      if (circuit[i].size() > 0) {
        int j = (up[i] ? 2 : 0) + (down[i] ? 1 : 0);
        ++hist[j];
        counts[j] += circuit[i].size();
      }
    }
    
    System.err.println("from " + circuit.length + " circuits: ");
    for (int i = 0; i != 4; ++i)
      System.err.println(hist[i] + ": " + counts[i]);
    
    /*TreeSet<Integer>[] circuits = getCircuits();
    TreeMap<Integer, Integer> circuitMap = getComponentMap(circuits);
    
    TreeMap<Integer, TreeSet<Integer>> circuitGraph = getCircuitGraph();
    TreeSet<Integer>[] components = findComponents(circuitGraph);
    TreeMap<Integer, Integer> componentMap = getComponentMap(components);
    TreeMap<Integer, TreeSet<Integer>> graph =
        getComponentGraph(circuitGraph, components, componentMap);
    
    TreeMap<Integer, Integer> hist = new TreeMap<Integer, Integer>();
    for (TreeSet<Integer> component : components) {
      Integer c = hist.get(component.size());
      if (c == null)
        c = 0;
      hist.put(component.size(), c + 1);
    }
    
    Statistics.printMap(hist);*/
  }
}
