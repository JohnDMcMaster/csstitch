package other;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

public class Simulator3 {
  
  static final String dir = "/home/noname/6502";
  
  static TreeSet<String> nodes;
  static TreeMap<String, Integer> strengths;
  
  static TreeSet<String> inputs, outputs;
  
  static TreeMap<String, TreeMap<String, TreeSet<String>>> switchesPCC;
  static TreeMap<String, TreeMap<String, TreeSet<String>>> switchesCPC;
  static TreeMap<String, TreeMap<String, TreeSet<String>>> switchesCCP;
  
  static {
    try {
      nodes = Tools2.readNodes(dir + "/my-nodes.txt");
      
      strengths = new TreeMap<String, Integer>();
      for (String node : nodes)
        if (node.matches("vss.*"))
          strengths.put(node, 4);
        else if (node.matches("vcc.*"))
          strengths.put(node, 3);
        //else if (node.matches("(vss|vcc)-weak"))
        //  strengths.put(node, 3);
        //else if (node.matches("ab\\d+"))
        //  strengths.put(node, 3);
        else if (node.matches("(idb|sb|adh|adl)[0-7]"))
          strengths.put(node, 2);
        else
          strengths.put(node, 1);
      
      // model capacitances according to region sizes
      
      // compensate for weird wiring of IR:
      strengths.put("724", 0);
      strengths.put("237", 0);
      strengths.put("343", 0);
      strengths.put("1590", 0);
      strengths.put("703", 0);
      strengths.put("1378", 0);
      strengths.put("74", 0);
      strengths.put("1183", 0);
      
      // compensate for weird wiring of address bus:
      strengths.put("246", 0);
      strengths.put("416", 0);
      strengths.put("1636", 0);
      strengths.put("864", 0);
      strengths.put("738", 0);
      strengths.put("463", 0);
      strengths.put("524", 0);
      strengths.put("577", 0);
      strengths.put("705", 0);
      strengths.put("1298", 0);
      strengths.put("836", 0);
      strengths.put("1667", 0);
      strengths.put("1451", 0);
      strengths.put("1353", 0);
      strengths.put("1514", 0);
      strengths.put("514", 0);
      
      inputs = new TreeSet<String>();
      outputs = new TreeSet<String>();
      for (String node : nodes)
        if (node.startsWith("input-"))
          inputs.add(node);
        else if (node.startsWith("output-"))
          outputs.add(node);
      inputs.remove("input-phi-1");
      inputs.remove("input-phi-2");
      
      String[][] transistors = Tools2.readTransistors(dir + "/my-trans.txt");
      switchesPCC = Tools2.trigraphCreate(transistors, 0, 1, 2);
      switchesCPC = Tools2.trigraphCreate(transistors, 2, 0, 1);
      switchesCCP = Tools2.trigraphCreate(transistors, 1, 2, 0);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static boolean isReal(String node) {
    return !(node.startsWith("vcc") || node.startsWith("vss"));
  }
  
  public static String[] getParents(TreeMap<String, String> parents, String node) {
    LinkedList<String> list = new LinkedList<String>();
    do {
      list.addFirst(node);
      node = parents.get(node);
    } while (node != null);
    return list.toArray(new String[] {});
  }
  
  public static void main(String[] args) {
    for (String a : nodes)
      if (Tools2.trigraphGet(switchesPCC, a).containsKey(a))
        throw new RuntimeException();
    
    for (String a : nodes)
      if (Tools2.trigraphGet(switchesCCP, a).containsKey(a))
        throw new RuntimeException(a);
    
    TreeSet<String> all = new TreeSet<String>();
    for (String node : nodes)
      if (isReal(node))
        all.add(node);
    
    while (!all.isEmpty()) {
      boolean found = false;
      String node = all.first();
      
      TreeMap<String, String> parents = new TreeMap<String, String>();
      TreeSet<String> queue = new TreeSet<String>();
      parents.put(node, null);
      queue.add(node);
      while (!queue.isEmpty()) {
        node = queue.pollFirst();
        for (String child : Tools2.trigraphGet(switchesCCP, node).keySet())
          if (isReal(child))
            if (parents.containsKey(child)) {
              String[] a = getParents(parents, child);
              String[] b = getParents(parents, node);
              int i = 0;
              while (i != a.length && i != b.length && a[i].equals(b[i]))
                ++i;
              
              ArrayList<String> circle = new ArrayList<String>();
              for (int j = i - 1; j != a.length; ++j)
                circle.add(a[j]);
              for (int j = b.length - 1; j != i - 1; --j)
                circle.add(b[j]);
              
              if (circle.size() >= 3) {
                found = true;
                System.err.println("circle: " + circle);
              }
            } else {
              parents.put(child, node);
              queue.add(child);
            }
      }
      
      all.removeAll(parents.keySet());
      
      if (found)
        System.err.println("======");
    }
  }
  
}
