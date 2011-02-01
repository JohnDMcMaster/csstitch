package metal;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import operations.image.ImageOpsFloat;

import data.Tools;
import distributed.Bootstrap;
import distributed.server.CipServer;
import distributed.server.Servers;

public class Test2 {
  
  public static float getMean(TreeMap<Float, Integer> histogram) {
    int total = 0;
    float sum = 0;
    for (float val : histogram.keySet()) {
      int n = histogram.get(val);
      total += n;
      sum += n * val;
    }
    return sum / total;
  }
  
  public static float getDeviation(TreeMap<Float, Integer> histogram) {
    float mean = getMean(histogram);
    
    int total = 0;
    float sum = 0;
    for (float val : histogram.keySet()) {
      int n = histogram.get(val);
      total += n;
      sum += n * (val - mean) * (val - mean);
    }
    return (float) Math.sqrt(sum / total);
  }
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(new CipServer("cip78"));
    
    float[][] image = MetalTools.getImageValues(0, 23);
    image = ImageOpsFloat.log(image);
    
    int sx = image[0].length;
    int sy = image.length;
    
    TreeMap<Float, Integer> histogram = new TreeMap<Float, Integer>();
    for (int y = 1; y != sy - 1; ++y)
      for (int x = 1 + y % 2; x < sx - 1; x += 2) {
        float value =
            image[y - 1][x - 1] - image[y - 1][x + 1] - image[y + 1][x - 1] + image[y + 1][x + 1];
        Integer num = histogram.get(value);
        if (num == null)
          num = 0;
        histogram.put(value, num + 1);
      }
    
    float mean = getMean(histogram);
    System.out.println(mean);
    
    float deviation = getDeviation(histogram);
    System.out.println(deviation);
    
    for (Iterator<Float> it = histogram.keySet().iterator(); it.hasNext();) {
      float val = it.next();
      if (!(val >= mean - 3 * deviation && val <= mean + 3 * deviation))
        it.remove();
    }
    
    mean = getMean(histogram);
    System.out.println(mean);
    
    deviation = getDeviation(histogram);
    System.out.println(deviation);
    
    float stepSize = 0.001f;
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;
    for (float value : histogram.keySet()) {
      int index = (int) Math.round(value / stepSize);
      min = Math.min(min, index);
      max = Math.max(max, index);
    }
    
    float window = 0.2f;
    min = (int) (-window / stepSize);
    max = (int) (+window / stepSize);
    
    int[] blocks = new int[max - min + 1];
    for (float value : histogram.keySet()) {
      int index = (int) Math.round(value / stepSize);
      if (index >= min && index <= max)
        blocks[index - min] += histogram.get(value);
    }
    
    max = Integer.MIN_VALUE;
    for (int n : blocks)
      max = Math.max(max, n);
    
    System.out.println(blocks.length);
    System.out.println(max);
    
    int columnWidth = 3;
    int height = 800;
    
    BufferedImage out =
        new BufferedImage(columnWidth * blocks.length, height, BufferedImage.TYPE_INT_RGB);
    Graphics g = out.getGraphics();
    
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, columnWidth * blocks.length, height);
    
    g.setColor(Color.BLACK);
    int zero = columnWidth * (-min) + columnWidth / 2;
    g.drawLine(zero, 0, zero, height);
    int half = columnWidth * (int) (0.5f / stepSize);
    g.drawLine(zero - half, height / 2, zero - half, height);
    g.drawLine(zero + half, height / 2, zero + half, height);
    
    g.setColor(Color.BLUE);
    for (int i = 0; i != blocks.length - 1; ++i) {
      int x0 = columnWidth * (i + 0) + columnWidth / 2;
      int x1 = columnWidth * (i + 1) + columnWidth / 2;
      int y0 = (int) ((1 - (double) blocks[i + 0] / max) * (height - 1));
      int y1 = (int) ((1 - (double) blocks[i + 1] / max) * (height - 1));
      g.drawLine(x0, y0, x1, y1);
    }
    
    g.dispose();
    Tools.writePNG(out, "stat.png");
  }
  
}
