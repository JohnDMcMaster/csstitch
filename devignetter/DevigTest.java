package devignetter;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;
import tools.RectLister;

public class DevigTest {

  public static void main(String[] args) throws IOException {
    BufferedImage image = ImageIO.read(new File(DataTools.DIR + "stitching/modern-filtering-10m7m5m3mlpn-tiny.png"));
    
    String filename = DataTools.DIR + "even-lighting-rects.dat";
    Rectangle[] r = DataTools.readRectangles(DataTools.openReading(filename));
    //Rectangle[] r = new Rectangle[] {};
    
    r = RectLister.doSelection(image, r);
    
    DataOutputStream out = DataTools.openWriting(filename);
    DataTools.writeRectangles(out, r);
    out.close();
  }

}
