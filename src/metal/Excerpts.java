package metal;

import general.execution.Command;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import configuration.Config;
import data.Tools;


public class Excerpts {
  
  private static final String RAM_DIR = Config.getOption("ram-dir");
  
  private static String getFilename(int stitch) {
    return RAM_DIR + File.separator + "stitch-" + stitch + ".bmp";
  }
  
  private static File getFile(int stitch) {
    return new File(getFilename(stitch));
  }
  
  private static void initialize(int stitch) throws IOException {
    new Command(new String[] {"convert",
        Config.getOption("local-dir") + File.separator + "stitch-" + stitch + ".png",
        getFilename(stitch)}).executeChecked();
  }
  
  public static void clear(int stitch) throws IOException {
    File file = getFile(stitch);
    if (file.exists())
      if (!file.delete())
        throw new IOException("cached image could not be deleted");
  }
  
  public static BufferedImage getExcerpt(int stitch, int x0, int y0, int x1, int y1)
      throws IOException {
    File file = getFile(stitch);
    if (!file.exists())
      initialize(stitch);
    
    ImageReader reader = ImageIO.getImageReadersBySuffix("bmp").next();
    reader.setInput(new FileImageInputStream(file));
    
    ImageReadParam param = reader.getDefaultReadParam();
    param.setSourceRegion(new Rectangle(x0, y0, x1 - x0, y1 - y0));
    
    return reader.read(0, param);
  }
  
  public static void main(String[] args) throws IOException {
    Image[] images = new Image[3];
    for (int i = 0; i != 3; ++i)
      images[i] = getExcerpt(i, 7000, 7000, 11000, 11000);
    Tools.displayImages(images);
  }
  
}
