package old.stitcher;

import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;

import old.storage.Point;
import old.storage.PointProcessor;
import old.storage.QuadTreeCreator2;
import old.storage.QuadTreeSizeFixer2;

import stitcher.StitchInfo;

import data.DataTools;
import data.Tools;
import distributed.Bootstrap;

public class DataStitcher {
  
  public static final int SX = 3250;
  public static final int SY = 2450;
  
  public static int su;
  public static int sv;
  public static int numImages;
  
  public static double[][] coefs;
  public static double[][][] positions;
  public static double perspectiveX, perspectiveY;
  
  public static double[][][] lighting;
  public static double[][] secondOrderLighting;
  public static double lightingX, lightingY;
  public static double lightingFactor = 1.0;
  
  public static WritableRaster[][] images;
  
  public static void readLighting(String filename) throws IOException {
    lighting = new double[4][][];
    for (int ly = 0; ly != 2; ++ly)
      for (int lx = 0; lx != 2; ++lx)
        lighting[2 * ly + lx] =
            DataTools.readMatrixDouble(DataTools.openReading(filename + "-" + lx + ly + ".dat"));
  }
  
  public static void readSecondOrderLighting(String filename) throws IOException {
    secondOrderLighting = new double[numImages][1];
    
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");
    
    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u)
        secondOrderLighting[v * su + u][0] = Math.exp(scanner.nextDouble());
    
    lightingX = scanner.nextDouble();
    lightingY = scanner.nextDouble();
    
    in.close();
  }
  
  public static void readParameters(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");
    
    su = scanner.nextInt();
    sv = scanner.nextInt();
    numImages = su * sv;
    System.err.println(su + " x " + sv);
    
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
        System.err.print(coefs[i][j] + ", ");
      }
      System.err.println();
    }
    System.err.println();
    
    // optional?
    perspectiveX = scanner.nextDouble();
    perspectiveY = scanner.nextDouble();
  }
  
  public static void loadImages(String source) throws IOException {
    images = new WritableRaster[sv][su];
    
    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u) {
        System.err.println(u + "-" + v);
        images[v][u] = ImageIO.read(new File(source + "/" + u + "-" + v + ".PNG")).getRaster();
      }
    System.err.println();
  }
  
  public static strictfp double getFactor(int s, double x, double y) {
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
  
  public static final int STITCH = 0;
  
  public static strictfp void main(String[] args) throws IOException {
    readParameters(args[1]);
    loadImages(args[2]);
    readLighting(args[3]);
    readSecondOrderLighting(args[4]);
    
    int[] pixel = new int[1];
    
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    
    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u) {
        System.err.println(u + "" + v);
        
        for (int y = 0; y != SY; ++y)
          for (int x = 0; x != SX; ++x) {
            if (Tools.isPixelDead(x, y))
              continue;
            
            int lx = x % 2;
            int ly = y % 2;
            int s = 2 * ly + lx;
            
            double xx = x - 0.5 * (SX - 1);
            double yy = y - 0.5 * (SY - 1);
            
            double factor = getFactor(s, xx, yy);
            
            xx = factor * xx + positions[v][u][0];
            yy = factor * yy + positions[v][u][1];
            
            minX = Math.min(minX, xx);
            minY = Math.min(minY, yy);
          }
      }
    
    System.err.println("minX: " + minX);
    System.err.println("minY: " + minY);
    
    //DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
    //    args[0])));
    PointProcessor processor = new PointProcessor(0);
    QuadTreeCreator2<Point> creator =
        new QuadTreeCreator2<Point>(
            DataTools.DIR + "stitching" + StitchInfo.SUFFICES[STITCH] + "/", processor);
    
    Point point = new Point();
    
    int[] hist = new int[900];
    
    for (int v = 0; v != sv; ++v)
      for (int u = 0; u != su; ++u) {
        System.err.println(u + "" + v);
        
        for (int y = 0; y != SY; ++y)
          for (int x = 0; x != SX; ++x) {
            if (Tools.isPixelDead(x, y))
              continue;
            
            int lx = x % 2;
            int ly = y % 2;
            int s = 2 * ly + lx;
            
            double xx = x - 0.5 * (SX - 1);
            double yy = y - 0.5 * (SY - 1);
            
            double factor = getFactor(s, xx, yy);
            
            xx = (factor * xx + positions[v][u][0]) - minX;
            yy = (factor * yy + positions[v][u][1]) - minY;
            
            double light =
                lighting[s][y / 2][x / 2]
                    * secondOrderLighting[v * su + u][0]
                    * Math.exp((lightingX * (xx - positions[sv - 1][su - 1][0]) + lightingY
                        * (yy - positions[sv - 1][su - 1][1])) / 2) * lightingFactor;
            double val = (images[v][u].getPixel(x, y, pixel)[0] / light);
            
            //out.writeFloat((float) xx);
            //out.writeFloat((float) yy);
            //out.writeFloat((float) val);
            //out.writeInt((s << 0) + (u << 2) + (v << 5) + (x << 8) + (y << 20));
            
            point.x = (float) xx;
            point.y = (float) yy;
            point.val = (float) val;
            
            // ttvvvuuuyyyyyyyyyyyxxxxxxxxxxxss
            point.flags =
                (s << 0) | ((x / 2) << 2) | ((y / 2) << 13) | (u << 24) | (v << 27)
                    | (STITCH << 30);
            
            //creator.add(new Point((float) xx, (float) yy, (float) val, (s << 0) + (u << 2)
            //    + (v << 5) + (x << 8) + (y << 20)));
            
            creator.add(point);
            
            ++hist[(int) (yy / 16)];
          }
      }
    
    for (int i = 0; i != hist.length; ++i)
      System.out.println(i + ": " + hist[i]);
    
    //out.close();
    creator.write(args[0], 1);
    
    new QuadTreeSizeFixer2(args[0]);
  }
  
}
