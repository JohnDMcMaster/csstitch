package hm;

import general.Streams;
import general.collections.Pair;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import operations.image.ImageOpsDouble;
import realmetal.Images;

import data.Tools;

public class Test {
  
  public static void main(String[] args) throws IOException {
    String dir = "/home/noname/di/other/electron-ula/k/ula-12c021m-33-date8422";
    
    TreeSet<String> names = new TreeSet<String>();
    for (String name : new File(dir).list())
      if (name.endsWith("JPG"))
        names.add(name);
    String[] files = names.toArray(new String[] {});
    
    int[] speeds = new int[names.size()];
    double min = 1000;
    
    Matcher matcher = Pattern.compile("1/(\\d+)sec").matcher(Streams.readText(dir + "/INFO.TXT"));
    for (int i = 0; i != speeds.length; ++i) {
      matcher.find();
      speeds[i] = Integer.parseInt(matcher.group(1));
      min = Math.min(min, 1.1 * speeds[i]);
    }
    
    for (int i = 0; i != speeds.length; ++i) {
      BufferedImage image = ImageIO.read(new File(dir + "/" + files[i]));
      WritableRaster raster = image.getRaster();
      
      int[] pixel = new int[3];
      for (int y = 0; y != image.getHeight(); ++y)
        for (int x = 0; x != image.getWidth(); ++x) {
          raster.getPixel(x, y, pixel);
          
          for (int j = 0; j != 3; ++j) {
            pixel[j] = (int) ((pixel[j] + 0.5) * Math.pow((double) speeds[i] / min, 0.4));
            if (pixel[j] >= 255)
              pixel[j] = 255;
          }
          raster.setPixel(x, y, pixel);
        }
      
      ImageIO.write(image, "png", new File(dir + "/" + files[i]));
    }
    
  }
  
}
