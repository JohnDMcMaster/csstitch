package devignetter;

import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import tools.LinearEquation;

import data.DataTools;

public class Devignetter {

  public static final int SX = 3250;
  public static final int SY = 2450;

  public static final double SLOPE = 32;

  public static final double RADIUS = 0.75 * SLOPE;
  public static final double CUTOFF_LOW = 0.4;
  public static final double CUTOFF_HIGH = 0.6;

  public static final int NUM_POINTS_X = (int) (SX / SLOPE) + 1;
  public static final int NUM_POINTS_Y = (int) (SY / SLOPE) + 1;

  public static final double OFFSET_X = (SX - (NUM_POINTS_X - 1) * SLOPE) / 2;
  public static final double OFFSET_Y = (SY - (NUM_POINTS_Y - 1) * SLOPE) / 2;

  public static int su;
  public static int sv;
  public static int numImages;

  public static double[][] coefs;
  public static double[][][] positions;
  public static double perspectiveX, perspectiveY;

  public static double[][][] lighting;
  
  public static WritableRaster[][] images;

  public static void readLighting(String filename) throws IOException {
    lighting = new double[4][][];
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        lighting[2 * ly + lx] = DataTools.readMatrixDouble(DataTools.openReading(filename + "-"
            + lx + ly + ".dat"));
  }
  
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
    
