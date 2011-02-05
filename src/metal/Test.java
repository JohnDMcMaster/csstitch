package metal;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import old.storage.Point;


import data.Tools;
import distributed.Bootstrap;
import distributed.server.Servers;

public class Test {
  
  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(Servers.CIP_91);
    
    int p = 0;
    int q = 0;
    int r = 0;
    
    Point[][] array = Selector.select(1, 23);
    float[][] copy = new float[array.length][array[0].length];
    float[][] result = new float[array.length][array[0].length];
    
    for (int y = 1; y != array.length - 1; ++y)
      for (int x = 1 + (y + 1) % 2; x < array[0].length - 1; x += 2) {
        if (array[y][x + 1] != null)
          copy[y][x + 1] = array[y][x + 1].val;
        
        Point a = array[y - 1][x];
        Point b = array[y][x - 1];
        Point c = array[y][x + 1];
        Point d = array[y + 1][x];
        
        if (a == null || b == null || c == null || d == null)
          continue;
        
        float aa = a.val;
        float bb = b.val;
        float cc = c.val;
        float dd = d.val;
        
        if (aa > dd) {
          float tt = aa;
          aa = dd;
          dd = tt;
        }
        
        if (bb > cc) {
          float tt = bb;
          bb = cc;
          cc = tt;
        }
        
        if (aa > bb) {
          float tt = aa;
          aa = bb;
          bb = tt;
        }
        
        // aa < dd, bb < cc, aa < bb
        if (cc < dd)
          ++p;
        else if (bb < dd)
          ++q;
        else {
          result[y][x + 1] = 1;
          ++r;
        }
      }
    
    BufferedImage[] images = new BufferedImage[2];
    images[1] = MetalTools.selectGreen(MetalTools.toImage(copy, 128));
    images[0] = MetalTools.selectGreen(MetalTools.toImage(result, 255));
    Tools.writePNG(images[0], "test-0.png");
    Tools.writePNG(images[1], "test-1.png");
    //Tools.displayImages(images);
    
    System.err.println("embedded : " + p);
    System.err.println("twisted  : " + q);
    System.err.println("separated: " + r);
  }
}
