package test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

import tools.RectLister;

import data.DataTools;

public class LightSampleSelector {

  public static void main(String[] args) throws Exception {
    BufferedImage source = ImageIO.read(new File(
//        "/home/noname/decapsulation/image-processing/dist3.png"));
        "/media/book/decapsulation/backup/raw-data/P6214307.PNG"));
    WritableRaster sourceRaster = source.getRaster();

    int sx = source.getWidth();
    int sy = source.getHeight();

    BufferedImage image = new BufferedImage((sx + sy) / 2, (sx + sy) / 2, BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster raster = image.getRaster();

    for (int y = 0; y != sy; ++y) {
      if (y % 100 == 0)
        System.out.println(y);
      
      for (int x = 0; x != sx; ++x) {
        if ((x + y) % 2 == 0)
          continue;

        int u = (x + (y + 1)) / 2;
        int v = (sy - (y + 1) + x) / 2;

        double val = sourceRaster.getPixel(x, y, (int[]) null)[1];
        if (x % 2 == 1)
          val *= 0.973709160818136;
        raster.setPixel(u, v, new int[] {(int) (32 * val)});
      }
    }
    
    //Tools.display(new PointChooser(image));
   
    final String filename = "/home/noname/decapsulation/image-processing/rectangles2.dat";
    
    FileInputStream in = new FileInputStream(filename);
    Rectangle[] rectangles = DataTools.readRectangles(new DataInputStream(in));
    in.close();
    
    rectangles = RectLister.doSelection(image, rectangles);
    
    FileOutputStream out = new FileOutputStream(filename);
    DataTools.writeRectangles(new DataOutputStream(out), rectangles);
    out.close();//*/
  }
}
