package other;

import general.collections.Maps;
import general.collections.Pair;
import general.collections.Sets;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

public class Simulator {
  
  private TreeMap<Integer, Integer> nodeLookup;
  private int[] nodes;
  
  private TreeMap<String, Integer> nameLookup;
  private String[] names;
  
  private boolean[] pullup;
  
  private int gnd, vcc;
  private int clk;
  
  private int res, rdy, nmi, irq, so, rw;
  private int[] busAddress, busData;
  
  private TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByChildren;
  private TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByParent;
  private TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>> switchesByChildParent;
  
  // 0: X (unknown), 1: GND, 2: VCC
  private TreeMap<Integer, Integer> inputValues;
  
  private int[] nodeValues;
  private int[] nodeValuesOld;
  
  private int[] memory;
  
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
  
  public Simulator() throws IOException {
    int[][] segments = Tools.readSegments();
    int[][] transistors = Tools.readTransistors();
    
    TreeMap<String, Integer> nameLookup = Tools.readNames();
    TreeMap<Integer, String> names = Maps.invert(nameLookup);
    
    TreeSet<Integer> nodeSet = Tools.getNodes(segments);
    TreeSet<Integer> pullups = Tools.getPullups(segments);
    
    nodeSet.remove(806); // has a pull-down 
    
    // 9 ==(866)== VCC, 9 and 866 otherwise unconnected
    nodeSet.remove(866);
    nodeSet.remove(9);
    
    Integer[] nodeArray = nodeSet.toArray(new Integer[] {});
    nodes = new int[nodeArray.length];
    for (int i = 0; i != nodes.length; ++i)
      nodes[i] = nodeArray[i];
    
    nodeLookup = new TreeMap<Integer, Integer>();
    for (int i = 0; i != nodes.length; ++i)
      nodeLookup.put(nodes[i], i);
    
    this.names = new String[nodes.length];
    for (int i = 0; i != nodes.length; ++i)
      this.names[i] = names.get(nodes[i]);
    
    this.nameLookup = new TreeMap<String, Integer>();
    for (int i = 0; i != nodes.length; ++i)
      if (this.names[i] != null)
        this.nameLookup.put(this.names[i], i);
    
    pullup = new boolean[nodes.length];
    for (int i = 0; i != nodes.length; ++i)
      pullup[i] = pullups.contains(nodes[i]);
    
    gnd = nodeLookup.get(Tools.GND);
    vcc = nodeLookup.get(Tools.VCC);
    
    clk = nodeLookup.get(Tools.CLK);
    
    res = nodeLookup.get(Tools.RES);
    rdy = nodeLookup.get(Tools.RDY);
    nmi = nodeLookup.get(Tools.NMI);
    irq = nodeLookup.get(Tools.IRQ);
    so = nodeLookup.get(Tools.SO);
    rw = nodeLookup.get(Tools.RW);
    
    busAddress = new int[16];
    for (int i = 0; i != busAddress.length; ++i)
      busAddress[i] = nodeLookup.get(Tools.getBusAddressBit(i));
    
    busData = new int[8];
    for (int i = 0; i != busData.length; ++i)
      busData[i] = nodeLookup.get(Tools.getBusDataBit(i));
    
    switchesByParent = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
    switchesByChildParent = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
    switchesByChildren = new TreeMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
    
    outer: for (int[] t : transistors) {
      Integer[] s = new Integer[3];
      for (int i = 0; i != 3; ++i) {
        s[i] = nodeLookup.get(t[i]);
        if (s[i] == null)
          continue outer;
      }
      
      if (s[0] != gnd && s[1] != s[2]) {
        for (int i = 1; i != 3; ++i) {
          trigraphAdd(switchesByParent, s[0], s[i], s[3 - i]);
          trigraphAdd(switchesByChildParent, s[3 - i], s[0], s[i]);
          trigraphAdd(switchesByChildren, s[i], s[3 - i], s[0]);
        }
      }
    }
    
    inputValues = new TreeMap<Integer, Integer>();
    inputValues.put(gnd, 1);
    inputValues.put(vcc, 2);
    
    nodeValues = new int[nodes.length];
    nodeValuesOld = new int[nodes.length];
    
    memory = new int[1 << 16];
    
    int[] program =
        new int[] {0xa9, 0x00, 0x20, 0x10, 0x00, 0x4c, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0xe8, 0x88, 0xe6, 0x40, 0x38, 0x69, 0x02, 0x60};
    
    for (int i = 0; i != program.length; ++i)
      memory[i] = program[i];
  }
  
