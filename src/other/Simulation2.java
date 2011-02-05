package other;

import java.io.IOException;
import java.io.PrintStream;
import java.util.TreeMap;
import java.util.TreeSet;

/*
 * MAKESHIFT RULES:
 * - transistors are either open or closed, depending with delay on the value on the gate (with unknown allowing switching between both possibilities)
 * - node networks can split and connect
 * - connect to split:
 *   * if a side is driven, the drive decides the result
 *   * if a side is undriven, it inherits the previous value
 * - split to connect:
 *   * if both sides are driven, signal strength decides the winner; draw means unknown
 *   * if exactly one side is driven, this side wins
 *   * if no side is driven, stored signal strength decides the winner; draw means unknown
 * - a list of opening/closing events is maintained
 */
public class Simulation2 {
  
  static Node VSS;
  static Node VCC;
  
  static class Node {
    String id;
    
    Transistor[] transistors;
  }
  
  static class Transistor {
    String id;
    
    Node gate;
    Node left, right;
  }
  
  TreeMap<String, Node> nodes;
  TreeMap<String, Transistor> transistors;
  
  public static String getNodeName(TreeMap<Integer, String> nameToNode, int node) {
    String result = nameToNode.get(node);
    if (result == null)
      return Integer.toString(node);
    
    return result;
  }
  
  public static void printInputControl(PrintStream outNodes, PrintStream outTrans,
      TreeMap<Integer, String> nodeToName, String name, String innerName, boolean inverse) {
    outNodes.println("// INPUT: " + name.toUpperCase());
    if (!inverse)
      outNodes.println(innerName + "-inv");
    outNodes.println("input-" + name);
    outNodes.println();
    
    outTrans.println("// INPUT: " + name.toUpperCase());
    outTrans.println("input-" + name + ", vss, " + innerName + (inverse ? "" : "-inv"));
    if (!inverse) {
      outTrans.println("vcc, vcc-weak, " + innerName + "-inv");
      outTrans.println(innerName + "-inv, vss, " + innerName);
    }
    outTrans.println("vcc, vcc-weak, " + innerName);
    outTrans.println();
  }
  
  public static void printOutputControl(PrintStream outNodes, PrintStream outTrans,
      TreeMap<Integer, String> nodeToName, String name, String innerName, boolean extra) {
    if (extra)
      outNodes.println("// OUTPUT: " + name.toUpperCase());
    outNodes.println("output-" + name);
    if (extra)
      outNodes.println();
    
    if (extra)
      outTrans.println("// OUTPUT: " + name.toUpperCase());
    outTrans.println("vcc, " + innerName + ", output-" + name);
    if (extra)
      outTrans.println();
  }
  
