package other;

import general.collections.Pair;
import general.collections.Sets;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

public class Simulator2 {
  
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
  
  enum State {
    X, S0, S1;
    
    public String toString() {
      return name();
    }
  };
  
  static TreeMap<String, State> states;
  static TreeMap<String, Pair<Integer, State>> clusters;
  
  public static TreeSet<String> getHull(String node, State switchState, boolean allowed) {
    TreeSet<String> result = new TreeSet<String>();
    
    TreeSet<String> queue = new TreeSet<String>();
    queue.add(node);
    while (!queue.isEmpty()) {
      node = queue.pollFirst();
      if (result.add(node) && !(node.startsWith("vss") || node.startsWith("vcc"))) {
        TreeMap<String, TreeSet<String>> map = Tools2.trigraphGet(switchesCPC, node);
        for (Entry<String, TreeSet<String>> entry : map.entrySet())
          if ((states.get(entry.getKey()) == switchState) == allowed)
            queue.addAll(entry.getValue());
      }
    }
    
    return result;
  }
  
  public static void evaluateCluster(String node) {
    TreeSet<String> cluster = getHull(node, State.S1, true);
    
    int strength = -1;
    State state = State.X;
    for (String a : cluster) {
      int str = strengths.get(a);
      State s = states.get(a);
      
      if (str > strength) {
        strength = str;
        state = s;
      } else if (str == strength && s != state)
        state = State.X;
    }
    
    Pair<Integer, State> result = new Pair<Integer, State>(strength, state);
    for (String a : cluster)
      if (!(a.startsWith("vss") || a.startsWith("vcc")))
        clusters.put(a, result);
  }
  
  public static void setState(String node, State state) {
    State oldState = states.get(node);
    if (oldState == null || (oldState == State.X) == (state == State.X))
      throw new RuntimeException("assertion failed for node " + node + ": (" + oldState
          + " == State.X ) != ( " + state + " == State.X )");
    
    states.put(node, state);
    evaluateCluster(node);
    
    for (Entry<String, TreeSet<String>> entry : Tools2.trigraphGet(switchesPCC, node).entrySet()) {
      String a = entry.getKey();
      for (String b : entry.getValue())
        if (a.compareTo(b) < 0) {
          evaluateCluster(a);
          if (state != State.S1)
            evaluateCluster(b);
        }
    }
  }
  
  public static State evaluateNode(String node) {
    if (node.startsWith("vss"))
      return State.S0;
    
    if (node.startsWith("vcc"))
      return State.S1;
    
    TreeMap<String, Integer> strengthMap = new TreeMap<String, Integer>();
    
    Pair<Integer, State> initVal = clusters.get(node);
    State initState = initVal.getB();
    if (initState == State.X)
      return State.X;
    
    TreeSet<Pair<String, Integer>> queue = new TreeSet<Pair<String, Integer>>();
    queue.add(new Pair<String, Integer>(node, initVal.getA()));
    
    while (!queue.isEmpty()) {
      Pair<String, Integer> item = queue.pollFirst();
      Integer str = strengthMap.get(item.getA());
      if (str == null || item.getB() < str) {
        strengthMap.put(item.getA(), item.getB());
        
        int newStr = item.getB();
        
        Pair<Integer, State> val = clusters.get(item.getA());
        if (val.getA() >= newStr) {
          if (val.getB() != initVal.getB())
            return State.X;
          
          newStr = val.getA();
        }
        
        if (!(item.getA().startsWith("vss") || item.getA().startsWith("vcc"))) {
          TreeMap<String, TreeSet<String>> map = Tools2.trigraphGet(switchesCPC, item.getA());
          for (Entry<String, TreeSet<String>> entry : map.entrySet())
            if (states.get(entry.getKey()) != State.S0)
              for (String x : entry.getValue())
                queue.add(new Pair<String, Integer>(x, newStr));
        }
      }
    }
    
    return initVal.getB();
  }
  
  public static void printNodeStatistics() {
    TreeMap<State, Integer> map = new TreeMap<State, Integer>();
    for (State state : State.values())
      map.put(state, 0);
    
    for (State state : states.values())
      map.put(state, map.get(state) + 1);
    
    System.err.println(map);
  }
  
  public static void printNodeState(String node) {
    System.err.println("state of " + node + ": " + states.get(node));
  }
  
  public static void printGood() {
    TreeSet<String> good = new TreeSet<String>();
    for (String node : nodes)
      if (states.get(node) != State.X && !node.matches("\\d+"))
        good.add(node);
    System.err.println(good);
  }
  
