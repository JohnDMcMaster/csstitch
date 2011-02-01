package test;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class FlattenRaw {

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.out.println("Usage: java FlattenRaw.java in.png out.png");
      System.exit(-1);
    }
    
    BufferedImage source = ImageIO.read(new File(args[0]));
    WritableRaster raster = source.getRaster();
    
    int sx = source.getWidth();
    int sy = source.getHeight();
    
    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster resultRaster = result.getRaster();
    
    int[] pixel = new int[3];
    int[] resultPixel = new int[1];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        resultPixel[0] = pixel[0] + pixel[1] + pixel[2];
        resultRaster.setPixel(x, y, resultPixel);
      }
    
    ImageIO.write(result, "PNG", new File(args[1]));
  }

}
