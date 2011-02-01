package other;

import general.Streams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {
  
  // replace with your working dir
  public static String DIR = ".";
  
  public static final int GND = 558;
  public static final int VCC = 657;
  
  public static final int CLK = 1171;
  
  public static final int RES = 159;
  public static final int RDY = 89;
  public static final int NMI = 1297;
  public static final int IRQ = 103;
  public static final int SO = 1672;
  public static final int RW = 1156;
  
  private static final int[] BUS_ADDRESS = new int[] {268, 451, 1340, 211, 435, 736, 887, 1493,
    230, 148, 1443, 399, 1237, 349, 672, 195};
  private static final int[] BUS_DATA = new int[] {1005, 82, 945, 650, 1393, 175, 1591, 1349};
  
  public static int getBusAddressBit(int i) {
    return BUS_ADDRESS[i];
  }
  
  public static int getBusDataBit(int i) {
    return BUS_DATA[i];
  }
  
  public static int[][] readTransistors() throws IOException {
    ArrayList<int[]> result = new ArrayList<int[]>();
    Matcher matcher =
        Pattern.compile("\\[[^,]+, (\\d+), (\\d+), (\\d+)\\]").matcher(
            Streams.readText(DIR + "/transdefs.js"));
    while (matcher.find()) {
      int[] transistor = new int[3];
      for (int i = 0; i != transistor.length; ++i)
        transistor[i] = Integer.parseInt(matcher.group(1 + i));
      
      result.add(transistor);
    }
    
    return result.toArray(new int[][] {});
  }
  
  public static int[][] readSegments() throws IOException {
    ArrayList<int[]> result = new ArrayList<int[]>();
    Matcher matcher =
        Pattern.compile("\\[ *(\\d+),'([\\+\\-])',(\\d+)((?:,\\d+,\\d+)+)\\]").matcher(
            Streams.readText(DIR + "/segdefs.js"));
    while (matcher.find()) {
      String[] coords = matcher.group(4).split(",");
      int[] item = new int[2 + coords.length];
      item[0] = Integer.parseInt(matcher.group(1));
      item[1] = matcher.group(2).charAt(0) == '+' ? 1 : 0;
      item[2] = Integer.parseInt(matcher.group(1));
      
      for (int i = 0; i != coords.length - 1; ++i)
        item[3 + i] = Integer.parseInt(coords[1 + i]);
      
      result.add(item);
    }
    
    return result.toArray(new int[][] {});
  }
  
  public static TreeMap<String, Integer> readNames() throws IOException {
    TreeMap<String, Integer> result = new TreeMap<String, Integer>();
    Matcher matcher =
        Pattern.compile("(\\w+): *(\\d+)").matcher(Streams.readText(DIR + "/nodenames.js"));
    while (matcher.find())
      result.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
    
    return result;
  }
  
  public static TreeMap<Integer, String> reverseNames(TreeMap<String, Integer> map) {
    TreeMap<Integer, String> result = new TreeMap<Integer, String>();
    for (Entry<String, Integer> entry : map.entrySet())
      result.put(entry.getValue(), entry.getKey());
    
    return result;
  }
  
  public static TreeSet<Integer> getNodes(int[][] segments) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    for (int[] segment : segments)
      result.add(segment[0]);
    
    return result;
  }
  
  public static TreeSet<Integer> getPullups(int[][] segments) {
    TreeMap<Integer, Boolean> map = new TreeMap<Integer, Boolean>();
    for (int[] segment : segments) {
      int node = segment[0];
      boolean pullup = segment[1] != 0;
      Boolean old = map.put(node, pullup);
      if (old != null && old != pullup)
        throw new RuntimeException("inconsistent data");
    }
    
    TreeSet<Integer> result = new TreeSet<Integer>();
    for (Entry<Integer, Boolean> entry : map.entrySet())
      if (entry.getValue())
        result.add(entry.getKey());
    
    return result;
  }
  
}