  public static void computeCoinductively() {
    for (int c = 0;; ++c) {
      //System.err.println("coinductive run " + c + "...");
      
      boolean change = false;
      
      for (Entry<String, State> entry : states.entrySet()) {
        State state = entry.getValue();
        if (state != State.X) {
          String node = entry.getKey();
          State newState = evaluateNode(node);
          if (newState != state) {
            setState(node, newState);
            change = true;
          }
        }
      }
      
      //printNodeStatistics();
      
      if (!change) {
        //System.err.println("number of coinductive runs: " + c);
        break;
      }
    }
  }
  
  public static void computeInductively() {
    for (int c = 0;; ++c) {
      //System.err.println("inductive run " + c + "...");
      
      boolean change = false;
      
      for (Entry<String, State> entry: states.entrySet()) {
        State state = entry.getValue();
        if (state == State.X) {
          String node = entry.getKey();
          State newState = evaluateNode(node);
          if (newState != State.X) {
            setState(node, newState);
            change = true;
          }
        }
      }
      
      //printNodeStatistics();
      
      if (!change) {
        //System.err.println("number of inductive runs: " + c);
        break;
      }
    }
  }
  
  public static void printRegister8(String prefix) {
    System.err.print(prefix + ": ");
    for (int i = 8; i != 0;)
      System.err.print(states.get(prefix + --i).toString().replace("S", ""));
    System.err.println();
  }
  
  public static void printRegister16(String prefix) {
    System.err.print(prefix + ": ");
    if (nodes.contains(prefix + "h0")) {
      for (int i = 8; i != 0;)
        System.err.print(states.get(prefix + "h" + --i).toString().replace("S", ""));
      for (int i = 8; i != 0;)
        System.err.print(states.get(prefix + "l" + --i).toString().replace("S", ""));
    } else
      for (int i = 16; i != 0;)
        System.err.print(states.get(prefix + --i).toString().replace("S", ""));
    System.err.println();
  }
  
  static boolean log = false;
  
  public static void printHelper() {
    printRegister8("a");
    printRegister8("x");
    printRegister8("y");
    printRegister8("s");
    
    printRegister8("ir");
    printRegister8("notir");
    
    printRegister16("pc");
    printRegister16("ad");
    printRegister8("idb");
    printRegister8("sb");
    
    printRegister8("dor");
    printRegister8("db");
    printRegister16("ab");
    
    // data path controls
    boolean unknowns = false;
    for (String node : nodes)
      if (node.startsWith("dpc")) {
        State state = states.get(node);
        if (state == State.S1)
          System.err.println(node);
        else if (state == State.X)
          unknowns = true;
      }
    
    if (unknowns)
      System.err.println("there are unknowns");
    
    printNodeState("input-data-0");
    printNodeState("data-0-in-inv");
    printNodeState("data-0-in");
    printNodeState("rw");
    printNodeState("db0");
    printNodeState("1072");
    printNodeState("1325");
  }
  
  public static void doCycle(TreeMap<String, State> inputValues) {
    for (String node : inputs) {
      State state = inputValues.get(node);
      if (state == null || !(state == State.S0 || state == State.S1))
        throw new RuntimeException("assertion failed: inputValues.get(" + node + ") invalid: "
            + state);
    }
    
    // inputs go to value
    if (log)
      System.err.println("=== VALUES GO KNOWN ===");
    for (String node : inputs)
      setState(node, inputValues.get(node));
    computeInductively();
    if (log)
      printHelper();
    
    // phi2 goes low
    //if (log)
    //  System.err.println("=== PHI2 GOES UNKNOWN ===");
    setState("input-phi-2", State.X);
    computeCoinductively();
    //if (log)
    //  printHelper();
    
    if (log)
      System.err.println("=== PHI2 GOES LOW ===");
    setState("input-phi-2", State.S0);
    computeInductively();
    //if (log)
    //  printHelper();
    
    // inputs go back to undefined
    //if (log)
    //  System.err.println("=== VALUES GO UNKNOWN===");
    for (String node : inputs)
      setState(node, State.X);
    computeCoinductively();
    //if (log)
    //  printHelper();
    
    // phi1 goes high
    //if (log)
    //  System.err.println("=== PHI1 GOES UNKNOWN===");
    setState("input-phi-1", State.X);
    computeCoinductively();
    //if (log)
    //  printHelper();
    
    if (log)
      System.err.println("=== PHI1 GOES HIGH ===");
    setState("input-phi-1", State.S1);
    computeInductively();
    if (log)
      printHelper();
    
    // phi1 goes low
    //if (log)
    //  System.err.println("=== PHI1 GOES UNKNOWN ===");
    setState("input-phi-1", State.X);
    computeCoinductively();
    //if (log)
    //  printHelper();
    
    if (log)
      System.err.println("=== PHI1 GOES LOW ===");
    setState("input-phi-1", State.S0);
    computeInductively();
    //if (log)
    //  printHelper();
    
    // phi2 goes high
    //if (log)
    //  System.err.println("=== PHI2 GOES UNKNOWN ===");
    setState("input-phi-2", State.X);
    computeCoinductively();
    //if (log)
    //  printHelper();
    
    if (log)
      System.err.println("=== PHI2 GOES HIGH ===");
    setState("input-phi-2", State.S1);
    computeInductively();
    if (log)
      printHelper();
  }
  
