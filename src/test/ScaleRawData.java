package test;


import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class ScaleRawData {

  public static final double[] MEANS = {591.4538795317337, 727.1132002547656, 359.0503858694775};

  public static BufferedImage scale(BufferedImage image) {
    WritableRaster raster = image.getRaster();

    int sx = image.getWidth();
    int sy = image.getHeight();

    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster resultRaster = result.getRaster();

    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        for (int i = 0; i != 3; ++i)
          pixel[i] = Math.min(255, (int) (128 * pixel[i] / MEANS[i]));
        resultRaster.setPixel(x, y, pixel);
      }

    return result;
  }

  public static void main(String[] args) throws IOException {
    int s = 6114199;

    for (int y = 0; y != 7; ++y)
      for (int x = 0; x != 5; ++x) {
        double[][] image = Tools
            .evenLightingNoMetal(DataTools.DIR + "grey-data/P" + s++ + ".PNG");
        Tools.writePNG(Tools.getColorImageFromMatrix(image), DataTools.DIR + "scaled/" + x + "-"
            + y + ".png");
      }
  }

}