  private int readMemory(int address) {
    return memory[address];
  }
  
  private void writeMemory(int address, int value) {
    memory[address] = value;
  }
  
  private TreeSet<Integer> findStrictComponent(int n) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    
    Stack<Integer> queue = new Stack<Integer>();
    queue.push(n);
    
    while (!queue.isEmpty()) {
      n = queue.pop();
      if (result.add(n)) {
        if (inputValues.containsKey(n))
          continue;
        
        TreeMap<Integer, TreeSet<Integer>> map = switchesByChildren.get(n);
        if (map != null)
          for (Entry<Integer, TreeSet<Integer>> entry : map.entrySet())
            for (int parent : entry.getValue())
              if (nodeValues[parent] == 2)
                queue.add(entry.getKey());
      }
    }
    
    return result;
  }
  
  private TreeSet<Integer> findComponent(int n, TreeSet<Integer> parents) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    
    Stack<Integer> queue = new Stack<Integer>();
    queue.push(n);
    
    while (!queue.isEmpty()) {
      n = queue.pop();
      if (result.add(n)) {
        if (inputValues.containsKey(n))
          continue;
        
        TreeMap<Integer, TreeSet<Integer>> map = switchesByChildren.get(n);
        if (map != null)
          for (Entry<Integer, TreeSet<Integer>> entry : map.entrySet())
            for (int parent : entry.getValue()) {
              if (nodeValues[parent] == 0)
                if (parents != null)
                  parents.add(parent);
              
              if (nodeValues[parent] == 0 || nodeValues[parent] == 2)
                queue.add(entry.getKey());
            }
      }
    }
    
    return result;
  }
  
  private boolean setValues(TreeSet<Integer> set, int value) {
    boolean change = false;
    for (int n : set) {
      if (nodeValues[n] != value) {
        if (nodeValues[n] != 0)
          throw new RuntimeException();
        
        nodeValues[n] = value;
      }
    }
    
    return change;
  }
  
  private boolean[] getValues(TreeSet<Integer> component) {
    boolean[] values = new boolean[3];
    for (Entry<Integer, Integer> entry : inputValues.entrySet())
      if (component.contains(entry.getKey()))
        values[entry.getValue()] = true;
    
    return values;
  }
  
  private boolean hasPullup(TreeSet<Integer> component) {
    for (int n : component)
      if (pullup[n])
        return true;
    
    return false;
  }
  
  private boolean partiallyEvalComponent(TreeSet<Integer> component) {
    boolean[] values = getValues(component);
    if (values[1] && values[2])
      return false;
    
    boolean change = false;
    
    TreeSet<Integer> set = new TreeSet<Integer>(component);
    while (!set.isEmpty()) {
      TreeSet<Integer> strict = findStrictComponent(set.first());
      boolean[] innerValues = getValues(strict);
      
      for (int i = 1; i != 3; ++i)
        if (innerValues[i])
          change |= setValues(strict, i);
      
      if (!(values[1] || values[2]) && hasPullup(strict))
        change |= setValues(strict, 2);
      
      set.removeAll(strict);
    }
    
    return change;
  }
  
  private int evalComponent(TreeSet<Integer> component) {
    boolean[] values = getValues(component);
    if (values[1] && values[2])
      throw new RuntimeException();
    
    if (!(values[1] || values[2])) {
      for (int n : component)
        values[2] |= pullup[n];
      
      if (!values[2])
        for (int n : component)
          values[nodeValuesOld[n]] = true;
    }
    
    int value = -1;
    for (int i = 0; i != 3; ++i)
      if (values[i]) {
        if (value != -1)
          throw new RuntimeException();
        
        value = i;
      }
    
    setValues(component, value);
    return value;
  }
  
  private void printStatistics() {
    int[] hist = new int[3];
    for (int v : nodeValues)
      ++hist[v];
    
    System.err.println(hist[1] + " GND, " + hist[2] + " VCC, " + hist[0] + " X");
  }
  
  private TreeSet<Integer> getChildren(int n) {
    TreeMap<Integer, TreeSet<Integer>> map = switchesByParent.get(n);
    if (map == null)
      map = new TreeMap<Integer, TreeSet<Integer>>();
    
    return new TreeSet<Integer>(map.keySet());
  }
  
  private void printInfo() {
    TreeSet<Integer> printed = new TreeSet<Integer>();
    TreeSet<Pair<Integer, Pair<Integer, Integer>>> queue =
        new TreeSet<Pair<Integer, Pair<Integer, Integer>>>();
    queue.add(new Pair<Integer, Pair<Integer, Integer>>(0, new Pair<Integer, Integer>(-1, res)));
    while (!queue.isEmpty()) {
      Pair<Integer, Pair<Integer, Integer>> p = queue.pollFirst();
      int q = p.getA();
      int parent = p.getB().getA();
      int n = p.getB().getB();
      if (printed.add(n)) {
        if (parent != -1)
          System.err.print(nodes[parent] + " -> ");
        System.err.print(nodes[n]);
        if (names[n] != null)
          System.err.print(" (" + names[n] + ")");
        System.err.println(": " + nodeValues[n]);
        
        if (q < 7)
          if (n == res || !inputValues.containsKey(n)) {
            System.err.print("children of " + nodes[n] + ": ");
            for (int m : getChildren(n))
              System.err.print(nodes[m] + ", ");
            System.err.println();

            for (int m : getChildren(n))
              queue.add(new Pair<Integer, Pair<Integer, Integer>>(q + 1,
                  new Pair<Integer, Integer>(n, m)));
          }
      }
    }
  }
  
  private void calculate() {
    System.err.println("calculating...");
    
    nodeValuesOld = nodeValues;
    nodeValues = new int[nodes.length];
    
    TreeSet<Integer> queue = new TreeSet<Integer>();
    for (int i = 0; i != nodes.length; ++i)
      queue.add(i);
    
    for (;;) {
      boolean change = false;
      
      for (Iterator<Integer> it = queue.iterator(); it.hasNext();) {
        printStatistics();
        
        int n = it.next();
        if (nodeValues[n] != 0) {
          it.remove();
          continue;
        }
        
        TreeSet<Integer> parents = new TreeSet<Integer>();
        TreeSet<Integer> component = findComponent(n, parents);
        
        if (parents.isEmpty()) {
          evalComponent(component);
          if (nodeValues[n] != 0)
            change = true;
          continue;
        }
        
        change |= partiallyEvalComponent(component);
      }
      
      if (!change)
        break;
    }
    
    printInfo();
  }
  
  private int readBus(int[] pads) {
    int value = 0;
    for (int i = 0; i != pads.length; ++i) {
      int bit = nodeValues[pads[i]];
      if (bit == 0)
        throw new RuntimeException();
      
      value += (bit - 1) << i;
    }
    
    return value;
  }
  
  private void writeBus(int[] pads, int value) {
    for (int i = 0; i != pads.length; ++i)
      inputValues.put(pads[i], (value & (1 << value)) == 0 ? 1 : 2);
    
    calculate();
    
    for (int i = 0; i != pads.length; ++i)
      inputValues.remove(pads[i]);
    
    calculate();
  }
  
  private int readBusAddress() {
    return readBus(busAddress);
  }
  
  private int readBusData() {
    return readBus(busData);
  }
  
  private void writeBusData(int value) {
    writeBus(busData, value);
  }
  
  private void handleRead() {
    //if (nodeValues[rw] == 0)
    //  throw new RuntimeException();
    
    //if (nodeValues[rw] == 2)
    //  writeBusData(readMemory(readBusAddress()));
  }
  
  private void handleWrite() {
    //if (nodeValues[rw] == 0)
    //  throw new RuntimeException();
    
    //if (nodeValues[rw] == 1)
    //  writeMemory(readBusAddress(), readBusData());
  }
  
  private void halfStep() {
    if (inputValues.get(clk) == 1) {
      handleRead();
      inputValues.put(clk, 2);
    } else {
      handleWrite();
      inputValues.put(clk, 1);
    }
    
    calculate();
  }
  
  public void initialize() {
    inputValues.put(rdy, 2);
    inputValues.put(so, 1);
    inputValues.put(nmi, 2);
    inputValues.put(irq, 2);
    inputValues.put(res, 2);
    inputValues.put(clk, 2);
    calculate();
    
    for (int i = 0; i != 50; ++i)
      System.err.println();
    
    inputValues.put(clk, 1);
    calculate();

    System.exit(0);
    
    for (int i = 0; i != 32; ++i)
      halfStep();
    
    inputValues.put(res, 1);
    calculate();
  }
  
  public static void main(String[] args) throws IOException {
    Simulator s = new Simulator();
    s.initialize();
  }
  
}