  public static TreeMap<String, State> getOutputValues() {
    TreeMap<String, State> result = new TreeMap<String, State>();
    for (String node : outputs)
      result.put(node, states.get(node));
    
    return result;
  }
  
  public static int getValue(String prefix, int numBits) {
    int result = 0;
    for (int i = 0; i != numBits; ++i) {
      State state = states.get(prefix + i);
      if (state == State.X)
        return -1;
      
      result |= (1 << i) * (state == State.S0 ? 0 : 1);
    }
    
    return result;
  }
  
  public static int getAddress() {
    return getValue("output-address-", 16);
  }
  
  public static int getData() {
    return getValue("output-data-", 8);
  }
  
  public static void setData(TreeMap<String, State> inputValues, int data) {
    for (int i = 0; i != 8; ++i)
      inputValues.put("input-data-" + i, (data & (1 << i)) != 0 ? State.S1 : State.S0);
  }
  
  public static void init() {
    for (String node : nodes)
      if (!(node.startsWith("vss") || node.startsWith("vcc")))
        states.put(node, State.X);
    
    for (String node : nodes)
      evaluateCluster(node);
    
    setState("input-phi-1", State.S0);
    setState("input-phi-2", State.S1);
  }
  
  static int[] memory = new int[0x00010000];
  
  public static void main(String[] args) {
    clusters = new TreeMap<String, Pair<Integer, State>>();
    states = new TreeMap<String, State>();
    
    for (String node : nodes)
      if (node.startsWith("vss")) {
        clusters.put(node, new Pair<Integer, State>(strengths.get(node), State.S0));
        states.put(node, State.S0);
      } else if (node.startsWith("vcc")) {
        clusters.put(node, new Pair<Integer, State>(strengths.get(node), State.S1));
        states.put(node, State.S1);
      }
    
    TreeMap<String, State> inputValues = new TreeMap<String, State>();
    
    TreeMap<String, State>[] result = new TreeMap[2];
    
    outer: for (int j = 0; j != 1; ++j) {
      init();
      
      int[] memStart =
          {0xa9, 0xff, 0xa2, 0xff, 0xa0, 0x00, 0x8b, 0xff, 0xea, 0xea};
      for (int i = 0; i != memStart.length; ++i)
        memory[i] = memStart[i];
      
      for (String node : inputs)
        inputValues.put(node, State.S0);
      inputValues.put("input-res", State.S0);
      inputValues.put("input-ready", State.S1);
      
      for (int i = 0; i != 16; ++i)
        doCycle(inputValues);
      
      System.err.println("****** DISABLING RESET ******");
      inputValues.put("input-res", State.S1);
      
      for (int i = 0; i != 7; ++i)
        doCycle(inputValues);
      
      long a = System.currentTimeMillis();
      
      int c = 0;
      for (c = 0;; ++c) {
        //log = true;
        
        TreeMap<String, State> outputValues = getOutputValues();
        State reading = outputValues.get("output-rnw");
        if (reading == State.X)
          throw new RuntimeException("error cycle");
        
        printRegister16("pc");
        printRegister8("a");
        printRegister8("x");
        printNodeState("p6");
        
        System.err.print(reading == State.S1 ? "reading " : "writing ");
        int address = getAddress();
        if (address == -1) {
          System.err.println("address invalid");
          printRegister16("output-address-");
          throw new RuntimeException();
        } else {
          System.err.printf("address %04x: ", address);
          
          if (reading == State.S1) {
            int data = memory[address];
            System.err.printf("%02x\n", data);
            setData(inputValues, data);
          } else {
            int data = getData();
            System.err.printf("%02x\n", data);
            memory[address] = data;
          }
        }
        
        if (getValue("pcl", 8) == 8)
          break;
        
        doCycle(inputValues);
      }
      
      long b = System.currentTimeMillis();
      
      System.err.println((b - a) / c);
    }
  }
}