  public static void main(String[] args) throws IOException {
    int[][] segments = Tools.readSegments();
    TreeSet<Integer> nodes = Tools.getNodes(segments);
    TreeSet<Integer> pullups = Tools.getPullups(segments);
    
    TreeMap<String, Integer> nameToNode = Tools.readNames();
    TreeMap<Integer, String> nodeToName = Tools.reverseNames(nameToNode);
    
    int[][] transistors = Tools.readTransistors();
    
    PrintStream outTrans = new PrintStream("/home/noname/6502/my-trans.txt");
    PrintStream outNodes = new PrintStream("/home/noname/6502/my-nodes.txt");
    
    // remove spam nodes
    nodes.remove(806);
    nodes.remove(866);
    nodes.remove(9);
    
    // remove clock generator
    TreeSet<Integer> clockGenerator = new TreeSet<Integer>();
    TreeSet<Integer> queue = new TreeSet<Integer>();
    queue.add(nameToNode.get("clk0"));
    while (!queue.isEmpty()) {
      int node = queue.pollFirst();
      if (node != nameToNode.get("cp1") && node != nameToNode.get("cclk") && node != Tools.GND
          && node != Tools.VCC && clockGenerator.add(node)) {
        for (int[] t : transistors)
          if (t[0] == node || t[1] == node || t[2] == node)
            for (int s : t)
              queue.add(s);
      }
    }
    
    System.err.println(clockGenerator);
    nodes.removeAll(clockGenerator);
    
    pullups.retainAll(nodes);
    
    TreeSet<Integer> badTransistors = new TreeSet<Integer>();
    for (int k = 0; k != transistors.length; ++k) {
      int[] t = transistors[k];
      if (getNodeName(nodeToName, t[0]).equals("cclk")) {
        for (int i = 1; i != 3; ++i)
          if (getNodeName(nodeToName, t[i]).matches("(sb|adh)[0-7]") && t[3 - i] == Tools.VCC)
            badTransistors.add(k);
      }
    }
    
    System.err.println(badTransistors);
    
    outNodes.println("// regular nodes");
    outTrans.println("// regular transistors");
    
    for (int n : nodes)
      outNodes.println(getNodeName(nodeToName, n));
    outNodes.println();
    
    for (int k = 0; k != transistors.length; ++k)
      if (!badTransistors.contains(k)) {
        int[] t = transistors[k];
        if (t[0] != Tools.GND && nodes.contains(t[0]) && nodes.contains(t[1])
            && nodes.contains(t[2]))
          outTrans.println(getNodeName(nodeToName, t[0]) + ", " + getNodeName(nodeToName, t[1])
              + ", " + getNodeName(nodeToName, t[2]));
      }
    outTrans.println();
    
    outTrans.println("// pullup transistors");
    for (int n : pullups)
      outTrans.println("vcc, vcc-weak, " + getNodeName(nodeToName, n));
    outTrans.println();
    
    printInputControl(outNodes, outTrans, nodeToName, "phi-1", "cp1", false);
    printInputControl(outNodes, outTrans, nodeToName, "phi-2", "cclk", false);
    
    printInputControl(outNodes, outTrans, nodeToName, "ready", "rdy", false);
    printInputControl(outNodes, outTrans, nodeToName, "so", "so", false);
    
    printInputControl(outNodes, outTrans, nodeToName, "res", "res", false);
    printInputControl(outNodes, outTrans, nodeToName, "nmi", "nmi", true);
    printInputControl(outNodes, outTrans, nodeToName, "irq", "irq", true);
    
    for (int i = 0; i != 8; ++i)
      printInputControl(outNodes, outTrans, nodeToName, "data-" + i, "data-" + i + "-in", false);
    
    outNodes.println("// RNW DATA CONTROL");
    for (int i = 0; i != 8; ++i)
      outNodes.println("data-" + i + "-in");
    outNodes.println();
    
    outTrans.println("// RNW DATA CONTROL");
    for (int i = 0; i != 8; ++i)
      outTrans.println("rw, data-" + i + "-in, db" + i);
    outTrans.println();
    
    printOutputControl(outNodes, outTrans, nodeToName, "sync", "sync", true);
    printOutputControl(outNodes, outTrans, nodeToName, "rnw", "rw", true);
    outNodes.println("// OUTPUT: ADDRESS");
    outTrans.println("// OUTPUT: ADDRESS");
    for (int i = 0; i != 16; ++i)
      printOutputControl(outNodes, outTrans, nodeToName, "address-" + i, "ab" + i, false);
    outNodes.println();
    outTrans.println();
    outNodes.println("// OUTPUT: DATA");
    outTrans.println("// OUTPUT: DATA");
    for (int i = 0; i != 8; ++i)
      printOutputControl(outNodes, outTrans, nodeToName, "data-" + i, "db" + i, false);
    outNodes.println();
    outTrans.println();
    
    outNodes.println("// WEAK POWER SIGNALS");
    outNodes.println("vss-weak");
    outNodes.println("vcc-weak");
    outNodes.println();
    
    outTrans.println("// WEAK TRANSISTORS - SPECIAL BUS");
    for (int i = 0; i != 8; ++i)
      outTrans.println("cclk, vcc-weak, sb" + i);
    outTrans.println();
    
    outTrans.println("// WEAK TRANSISTORS - ADDRESS HIGH BUS");
    for (int i = 0; i != 8; ++i)
      outTrans.println("cclk, vcc-weak, adh" + i);
    outTrans.println();
  }
}
