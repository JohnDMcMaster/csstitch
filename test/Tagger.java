package test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

public class Tagger {

  public static String[] readNames() throws IOException {
    ArrayList<String> names = new ArrayList<String>();
    BufferedReader in = new BufferedReader(new FileReader("/home/noname/di/names"));
    String line;
    while ((line = in.readLine()) != null)
      names.add(line.substring(0, line.length()));
    in.close();
    return names.toArray(new String[] {});
  }

  public static int getIndex(String[] array, String key) {
    for (int i = 0; i != array.length; ++i)
      if (array[i].equals(key))
        return i;

    return -1;
  }

  public static void save(TreeMap<String, String>[] tags, String filename) throws IOException {
    BufferedWriter out = new BufferedWriter(new FileWriter(filename));
    for (int i = 0; i != tags.length; ++i) {
      for (String key : tags[i].keySet())
        out.write(key + ": " + tags[i].get(key) + "\n");
      out.write("\n");
    }
    out.close();
  }

  public static TreeMap<String, String>[] load(String[] names, String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    TreeMap<String, String>[] tags = new TreeMap[names.length];
    for (int i = 0; i != names.length; ++i) {
      tags[i] = new TreeMap<String, String>();
      tags[i].put("name", names[i]);
    }

    String line;
    while ((line = in.readLine()) != null) {
      TreeMap<String, String> map = new TreeMap<String, String>();
      while (line.length() != 0) {
        int ind = line.indexOf(": ");
        map.put(line.substring(0, ind), line.substring(ind + 2));
        line = in.readLine();
      }

      int ind = map.containsKey("name") ? getIndex(names, map.get("name")) : -1;
      if (ind == -1)
        throw new IOException("unknown name: " + map.get("name"));
      tags[ind] = map;
    }
    in.close();
    return tags;
  }
  
  public static TreeMap<String, String>[] load(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    ArrayList<TreeMap<String, String>> tags = new ArrayList<TreeMap<String,String>>();
    
    String line;
    while ((line = in.readLine()) != null) {
      TreeMap<String, String> map = new TreeMap<String, String>();
      while (line.length() != 0) {
        int ind = line.indexOf(": ");
        map.put(line.substring(0, ind), line.substring(ind + 2));
        line = in.readLine();
      }
      tags.add(map);
    }
    in.close();
    
    return tags.toArray(new TreeMap[] {});
  }

  public static void main(String[] args) throws IOException {
    String[] names = readNames();
    /*TreeMap<String, String>[] tags = new TreeMap[names.length];
    for (int i = 0; i != names.length; ++i) {
      tags[i] = new TreeMap<String, String>();
      tags[i].put("name", names[i]);
    }*/

    TreeMap<String, String>[] tags = load(names, "/home/noname/di/tags.txt");

    int raws = 0;
    int lense20 = 0;
    for (int i = 0; i != tags.length; ++i) {
      if ("ORF".equals(tags[i].get("format"))) {
        ++raws;
        if ("20x".equals(tags[i].get("lense")))
          ++lense20;
      }
    }

    System.out.println("raws: " + raws);
    System.out.println("raws with lense 20x: " + lense20);
    
    TreeSet<String> t = new TreeSet<String>();
    for (int i = 0; i != names.length; ++i)
      t.addAll(tags[i].keySet());
    System.out.println(t);

    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      int i = -1, j = -1;

      String line = in.readLine();
      if (line.equals("exit"))
        break;

      if (line.contains("-")) {
        String[] comp = line.split(" *- *");
        if (comp.length >= 2) {
          i = getIndex(names, comp[0]);
          j = getIndex(names, comp[1]);
        }
      } else {
        i = getIndex(names, line);
        j = i;
      }

      if (i < 0 || j < 0 || i > j) {
        System.out.println("invalid input");
        continue;
      }

      String tag = in.readLine();
      String val = in.readLine();

      if (val.equals("delete")) {
        for (int k = i; k <= j; ++k) {
          if (!tags[k].containsKey(tag))
            System.out.println("warning: tag value does not exist in image " + names[k]);
          tags[k].remove(tag);
        }
      } else
        for (int k = i; k <= j; ++k) {
          if (tags[k].containsKey(tag))
            System.out.println("warning: pre-existing tag value " + tags[k].get(tag) + " of image "
                + names[k] + " overwritten");
          tags[k].put(tag, val);
        }
    }

    save(tags, "/home/noname/di/tags-out.txt");
  }

}
