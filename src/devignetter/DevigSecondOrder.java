package devignetter;

import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

import stitcher.StitchInfo;
import tools.LinearEquation;

import data.DataTools;
import distributed.Bootstrap;

public class DevigSecondOrder {
  
  public static final int STITCH = 3;

  public static final int SX = 3250;
  public static final int SY = 2450;

  public static int su;
  public static int sv;
  public static int numImages;

  public static double[][] coefs;
  public static double[][][] positions;
  public static double perspectiveX, perspectiveY;

  public static WritableRaster[] images;
  public static double[][][] lighting;

  public static void readParameters(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");

    su = scanner.nextInt();
    sv = scanner.nextInt();
    numImages = su * sv;
    System.out.println(su + " x " + sv);

    positions = new double[sv][su][2];
    for (int i = 0; i != numImages; ++i) {
      positions[i / su][i % su][0] = scanner.nextDouble();
      positions[i / su][i % su][1] = scanner.nextDouble();
    }

    int numCoefs = scanner.nextInt();
    coefs = new double[4][numCoefs];
    for (int i = 0; i != 4; ++i) {
      for (int j = 0; j != numCoefs; ++j) {
        coefs[i][j] = scanner.nextDouble();
        System.out.print(coefs[i][j] + ", ");
      }
      System.out.println();
    }
    System.out.println();

    perspectiveX = scanner.nextDouble();
    perspectiveY = scanner.nextDouble();
  }

