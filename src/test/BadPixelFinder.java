package test;

import general.collections.Pair;
import general.Statistics;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class BadPixelFinder {

  public static TreeSet<Pair<Integer, Integer>> findBadPixels(String step) throws IOException {
    double[][] image = Tools.getMatrixFromImage(ImageIO.read(new File(DataTools.DIR + "images/" + step
        + ".png")));
    TreeSet<Pair<Integer, Integer>> results = new TreeSet<Pair<Integer, Integer>>();

    int sx = image[0].length;
    int sy = image.length;

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 0)
          if (image[y][x] >= 700)
            results.add(new Pair<Integer, Integer>(x, y));

    return results;
  }

  public static void main(String[] args) throws IOException {
    TreeMap<Pair<Integer, Integer>, Integer> map = new TreeMap<Pair<Integer, Integer>, Integer>();
    for (int y = 0; y != 7; ++y)
      for (int x = 0; x != 5; ++x) {
        System.out.println(x);
        for (Pair<Integer, Integer> p : findBadPixels(x + "-" + y)) {
          if (!map.containsKey(p))
            map.put(p, 0);
          map.put(p, map.get(p) + 1);
        }
      }

    for (Pair<Integer, Integer> p : map.keySet())
      if (map.get(p) > 4)
        System.out.println(p + ": " + map.get(p));
  }

}
