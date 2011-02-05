package metal;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;

import data.Tools;

public class ColorClusters {
  
  public static double distanceSquared(double[] v0, double[] v1) {
    double d0 = v0[0] - v1[0];
    double d1 = v0[1] - v1[1];
    double d2 = v0[2] - v1[2];
    return d0 * d0 + d1 * d1 + d2 * d2;
  }
  
  public static int nearestCenter(double[][] centers, double[] v) {
    double dist = Double.POSITIVE_INFINITY;
    int index = -1;
    for (int j = 0; j != centers.length; ++j) {
      double d = distanceSquared(v, centers[j]);
      if (d < dist) {
        dist = d;
        index = j;
      }
    }
    
    return index;
  }
  
  public static double[][] partition(double[][] entries, int numClusters) {
    int numEntries = entries.length;
    
    double[] distances = new double[numEntries];
    for (int i = 0; i != numEntries; ++i)
      distances[i] = 1024 * 1024;
    
    int[] initialCenters = new int[numClusters];
    for (int j = 0; j != numClusters; ++j) {
      System.err.println("choosing initial center " + j + "...");
      
      double distancesSum = 0;
      for (int i = 0; i != numEntries; ++i)
        distancesSum += distances[i];
      
      double centerIndex = Math.random() * distancesSum;
      for (int i = 0;; ++i) {
        centerIndex -= distances[i];
        if (centerIndex < 0) {
          initialCenters[j] = i;
          break;
        }
      }
      
      for (int i = 0; i != numEntries; ++i)
        distances[i] =
            Math.min(distances[i], distanceSquared(entries[i], entries[initialCenters[j]]));
    }
    
    double[][] centers = new double[numClusters][3];
    for (int j = 0; j != numClusters; ++j)
      for (int k = 0; k != 3; ++k)
        centers[j][k] = entries[initialCenters[j]][k];
    
    for (int c = 0;; ++c) {
      System.err.println("round " + c + "...");
      
      double[][] sums = new double[numClusters][3];
      int[] counts = new int[numClusters];
      
      for (int i = 0; i != numEntries; ++i) {
        int j = nearestCenter(centers, entries[i]);
        for (int k = 0; k != 3; ++k)
          sums[j][k] += entries[i][k];
        ++counts[j];
      }
      
      boolean change = false;
      for (int j = 0; j != numClusters; ++j) {
        double norm = 0;
        for (int k = 0; k != 3; ++k) {
          sums[j][k] /= counts[j];
          norm += sums[j][k] * sums[j][k];
        }
        norm = Math.sqrt(norm);
        
        for (int k = 0; k != 3; ++k) {
          sums[j][k] /= norm;
          if (sums[j][k] != centers[j][k])
            change = true;
        }
      }
      
      if (!change)
        break;
      
      centers = sums;
    }
    
    return centers;
  }
  
  public static void main(String[] args) throws IOException {
    BufferedImage image =
        ImageIO.read(new File("/media/book/decapsulation/backup/raw-batches/batch-8/P6214349.JPG"));
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[3];
    
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    double[][] entries = new double[sx * sy][3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        double norm = Math.sqrt(pixel[0] * pixel[0] + pixel[1] * pixel[1] + pixel[2] * pixel[2]);
        for (int i = 0; i != 3; ++i)
          entries[y * sx + x][i] = pixel[i] / norm;
      }
    
    int numClusters = 5;
    double[][] centers = partition(entries, numClusters);
    Arrays.sort(centers, new Comparator<double[]>() {
      public int compare(double[] o1, double[] o2) {
        if (o1[1] != o2[1])
          return (int) Math.signum(o1[1] - o2[1]);
        return (int) Math.signum(o1[2] - o2[2]);
      }
    });
    
    for (int j = 0; j != numClusters; ++j) {
      for (int k = 0; k != 3; ++k)
        System.out.print(centers[j][k] + ", ");
      System.out.println();
    }
    System.out.println();
    
    double[][] means = new double[numClusters][3];
    int[] counts = new int[numClusters];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int j = nearestCenter(centers, entries[y * sx + x]);
        raster.getPixel(x, y, pixel);
        for (int k = 0; k != 3; ++k)
          means[j][k] += pixel[k];
        ++counts[j];
      }
    
    for (int j = 0; j != numClusters; ++j)
      for (int k = 0; k != 3; ++k)
        means[j][k] /= counts[j];
    
    BufferedImage result = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster resultRaster = result.getRaster();
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int j = nearestCenter(centers, entries[y * sx + x]);
        for (int k = 0; k != 3; ++k)
          pixel[k] = (int) Math.round(means[j][k]);
        resultRaster.setPixel(x, y, pixel);
      }
    
    Tools.displayImages(new Image[] {image, result});
  }
  
}
