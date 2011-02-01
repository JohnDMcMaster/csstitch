package devignetter;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;

import tools.RectLister;

public class ObstacleSelector {

  final static int SU = 5;
  final static int SV = 7;
  
  public static void main(String[] args) throws IOException {
    for (int v = 0; v != SV; ++v)
      for (int u = 0; u != SU; ++u) {
        String name = DataTools.DIR + "obstacles/" + u + "-" + v + ".dat";
        Rectangle[] r = DataTools.readRectangles(DataTools.openReading(name));
        BufferedImage image = ImageIO.read(new File(DataTools.DIR + "composites/" + u + "-" + v + ".PNG"));
        r = RectLister.doSelection(image, r);
        DataOutputStream out = DataTools.openWriting(name);
        DataTools.writeRectangles(out, r);
        out.close();
      }
  }

}
