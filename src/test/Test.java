package test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import tools.SumOfSquares;

import data.DataTools;
import data.Tools;


public class Test {

  public static final int SU = 5;
  public static final int SV = 6;

  public static void main(String[] args) throws IOException {
    produceComponents();
    produceComposites();
  }

  public static double[][] findMeans() throws IOException {
    // values for HF (images/)
    //if (0 == 0)
    //  return new double[][] { {591.4538795317337, 736.6763398286612},
    //      {717.55006068087, 359.0503858694775}};

    // values for cpu 5 scan (images-cpu-5/)
    //if (0 == 0)
    //  return new double[][] { {845.3145783431264, 1091.8632537017268},
    //      {1064.5247992536445, 556.5200479677056}};

    // values for cpu 3 scan (images-cpu-3/)
    //if (0 == 0)
    //  return new double[][] { {569.9324263819988, 679.163458947148},
    //      {663.3149173082157, 329.03346262271066}};
    //
    
    // values for ppu scan (images-ppu/)
    if (0 == 0)
      return new double[][] { {429.3827014965988, 568.5142887953951},
          {555.3936164939821, 269.5388677760335}};
    
    int sx = 3250;
    int sy = 2450;

    double[][] means = new double[2][2];

    for (int uy = 0; uy != SV; ++uy)
      for (int ux = 0; ux != SU; ++ux) {
        System.out.println(ux + "-" + uy);
        double[][] matrix = Tools.getMatrixFromImage(DataTools.DIR + "images-ppu/" + ux + "-"
            + uy + ".PNG");

        double[][] tempMeans = new double[2][2];
        for (int y = 0; y != sy; ++y)
          for (int x = 0; x != sx; ++x)
            tempMeans[y % 2][x % 2] += matrix[y][x];

        for (int b = 0; b != 2; ++b)
          for (int a = 0; a != 2; ++a)
            means[b][a] += tempMeans[b][a] / ((sx / 2) * (sy / 2));
      }

    for (int b = 0; b != 2; ++b)
      for (int a = 0; a != 2; ++a) {
        means[b][a] /= SU * SV;
        System.out.println(means[b][a]);
      }

    return means;
  }

  public static void produceComponents() throws IOException {
    int sx = 3250;
    int sy = 2450;

    double[][] submatrix = new double[sy / 2][sx / 2];
    double[][] means = findMeans();

    for (int uy = 0; uy != SV; ++uy)
      for (int ux = 0; ux != SU; ++ux) {
        double[][] matrix = Tools.getMatrixFromImage(DataTools.DIR + "images-ppu/" + ux + "-"
            + uy + ".PNG");

        for (int b = 0; b != 2; ++b)
          for (int a = 0; a != 2; ++a) {
            for (int y = 0; y != sy / 2; ++y)
              for (int x = 0; x != sx / 2; ++x)
                submatrix[y][x] = matrix[2 * y + b][2 * x + a];

            Tools.scaleMatrix(submatrix, 192 / means[b][a]);

            BufferedImage out = Tools.getGreyscaleColorImageFromMatrix(submatrix);
            Tools.writePNG(out, DataTools.DIR + "comp-ppu-" + a + b + "/" + ux + "-" + uy
                + ".PNG");
          }
      }
  }

  public static void produceComposites() throws IOException {
    int sx = 3250;
    int sy = 2450;

    double[][] means = findMeans();
    int[][] out = new int[sy][sx];

    for (int uy = 0; uy != SV; ++uy)
      for (int ux = 0; ux != SU; ++ux) {
        double[][] matrix = Tools.getMatrixFromImage(DataTools.DIR + "images-ppu/" + ux + "-"
            + uy + ".PNG");

        for (int y = 0; y != sy; ++y)
          for (int x = 0; x != sx; ++x)
            out[y][x] = (int) (192 * matrix[y][x] / means[y % 2][x % 2]);

        Tools.writePNG(Tools.getGreyscaleImageFromMatrix(out), DataTools.DIR + "composites-ppu/"
            + ux + "-" + uy + ".PNG");
      }
  }

}
