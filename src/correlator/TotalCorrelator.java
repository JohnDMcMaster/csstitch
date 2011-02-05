package correlator;

import general.Streams;


import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class TotalCorrelator {

  public static final int SX = 3250;
  public static final int SY = 2450;

  public static final String OUT_DIR = DataTools.DIR + "corr-list-21-22-23/";

  public static void main(String[] args) throws IOException {
    int[][] matrix = new int[SY][SX];
    int[] pixel = new int[1];

    String[] names = Streams.readText(OUT_DIR + "list.txt").split("\n");
    for (String name : names) {
      System.out.println(name);
      BufferedImage image = ImageIO.read(new File("/media/book/decapsulation/backup/grey-data/"
          + name + ".PNG"));
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
        Tools.writePNG(Tools.getGreyscaleImageFromMatrix(submatrices[ly][lx]), OUT_DIR + "comp-"
            + lx + ly + ".PNG");
      }

    int[][] submatrix = new int[SY / 2][SX / 2];
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        for (int y = 0; y != SY / 2; ++y)
          for (int x = 0; x != SX / 2; ++x)
            submatrix[y][x] += submatrices[ly][lx][y][x];

    Tools.scaleMatrix(submatrix, 192 / Tools.findMean(submatrix));
    Tools.writePNG(Tools.getGreyscaleImageFromMatrix(submatrix), OUT_DIR + "total.PNG");
  }

}
