package correlator;


import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class Correlator {

  final static int SU = 5;
  final static int SV = 7;

  final static int SX = 3250;
  final static int SY = 2450;

  final static String cpu = "-cpu-5";

  public static void main(String[] args) throws IOException {
    int[][] matrix = new int[SY][SX];
    int[] pixel = new int[1];

    for (int v = 0; v != SV; ++v)
      for (int u = 0; u != SU; ++u) {
        System.out.println(u + "-" + v);
        BufferedImage image = ImageIO.read(new File(DataTools.DIR + "images" + cpu + "/" + u + "-"
            + v + ".PNG"));
        WritableRaster raster = image.getRaster();

        for (int y = 0; y != SY; ++y)
          for (int x = 0; x != SX; ++x) {
            raster.getPixel(x, y, pixel);
            matrix[y][x] += pixel[0];
          }
      }

    int[][][][] submatrices = new int[2][2][SY / 2][SX / 2];

    for (int y = 0; y != SY; ++y)
      for (int x = 0; x != SX; ++x)
        submatrices[y % 2][x % 2][y / 2][x / 2] = matrix[y][x];

    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        Tools.scaleMatrix(submatrices[ly][lx], 192 / Tools.findMean(submatrices[ly][lx]));
        Tools.writePNG(Tools.getGreyscaleImageFromMatrix(submatrices[ly][lx]), DataTools.DIR
            + "correlations" + cpu + "/comp-" + lx + ly + ".PNG");
      }

    int[][] submatrix = new int[SY / 2][SX / 2];
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        for (int y = 0; y != SY / 2; ++y)
          for (int x = 0; x != SX / 2; ++x)
            submatrix[y][x] += submatrices[ly][lx][y][x];

    Tools.scaleMatrix(submatrix, 192 / Tools.findMean(submatrix));
    Tools.writePNG(Tools.getGreyscaleImageFromMatrix(submatrix), DataTools.DIR + "correlations"
        + cpu + "/total.PNG");
  }

}
