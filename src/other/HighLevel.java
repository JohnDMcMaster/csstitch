package other;

import general.collections.Sets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class HighLevel {
  
  public static TreeSet<Integer> nodes;
  public static TreeSet<Integer> pullups;
  
  public static TreeMap<String, Integer> nameToNode;
  public static TreeMap<Integer, String> nodeToName;
  
  public static TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByChildren;
  public static TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByParent;
  public static TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByChildParent;
  
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
  
  static {
    try {
      int[][] segments = Tools.readSegments();
      nodes = Tools.getNodes(segments);
      pullups = Tools.getPullups(segments);
      
      nameToNode = Tools.readNames();
      nodeToName = Tools.reverseNames(nameToNode);
      
      int[][] transistors = Tools.readTransistors();
      
      switchesByChildren = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
      switchesByParent = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
      switchesByChildParent = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
      
      for (int[] t : transistors)
        if (t[0] != Tools.GND && t[1] != t[2])
          for (int i = 1; i != 3; ++i) {
            trigraphAdd(switchesByParent, t[0], t[i], t[3 - i]);
            trigraphAdd(switchesByChildParent, t[3 - i], t[0], t[i]);
            trigraphAdd(switchesByChildren, t[i], t[3 - i], t[0]);
          }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static <A extends Comparable<? super A>> TreeSet<A> trigraphGet(
      TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph, A a, A b) {
    TreeMap<A, TreeSet<A>> map = trigraph.get(a);
    if (map != null) {
      TreeSet<A> set = map.get(b);
      if (set != null)
        return set;
    }
    
    return new TreeSet<A>();
  }
  
  private static <A extends Comparable<? super A>> TreeMap<A, TreeSet<A>> trigraphGet(
      TreeMap<A, TreeMap<A, TreeSet<A>>> trigraph, A a) {
    TreeMap<A, TreeSet<A>> map = trigraph.get(a);
    if (map != null)
      return map;
    
    return new TreeMap<A, TreeSet<A>>();
  }
  
  public static void printNode(int node) {
    String name = nodeToName.get(node);
    if (name != null)
      System.err.print(name);
    else
      System.err.print(node);
    
    if (pullups.contains(node))
      System.err.print("+");
  }
  
  public static TreeSet<Integer> getCircuit(int node) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    TreeSet<Integer> queue = new TreeSet<Integer>();
    queue.add(node);
    while (!queue.isEmpty()) {
      node = queue.pollFirst();
      if (result.add(node) && node != Tools.GND && node != Tools.VCC)
        queue.addAll(trigraphGet(switchesByChildren, node).keySet());
    }
    return result;
  }
  
  public static Integer isFan(int node, boolean dir) {
    TreeSet<Integer> a = trigraphGet(switchesByChildren, node, dir ? Tools.VCC : Tools.GND);
    TreeSet<Integer> b = trigraphGet(switchesByChildren, node, dir ? Tools.GND : Tools.VCC);
    
    for (int d : a)
      for (int c : b)
        if (pullups.contains(c)) {
          TreeMap<Integer, TreeSet<Integer>> map = trigraphGet(switchesByChildren, c);
          if (map.size() == 1 && map.firstKey() == Tools.GND) {
            TreeSet<Integer> set = map.firstEntry().getValue();
            if (set.size() == 1 && set.first() == d)
              return d;
          }
        }
    
    return null;
  }
  
  public static boolean match(TreeMap<Integer, TreeSet<Integer>> u,
      TreeMap<Integer, TreeSet<Integer>> v) {
    for (int a : u.keySet()) {
      TreeSet<Integer> x = u.get(a);
      TreeSet<Integer> y = v.get(a);
      if (y == null)
        return false;
      
      if (!Sets.symmetricDifference(x, y).isEmpty())
        return false;
    }
    
    return true;
  }
  
  public static boolean isGeneralizedFan(int node) {
    TreeSet<Integer> set = trigraphGet(switchesByChildren, node, Tools.VCC);
    if (set.size() != 1)
      return false;
    
    int alter = set.first();
    if (!pullups.contains(alter))
      return false;
    
    // verify that alter without the pullup is equivalent to node without the VCC switch
    // first-order approximation
    TreeMap<Integer, TreeSet<Integer>> u =
        new TreeMap<Integer, TreeSet<Integer>>(trigraphGet(switchesByChildren, node));
    TreeMap<Integer, TreeSet<Integer>> v =
        new TreeMap<Integer, TreeSet<Integer>>(trigraphGet(switchesByChildren, alter));
    
    u.remove(Tools.VCC);
    return match(u, v) && match(v, u);
  }
  
  public static int getNode(Object o) {
    if (o instanceof Integer)
      return (Integer) o;
    
    String s = (String) o;
    if (s.matches("\\d+"))
      return Integer.parseInt(s);
    
    return nameToNode.get(s);
  }
  
  public static void printTriplet(int gate, int from, int to) {
    printNode(from);
    System.err.print(" ==(");
    printNode(gate);
    System.err.print(")== ");
    printNode(to);
    System.err.println();
  }
  
  public static void printGate(int node) {
    TreeMap<Integer, TreeSet<Integer>> map = trigraphGet(switchesByParent, node);
    for (int a : map.keySet())
      for (int b : map.get(a))
        if (a < b)
          printTriplet(node, a, b);
    System.err.println();
  }
  
  public static void printCircuit(int node) {
    printNode(node);
    System.err.print(": ");
    
    TreeSet<Integer> circuit = getCircuit(node);
    System.err.print("[");
    for (Iterator<Integer> it = circuit.iterator(); it.hasNext();) {
      printNode(it.next());
      if (it.hasNext())
        System.err.print(", ");
    }
    System.err.println("]");
    
    for (int a : circuit)
      for (int b : circuit)
        if (a < b)
          for (int c : trigraphGet(switchesByChildren, a, b))
            printTriplet(c, a, b);
    System.err.println();
  }
  
  public static ArrayList<Integer> findPathToParent(int from, int to, int maxDepth) {
    int[] distances = new int[nodes.last() + 1];
    for (int i = 0; i != nodes.last() + 1; ++i)
      distances[i] = Integer.MAX_VALUE;
    return findPathToParent(from, to, maxDepth, 0, distances);
  }
  
  public static ArrayList<Integer> findPathToParent(int from, int to, int maxDepth, int i,
      int[] distances) {
    if (i >= distances[from])
      return null;
    
    distances[from] = i;
    
    if (from == to)
      return new ArrayList<Integer>();
    
    if (i == maxDepth)
      return null;
    
    TreeSet<Integer> circuit = getCircuit(from);
    TreeSet<Integer> gates = new TreeSet<Integer>();
    for (int node : circuit)
      if (node != Tools.VCC && node != Tools.GND)
        gates.addAll(trigraphGet(switchesByChildParent, node).keySet());
    
    for (int node : gates) {
      ArrayList<Integer> result = findPathToParent(node, to, maxDepth, i + 1, distances);
      if (result != null) {
        result.add(node);
        return result;
      }
    }
    
    return null;
  }
  
  public static void main(String[] args) throws IOException {
    System.err.println("num nodes: " + nodes.size());
    System.err.println("num relevant nodes: " + switchesByParent.size());
    
    TreeSet<Integer> generalizedPullups = new TreeSet<Integer>(pullups);
    for (int n : nodes)
      if (isFan(n, true) != null || isFan(n, false) != null || isGeneralizedFan(n))
        generalizedPullups.add(n);
    
    TreeSet<Integer> interesting = new TreeSet<Integer>(switchesByParent.keySet());
    interesting.removeAll(generalizedPullups);
    
    // strange nodes
    // 9 ==(866)== VCC, 9 and 866 otherwise unconnected
    interesting.remove(866);
    interesting.remove(9);
    
    System.err.println("num non-combinatorial nodes: " + interesting.size());
    
    TreeMap<Integer, Integer> hist = new TreeMap<Integer, Integer>();
    
    for (Iterator<Integer> it = interesting.iterator(); it.hasNext();) {
      int n = it.next();
      TreeMap<Integer, TreeSet<Integer>> map = trigraphGet(switchesByChildren, n);
      if (map.size() == 1 && generalizedPullups.contains(map.firstKey())) {
        it.remove();
        
        for (int key : map.firstEntry().getValue()) {
          Integer c = hist.get(key);
          if (c == null)
            c = 0;
          hist.put(key, c + 1);
        }
      }
    }
    
    System.err.println("without trivial dynamic latches: " + interesting.size());
    
    for (int n : interesting)
      if (nodeToName.containsKey(n)) {
        //printNode(n);
        //System.err.println();
        //printCircuit(n);
      }
    System.err.println();
    
    for (int depth = 0; depth != 0; ++depth) {
      System.err.println("depth " + depth + "...");
      
      ArrayList<Integer> path = findPathToParent(getNode("sb0"), getNode("C1x5Reset"), depth);
      if (path != null) {
        System.err.println("found path of length " + depth + ": ");
        for (int node : path) {
          printNode(node);
          System.err.print(", ");
        }
        System.err.println();
        break;
      }
    }
    
    int node = getNode("1325");
    
    printCircuit(node);
    //printGate(node);
  }
}
