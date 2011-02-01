package test;

import general.Statistics;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import data.Tools;

import tools.PointChooser;

/**
int u = (x + (y + 1)) / 2;
int v = (sy - (y + 1) + x) / 2;

inverse transform:
2 * u = x + y + 1
2 * v = sy - 1 - y + x

x = u + v - sy / 2
y = u - v + sy / 2 - 1
*/
public class LightDistribution {

  public static void main(String[] args) throws Exception {
    BufferedImage source = ImageIO.read(new File(
        "/home/noname/decapsulation/image-processing/dist0.png"));
    WritableRaster sourceRaster = source.getRaster();

    BufferedImage image = Tools.getGreenComponent(source, 32);
    WritableRaster raster = image.getRaster();

    int sx = source.getWidth();
    int sy = source.getHeight();

    final ArrayList<int[]>[] smoothed = (ArrayList<int[]>[]) new ArrayList[1000];

    for (int y = 0; y != sy; ++y) {
      if (y % 10 == 0)
        System.out.println(y);

      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 1) {
          int u = (x + (y + 1)) / 2;
          int v = (sy - (y + 1) + x) / 2;

          TreeMap<Integer, Integer> values = new TreeMap<Integer, Integer>();
          int num = 0;
          int sum = 0;

          for (int vv = -1; vv <= 1; ++vv)
            for (int uu = -1; uu <= 1; ++uu)
              if (uu * uu + vv * vv <= 200) {
                int ru = u + uu;
                int rv = v + vv;
                int rx = ru + rv - sy / 2;
                int ry = ru - rv + sy / 2 - 1;
                if (rx >= 0 && ry >= 0 && rx < sx && ry < sy) {
                  int val = sourceRaster.getPixel(rx, ry, new int[3])[1];
                  if (!values.containsKey(val))
                    values.put(val, 0);
                  values.put(val, values.get(val) + 1);
                  num += 1;
                  sum += val;
                }
              }

          int limit = num / 4;
          for (int i = 0; i <= limit; ++i) {
            int a = values.firstKey();
            int b = values.lastKey();
            int val = Math.abs(num * a - sum) > Math.abs(num * b - sum) ? a : b;

            values.put(val, values.get(val) - 1);
            if (values.get(val) == 0)
              values.remove(val);
            num -= 1;
            sum -= val;
          }

          int val = sum / num;
          if (smoothed[val] == null)
            smoothed[val] = new ArrayList<int[]>();
          smoothed[val].add(new int[] {u, v});
        }
    }

    Tools.display(new PointChooser(image, new PointChooser.Handler() {
      int bound = 600;
      int scale = 0;
      boolean scaleSet = false;

      public void draw(Graphics g, double zoom) {
        g.setColor(Color.RED);
        for (int i = bound; i < Math.max(smoothed.length, bound + 5); ++i)
          if (smoothed[i] != null)
            for (int[] p : smoothed[i])
              g.drawLine(p[0], p[1], p[0], p[1]);
      }

      public boolean click(int button, int x, int y) {
        boolean ret;

        if (scaleSet) {
          if (button == 1)
            ++scale;
          else if (button == 3)
            --scale;
          else if (button == 2)
            scaleSet = false;
          ret = false;
        } else if (button == 1) {
          bound += scale;
          if (bound < 0)
            bound = 0;
          if (bound >= smoothed.length)
            bound = smoothed.length - 1;
          ret = true;
        } else if (button == 3) {
          bound -= scale;
          if (bound < 0)
            bound = 0;
          if (bound >= smoothed.length)
            bound = smoothed.length - 1;
          ret = true;
        } else if (button == 2) {
          scaleSet = true;
          ret = false;
        } else
          ret = false;

        if (scaleSet)
          System.out.println("scale " + scale);
        else
          System.out.println("bound " + bound);

        return ret;
      }
    }));

    //Statistics.printMap(hist);
    //Statistics.printMap(smoothedHist);
  }
}
