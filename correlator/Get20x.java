package correlator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import data.DataTools;
import test.Tagger;

public class Get20x {

  public static final Integer[] NUMS = {21, 22, 23};
  public static TreeSet<Integer> NUM_SET = new TreeSet<Integer>(Arrays.asList(NUMS));
  public static final String OUT_DIR = DataTools.DIR + "corr-list-" + numsToString() + "/";

  public static String numsToString() {
    String res = "";
    for (int i : NUMS)
      res += String.format("%02d-", i);
    if (NUMS.length != 0)
      res = res.substring(0, res.length() - 1);
    return res;
  }

  public static void main(String[] args) throws IOException {
    TreeMap<String, String>[] tags = Tagger.load(DataTools.DIR + "tags.txt");

    new File(OUT_DIR.substring(0, OUT_DIR.length() - 1)).mkdir();
    PrintStream out = new PrintStream(OUT_DIR + "list.txt");

    boolean run = false;
    int runNumber = 0;
    for (TreeMap<String, String> entry : tags) {
      if ("20x".equals(entry.get("lense"))) {
        if (!run) {
          System.out.println("number " + runNumber);
          System.out.println(entry.get("chip-model") + ", " + entry.get("chip-identifier"));
          System.out.println(entry.get("description-group"));
          System.out.println(entry.get("description"));
        }

        System.out.println(entry.get("name"));
        run = true;
      } else {
        if (run) {
          ++runNumber;
          System.out.println();
        }

        run = false;
      }

      if ("20x".equals(entry.get("lense")) && NUM_SET.contains(runNumber))
        out.println(entry.get("name"));
    }

    out.close();
  }
}
