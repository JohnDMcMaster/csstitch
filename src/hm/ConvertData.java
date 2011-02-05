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

public class ConvertData {
  
  public static void convert(File file) throws IOException {
    BufferedImage image = ImageIO.read(file);
    WritableRaster imageRaster = image.getRaster();
    
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    String path = file.getAbsolutePath();
    path = path.substring(0, path.lastIndexOf("."));
    new File(path).mkdirs();
    
    BufferedImage[] components = new BufferedImage[4];
    int[] pixel = new int[4];
    int[] max = new int[4];
    
    for (int channel = 0; channel != 4; ++channel) {
      components[channel] = new BufferedImage(sx / 2, sy / 2, BufferedImage.TYPE_USHORT_GRAY);
      WritableRaster raster = components[channel].getRaster();
      
      for (int y = 0; y != sy / 2; ++y)
        for (int x = 0; x != sx / 2; ++x) {
          imageRaster.getPixel(2 * x + channel % 2, 2 * y + channel / 2, pixel);
          pixel[0] = pixel[channel / 2 + channel % 2];
          max[channel] = Math.max(max[channel], pixel[0]);
          raster.setPixel(x, y, pixel);
        }
      
      ImageIO.write(components[channel], "png", new File(path + "/" + channel + ".png"));
    }
    
    BufferedImage preview = new BufferedImage(sx / 2, sy / 2, BufferedImage.TYPE_INT_RGB);
    imageRaster = preview.getRaster();
    
    for (int y = 0; y != sy / 2; ++y)
      for (int x = 0; x != sx / 2; ++x)
        for (int color = 0; color != 3; ++color) {
          pixel[color] =
              (255 * components[color + color / 2].getRaster().getPixel(x, y, (int[]) null)[0])
                  / max[color + color / 2];
          imageRaster.setPixel(x, y, pixel);
        }
    
    ImageIO.write(preview, "jpeg", new File(path + "/preview.jpg"));
  }
  
  public static void main(String[] args) throws IOException {
    for (String arg : args)
      convert(new File(arg));
  }
  
}
