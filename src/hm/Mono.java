/*
Copyright Christian Sattler <sattler.christian@gmail.com>
Modifications by John McMaster <JohnDMcMaster@gmail.com>
*/

package hm;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Mono {
  
  public static void main(String[] args) throws IOException {
    args = new String[] {"/home/noname/di/other/xray/cray/data"};
    
    for (String name : new File(args[0]).list())
      if (name.endsWith("jpg")) {
        BufferedImage image = ImageIO.read(new File(args[0] + "/" + name));
        WritableRaster raster = image.getRaster();
        
        BufferedImage output =
            new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        WritableRaster outputRaster = output.getRaster();
        
        int[] pixel = new int[3];
        for (int y = 0; y != image.getHeight(); ++y)
          for (int x = 0; x != image.getWidth(); ++x) {
            raster.getPixel(x, y, pixel);
            pixel[1] = pixel[0];
            pixel[2] = pixel[0];
            outputRaster.setPixel(x, y, pixel);
          }
        
        String outputName = args[0] + "/" + name.substring(0, name.length() - 3) + "png";
        ImageIO.write(output, "png", new File(outputName));
      }
  }
  
}
