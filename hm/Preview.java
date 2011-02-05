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

public class Preview {
  
  public static void createPreview(String dir) throws IOException {
    BufferedImage[] images = new BufferedImage[3];
    WritableRaster[] rasters = new WritableRaster[3];
    
    for (int c = 0; c != images.length; ++c) {
      images[c] = ImageIO.read(new File(dir + (c + c / 2) + ".png"));
      rasters[c] = images[c].getRaster();
    }
    
    int sx = images[0].getWidth();
    int sy = images[1].getHeight();
    
    double[] means = new double[3];
    int[] p = new int[1];
    
    for (int c = 0; c != images.length; ++c) {
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          means[c] += rasters[c].getPixel(x, y, p)[0];
      
      means[c] /= sx * sy;
    }
    
    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = result.getRaster();
    int[] pixel = new int[3];
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        for (int c = 0; c != 3; ++c) {
          int v = (int) ((128 * rasters[c].getPixel(x, y, p)[0]) / means[c]);
          pixel[c] = v > 255 ? 255 : v;
        }
        
        raster.setPixel(x, y, pixel);
      }
    
    ImageIO.write(result, "jpeg", new File(dir + "preview.jpg"));
  }
  
  public static void main(String[] args) throws IOException {
    for (String arg : args)
      createPreview(arg + "/");
  }
}