  public static void loadImages(String source) throws IOException {
    images = new WritableRaster[sv * su];

    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u) {
        System.out.println(u + "-" + v);
        images[v * su + u] = ImageIO.read(new File(source + "/" + u + "-" + v + ".PNG"))
            .getRaster();
      }
    System.out.println();
  }

  public static void loadLighting(String filename) throws IOException {
    lighting = new double[4][][];
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        lighting[2 * ly + lx] = DataTools.readMatrixDouble(DataTools.openReading(filename + "-"
            + lx + ly + ".dat"));
  }

  public static double getFactor(int s, double x, double y) {
    double factor = 0;
    double rrpow = 1;
    double rr = x * x + y * y;
    for (int k = 0; k != coefs[0].length; ++k) {
      factor += rrpow * coefs[s][k];
      rrpow *= rr;
    }
    double xx = factor * x;
    double yy = factor * y;
    double perspectiveFactor = xx * perspectiveX + yy * perspectiveY;
    return factor * (1 + perspectiveFactor);
  }

  public static double getInvFactor(int s, double xx, double yy) {
    double aa = xx;
    double bb = yy;
    double factor = 0;
    for (int i = 0; i != 10; ++i) {
      factor = getFactor(s, aa, bb);
      aa = xx / factor;
      bb = yy / factor;
    }
    return 1 / factor;
  }

  public static void mapPoint(int lx, int ly, int u, int v, double x, double y, double[] result) {
    x -= 0.5 * (SX - 1);
    y -= 0.5 * (SY - 1);

    double factor = getFactor(2 * ly + lx, x, y);

    result[0] = x * factor + positions[v][u][0];
    result[1] = y * factor + positions[v][u][1];
  }

  public static void mapPoint(int lx, int ly, int u, int v, double[] point, double[] result) {
    mapPoint(lx, ly, u, v, point[0], point[1], result);
  }

  public static void unmapPoint(int lx, int ly, int u, int v, double xx, double yy, double[] result) {
    xx -= positions[v][u][0];
    yy -= positions[v][u][1];

    double invFactor = getInvFactor(2 * ly + lx, xx, yy);

    result[0] = invFactor * xx + 0.5 * (SX - 1);
    result[1] = invFactor * yy + 0.5 * (SY - 1);
  }

  public static void unmapPoint(int lx, int ly, int u, int v, double[] point, double[] result) {
    unmapPoint(lx, ly, u, v, point[0], point[1], result);
  }

  public static void saveParmeters(String filename, double[] params) throws IOException {
    double[][] matrix = new double[numImages][3];
    for (int a = 0; a != numImages; ++a) {
      matrix[a][0] = params[2 * a + 0];
      matrix[a][1] = params[2 * a + 1];
      if (a != numImages - 1)
        matrix[a][2] = params[numImages + a];
      else
        matrix[a][2] = 1;
    }
    DataOutputStream out = DataTools.openWriting(filename);
    DataTools.writeMatrixDouble(out, matrix);
    out.close();
  }

  public static double getFactor(int lx, int ly, int a, int b) throws IOException {
    double factor = 0;
    double constant = 0;

    double[] mapped = new double[2];
    double[] unmapped = new double[2];

    int[] pixel = new int[1];

    for (int y0 = ly; y0 < SY; y0 += 2)
      for (int x0 = lx; x0 < SX; x0 += 2) {
        mapPoint(lx, ly, a % su, a / su, x0, y0, mapped);
        unmapPoint(lx, ly, b % su, b / su, mapped, unmapped);

        int x1 = (int) Math.round((unmapped[0] - lx) / 2) * 2 + lx;
        int y1 = (int) Math.round((unmapped[1] - ly) / 2) * 2 + ly;

        if (x1 < 0 || y1 < 0 || x1 >= SX || y1 >= SY)
          continue;

        double val0 = images[a].getPixel(x0, y0, pixel)[0];
        double val1 = images[b].getPixel(x1, y1, pixel)[0];

        val0 /= lighting[2 * ly + lx][y0 / 2][x0 / 2];
        val1 /= lighting[2 * ly + lx][y1 / 2][x1 / 2];

        factor += val0 * val0;
        constant += val0 * val1;
      }

    return constant / factor;
  }

  public static void process() throws IOException {
    int numVars = numImages - 1;
    
    double[][] matrix = new double[numVars][numVars];
    double[] constants = new double[numVars];

    double[] mapped = new double[2];
    double[] unmapped = new double[2];

    int[] indices = new int[3];
    double[] values = new double[3];

    int[] pixel = new int[1];

    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx) {
        System.out.println("" + lx + ly + ":");
        
        for (int b = 0; b != numImages; ++b)
          for (int a = 0; a != numImages; ++a) {
            int u0 = a % su;
            int v0 = a / su;

            int u1 = b % su;
            int v1 = b / su;

            if (Math.abs(u0 - u1) + Math.abs(v0 - v1) != 1)
              continue;

            double factor = getFactor(lx, ly, a, b);
            System.out.println("" + u0 + v0 + "-" + u1 + v1 + ": " + factor);

            indices[0] = a == numVars ? 0 : a;
            indices[1] = b == numVars ? 0 : b;
            indices[2] = numVars;

            values[0] = a == numVars ? 0 : 1;
            values[1] = b == numVars ? 0 : -1;
            values[2] = Math.log(factor);

            for (int i = 0; i != indices.length; ++i)
              for (int j = 0; j != indices.length; ++j)
                if (indices[i] != numVars)
                  if (indices[j] != numVars)
                    matrix[indices[i]][indices[j]] += values[i] * values[j];
                  else
                    constants[indices[i]] -= values[i] * values[j];

          }
        
        System.out.println();
      }

    double[] params = LinearEquation.solveLinearEquation(matrix, constants);

    for (int v = 0; v != sv; ++v) {
      for (int u = 0; u != su; ++u)
        System.out.print("" + (v == sv - 1 && u == su - 1 ? 0 : params[v * su + u]) + " ");
      System.out.println();
    }
    System.out.println();

    //saveParmeters(DataTools.DIR + "light-second-order.dat", params);
  }

  public static void main(String[] args) throws IOException {
    Bootstrap.bootstrap(args);

    System.out.println(STITCH);

    readParameters(DataTools.DIR + "stitching" + StitchInfo.SUFFICES[STITCH] + "/modern-filtering-10m7m5m3mlpn.txt");
    loadImages(DataTools.DIR + "composites" + StitchInfo.SUFFICES[STITCH]);
    loadLighting(DataTools.DIR + "light-dist-disk-0");

    process();
  }

}