    // optional?
    perspectiveX = scanner.nextDouble();
    perspectiveY = scanner.nextDouble();
  }

  public static void loadImages(String source) throws IOException {
    images = new WritableRaster[sv][su];

    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u) {
        System.out.println(u + "-" + v);
        images[v][u] = ImageIO.read(new File(source + "/" + u + "-" + v + ".PNG")).getRaster();
      }
    System.out.println();
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

  public static double sample(int lx, int ly, int u, int v, double cx, double cy) {
    int xMin = Math.max(0, 2 * (int) Math.floor((cx - RADIUS) / 2 - 1));
    int yMin = Math.max(0, 2 * (int) Math.floor((cy - RADIUS) / 2 - 1));

    int xMax = Math.min(SX, 2 * (int) Math.floor((cx + RADIUS) / 2 + 1));
    int yMax = Math.min(SY, 2 * (int) Math.floor((cy + RADIUS) / 2 + 1));

    TreeMap<Double, Integer> histogram = new TreeMap<Double, Integer>();
    int numPoints = 0;
    int[] pixel = new int[1];

    for (int y = yMin + ly; y < yMax; y += 2)
      for (int x = xMin + lx; x < xMax; x += 2) {
        double a = x - cx;
        double b = y - cy;
        if (a * a + b * b > RADIUS * RADIUS)
          continue;

        images[v][u].getPixel(x, y, pixel);
        Double val = pixel[0] / lighting[2 * ly + lx][y / 2][x / 2];
        Integer num = histogram.get(val);
        if (num == null)
          histogram.put(val, 1);
        else
          histogram.put(val, num + 1);
        ++numPoints;
      }

    double sum = 0;
    int weight = 0;
    int counter = 0;
    for (double value : histogram.keySet())
      for (int i = histogram.get(value); i != 0; --i) {
        if (counter >= CUTOFF_LOW * numPoints && counter < CUTOFF_HIGH * numPoints) {
          sum += value;
          ++weight;
        }
        ++counter;
      }
    
    return sum / weight;
  }

  public static void solveSecondOrder(int lx, int ly) {
    int numVars = 2 * su * sv;
    
    double[][] matrix = new double[numVars][numVars];
    double[] constants = new double[numVars];

    double[] vec = new double[numVars + 1];

    double[] mapped = new double[2];
    double[] unmapped = new double[2];

    int[] pixel = new int[1];

    for (int b = 0; b != numImages; ++b)
      for (int a = 0; a != numImages; ++a) {
        int u0 = a % su;
        int v0 = a / su;

        int u1 = b % su;
        int v1 = b / su;

        if (Math.abs(u0 - u1) + Math.abs(v0 - v1) != 1)
          continue;

        System.out.println("" + u0 + v0 + "-" + u1 + v1);

        for (int y0 = ly; y0 < SY; y0 += 2)
          for (int x0 = lx; x0 < SX; x0 += 2) {
            mapPoint(lx, ly, u0, v0, x0, y0, mapped);
            unmapPoint(lx, ly, u1, v1, mapped, unmapped);

            int x1 = (int) Math.round((unmapped[0] - lx) / 2) * 2 + lx;
            int y1 = (int) Math.round((unmapped[1] - ly) / 2) * 2 + ly;

            if (x1 < 0 || y1 < 0 || x1 >= SX || y1 >= SY)
              continue;

            int val0 = images[v0][u0].getPixel(x0, y0, pixel)[0];
            int val1 = images[v1][u1].getPixel(x1, y1, pixel)[0];

            val0 /= lighting[2 * ly + lx][y0 / 2][x0 / 2];
            val1 /= lighting[2 * ly + lx][y1 / 2][x1 / 2];

            
            
            for (int i = 0; i != numVars; ++i) {
              for (int j = 0; j != numVars; ++j)
                matrix[i][j] += vec[i + 1] * vec[j + 1];
              constants[i] -= vec[i + 1] * vec[0];
            }
          }
      }

    double[] values = LinearEquation.solveLinearEquation(matrix, constants);
    for (int i = 0; i != values.length; ++i)
      System.out.println(values[i]);
    System.out.println();

  }

  public static void main(String[] args) throws IOException {
    readParameters(DataTools.DIR + "stitching/" + "10m5m3ml.txt");
    readLighting(DataTools.DIR + "light-dist-disk-0");
    loadImages(DataTools.DIR + "composites");

    //solveFirstOrder(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
    System.exit(0);

    ArrayList<double[]>[][] correlations = new ArrayList[NUM_POINTS_Y][NUM_POINTS_X];
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x)
        correlations[y][x] = new ArrayList<double[]>();

    TreeMap<Integer, Double>[] matrix = new TreeMap[NUM_POINTS_X * NUM_POINTS_Y];
    for (int i = 0; i != NUM_POINTS_X * NUM_POINTS_Y; ++i)
      matrix[i] = new TreeMap<Integer, Double>();

    // fixed channel
    final int lx = 0;
    final int ly = 0;

    double[] mapped = new double[2];
    double[] unmapped = new double[2];

    for (int b = 0; b != numImages; ++b)
      for (int a = 0; a != numImages; ++a) {
        int u0 = a % su;
        int v0 = a / su;

        int u1 = b % su;
        int v1 = b / su;

        if (Math.abs(u0 - u1) + Math.abs(v0 - v1) != 1)
          continue;

        System.out.println("" + u0 + v0 + "-" + u1 + v1);

        double xxMin = Double.NEGATIVE_INFINITY;
        double yyMin = Double.NEGATIVE_INFINITY;

        double xxMax = Double.POSITIVE_INFINITY;
        double yyMax = Double.POSITIVE_INFINITY;

        for (int j = 0; j != 2; ++j)
          for (int i = 0; i != 2; ++i) {
            double x = lx + i * (SX - 2);
            double y = ly + j * (SY - 2);

            for (int k = 0; k != 2; ++k) {
              mapPoint(lx, ly, k == 0 ? u0 : u1, k == 0 ? v0 : v1, x, y, mapped);

              if (i == 0)
                xxMin = Math.max(xxMin, mapped[0]);
              else
                xxMax = Math.min(xxMax, mapped[0]);

              if (j == 0)
                yyMin = Math.max(yyMin, mapped[1]);
              else
                yyMax = Math.min(yyMax, mapped[1]);
            }
          }

        double x0Min = Double.NEGATIVE_INFINITY;
        double y0Min = Double.NEGATIVE_INFINITY;

        double x0Max = Double.POSITIVE_INFINITY;
        double y0Max = Double.POSITIVE_INFINITY;

        for (int j = 0; j != 2; ++j)
          for (int i = 0; i != 2; ++i) {
            unmapPoint(lx, ly, u0, v0, i == 0 ? xxMin : xxMax, j == 0 ? yyMin : yyMax, unmapped);

            if (i == 0)
              x0Min = Math.max(x0Min, unmapped[0]);
            else
              x0Max = Math.min(x0Max, unmapped[0]);

            if (j == 0)
              y0Min = Math.max(y0Min, unmapped[1]);
            else
              y0Max = Math.min(y0Max, unmapped[1]);
          }

        for (double y0 = OFFSET_Y; y0 < SY; y0 += SLOPE) {
          //System.out.println(y0);

          if (y0 >= y0Min && y0 <= y0Max)
            for (double x0 = OFFSET_X; x0 < SX; x0 += SLOPE)
              if (x0 >= x0Min && x0 <= x0Max) {
                mapPoint(lx, ly, u0, v0, x0, y0, mapped);
                unmapPoint(lx, ly, u1, v1, mapped, unmapped);

                double x1 = unmapped[0];
                double y1 = unmapped[1];

                int p0 = (int) Math.round((x0 - OFFSET_X) / SLOPE);
                int q0 = (int) Math.round((y0 - OFFSET_Y) / SLOPE);

                double p1 = (x1 - OFFSET_X) / SLOPE;
                double q1 = (y1 - OFFSET_Y) / SLOPE;

                p1 = Math.max(0, Math.min(NUM_POINTS_X - 1, p1));
                q1 = Math.max(0, Math.min(NUM_POINTS_Y - 1, q1));

                double val0 = sample(lx, ly, u0, v0, x0, y0);
                double val1 = sample(lx, ly, u1, v1, x1, y1);

                correlations[q0][p0].add(new double[] {p1, q1, val0, val1});

                /*int p1Min = (int) Math.floor(p1);
                int q1Min = (int) Math.floor(q1);

                int p1Max = (int) Math.ceil(p1);
                int q1Max = (int) Math.ceil(q1);

                double weightX = p1Max - p1;
                double weightY = q1Max - q1;

                int[] coefs = new int[5];
                double[] weights = new double[5];

                coefs[0] = q0 * NUM_POINTS_X + p0;
                weights[0] = -val1;

                for (int j = 0; j != 2; ++j)
                  for (int i = 0; i != 2; ++i) {
                    coefs[1 + 2 * j + i] = (j == 0 ? q1Min : q1Max) * NUM_POINTS_X
                        + (i == 0 ? p1Min : p1Max);
                    weights[1 + 2 * j + i] = (i == 0 ? weightX : 1 - weightX)
                        * (j == 0 ? weightY : 1 - weightY) * val0;
                  }

                for (int j = 0; j != 5; ++j) {
                  TreeMap<Integer, Double> map = matrix[coefs[j]];
                  for (int i = 0; i != 5; ++i) {
                    if (!map.containsKey(coefs[i]))
                      map.put(coefs[i], 0.);
                    map.put(coefs[i], map.get(coefs[i]) + weights[i] * weights[j]);
                  }
                }*/
              }
        }
      }

    double[][][][] corrArray = new double[NUM_POINTS_Y][NUM_POINTS_X][][];
    for (int y = 0; y != NUM_POINTS_Y; ++y)
      for (int x = 0; x != NUM_POINTS_X; ++x) {
        corrArray[y][x] = new double[correlations[y][x].size()][];
        for (int k = 0; k != corrArray[y][x].length; ++k)
          corrArray[y][x][k] = correlations[y][x].get(k);
      }

    DataOutputStream out = DataTools.openWriting(DataTools.DIR + "stitching/devig-points.dat");
    DataTools.writeDoubleArrayArrayMatrix(out, corrArray);
    out.close();

    //out = DataTools.openWriting(DataTools.DIR + "stitching/devig.dat");
    //DataTools.writeIntegerDoubleMapArray(out, matrix);
    //out.close();
  }
}
