package test;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;

public class PixelChecker {

  public static void main(String[] args) throws Exception {
    BufferedImage image = ImageIO.read(new File("/home/noname/decapsulation/image-processing/dist3.png"));
    WritableRaster raster = image.getRaster();
    
    long sumA = 0;
    long sumB = 0;
    
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    for (int y = 0; y != sy; ++y)
    for (int x = 0; x != sx; ++x)
      if ((x + y) % 2 == 1) {
        int v = raster.getPixel(x, y, new int[3])[1];
        if (x % 4 == 1 || x % 4 == 2)
          sumA += v;
        else
          sumB += v;
      }
    
    System.out.println(sumA / (0.25 * sx * sy));
    System.out.println(sumB / (0.25 * sx * sy));
    System.out.println(sumA / (double) sumB);
  }

}
