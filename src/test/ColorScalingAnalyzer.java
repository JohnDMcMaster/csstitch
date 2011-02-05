package test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.Format;
import java.util.TreeMap;
import java.util.TreeSet;

import data.DataTools;

public class ColorScalingAnalyzer {

  public static class Entry {
    double factor, offset, fix;
    int numPoints;
  }

  public static class Picture {
    Entry[][] entriesX = new Entry[4][4];
    Entry[][] entriesY = new Entry[4][4];
  }

  public static TreeMap<String, Picture> read(String filename) throws IOException {
    TreeMap<String, Picture> result = new TreeMap<String, Picture>();

    BufferedReader in = new BufferedReader(new FileReader(filename));
    String line = in.readLine();
    while (line != null) {
      Picture picture = new Picture();
      result.put(line.substring(line.lastIndexOf('/') + 1, line.indexOf(".PNG")), picture);
      line = in.readLine();

      while (line != null && !line.contains("PNG")) {
        int a = line.charAt(0) - '0';
        int b = line.charAt(2) - '0';
        line = in.readLine();

        Entry[] entries = {new Entry(), new Entry()};
        picture.entriesX[a][b] = entries[0];
        picture.entriesY[a][b] = entries[1];

        for (int i = 0; i != 2; ++i) {
          String factor = line.substring(line.indexOf(": ") + 2, line.indexOf(", "));
          String offset = line.substring(line.indexOf(", ") + 2, line.indexOf("; "));
          String fix = line.substring(line.lastIndexOf(": ") + 2, line.lastIndexOf("; "));
          String numPoints = line
              .substring(line.lastIndexOf("; ") + 2, line.lastIndexOf(" points"));

          entries[i].factor = Double.parseDouble(factor);
          entries[i].offset = Double.parseDouble(offset);
          entries[i].fix = Double.parseDouble(fix);
          entries[i].numPoints = Integer.parseInt(numPoints);

          line = in.readLine();
        }
      }
    }

    return result;
  }

  public static void main(String[] args) throws IOException {
    TreeMap<String, Picture> results = read(DataTools.DIR + "report_aberr.txt");
    //System.out.println("number of pictures evaluated: " + results.size());

    TreeMap<String, String>[] tags = Tagger.load(DataTools.DIR + "tags.txt");
    TreeSet<String> completeScans = new TreeSet<String>();
    for (int i = 0; i != tags.length; ++i) {
      String lense = tags[i].get("lense");
      String description = tags[i].get("description-group");
      if (lense != null && lense.equals("20x") && description != null
          && description.startsWith("scan (complete"))
        completeScans.add(tags[i].get("name"));
    }
    //System.out.println("number of complete scan pictures: " + completeScans.size());

    results.keySet().retainAll(completeScans);
    //System.out.println("number of complete scan pictures evaluated: " + results.size());

    for (int a = 0; a != 4; ++a)
      for (int b = 0; b != 4; ++b)
        for (int i = 0; i != 2; ++i) {
          double meanFactor = 0, meanFix = 0;
          TreeSet<Double> factors = new TreeSet<Double>(), fixs = new TreeSet<Double>();
          for (String name : results.keySet()) {
            Entry entry;
            if (i == 0)
              entry = results.get(name).entriesX[a][b];
            else
              entry = results.get(name).entriesY[a][b];

            if (entry != null) {
              factors.add(entry.factor);
              meanFactor += entry.factor;

              fixs.add(entry.fix);
              meanFix += entry.fix;
            }
          }

          if (factors.size() != 0) {
            meanFactor /= factors.size();
            meanFix /= fixs.size();

            System.out.println(a + "-" + b + ", " + (i == 0 ? 'X' : 'Y') + ":");
            System.out.println("  factor: " + meanFactor + ", "
                + factors.toArray(new Double[] {})[factors.size() / 2]);
            System.out.println("  fix: " + meanFix + ", "
                + fixs.toArray(new Double[] {})[fixs.size() / 2]);
          }
        }
  }

}
