package old.stitcher;


import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import stitcher.StitchInfo;

import data.DataTools;
import data.Tools;

public strictfp class AffineTransformer {

  public static BufferedImage transform(BufferedImage source, double[] values) {
    int sx = source.getWidth();
    int sy = source.getHeight();

    BufferedImage result = new BufferedImage(sx, sy, source.getType());
    WritableRaster raster = result.getRaster();
    WritableRaster sourceRaster = source.getRaster();

    double invDet = 1 / (values[0] * values[3] - values[1] * values[2]);
    double n00 = invDet * values[3];
    double n01 = -invDet * values[1];
    double n10 = -invDet * values[2];
    double n11 = invDet * values[0];

    int[] pixel = new int[4];

    for (int y = 0; y != sy; ++y) {
      System.out.println(y);

      for (int x = 0; x != sx; ++x) {
        double xx = x - values[4] / 2;
        double yy = y - values[5] / 2;

        double xxx = n00 * xx + n01 * yy;
        double yyy = n10 * xx + n11 * yy;

        int a = (int) Math.round(xxx);
        int b = (int) Math.round(yyy);

        if (a >= 0 && b >= 0 && a < sx && b < sy)
          raster.setPixel(x, y, sourceRaster.getPixel(a, b, pixel));
      }
    }

    return result;
  }

  public static final double RENDER_SCALE = 2;

  public static void main(String[] args) throws IOException {
    double[][] values = new double[][] {
        {1.0, -0.0029911917142704776, 0.0013225407383610334, 1.0, 0.0, 0.0},
        {0.9999677374708568, -0.001974427205493879, 4.907943379827699E-4, 0.9998254452321185,
            89.27990064594864, 78.70119809523271},
        {0.9998873783459207, -0.0015510563810195838, -4.390198869702378E-5, 1.00069607560882,
            -9.377344601364525, 33.623387609195646}};

    for (int i = 0; i != 3; ++i) {
      BufferedImage source = ImageIO.read(new File(DataTools.DIR + "sharp-stitch-" + RENDER_SCALE
          + StitchInfo.SUFFICES[i] + ".png"));
      Tools.writePNG(transform(source, values[i]), DataTools.DIR + "sharp-stitch-" + RENDER_SCALE
          + "-transformed" + StitchInfo.SUFFICES[i] + ".png");
    }
  }
}
