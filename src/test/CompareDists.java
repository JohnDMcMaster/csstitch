package test;

import java.io.DataOutputStream;
import java.io.IOException;

import data.DataTools;

public class CompareDists {

  public static void main(String[] args) throws IOException {
    double[][][] matrices = new double[5][][];
    matrices[0] = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "dist0.dat"));
    matrices[1] = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "dist1.dat"));
    matrices[2] = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "dist2.dat"));
    matrices[3] = DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "dist3.dat"));

    int sx = matrices[0][0].length;
    int sy = matrices[0].length;
    
    matrices[4] = new double[sy][sx];
    double max = 0;
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        for (int i = 0; i != 4; ++i) {
          matrices[4][y][x] += matrices[i][y][x];
          if (matrices[4][y][x] > max)
            max = matrices[4][y][x];
        }
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        matrices[4][y][x] /= max;

    DataOutputStream out = DataTools.openWriting(DataTools.DIR + "light-dist-20.dat");
    DataTools.writeMatrixDouble(out, matrices[4]);
    out.close();
    
    for (int i = 0; i != matrices.length; ++i) {
      double sum = 0;
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          sum += matrices[i][y][x];
      double mean = sum / sum;
      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          matrices[i][y][x] /= mean;
    }

    double maxDist = 0;
    double meanSqDist = 0;
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        double q = Math.abs(matrices[0][y][x] / matrices[1][y][x] - 1);
        maxDist = Math.max(maxDist, q);
        meanSqDist = q * q;
      }
    meanSqDist = Math.sqrt(meanSqDist) / (sx * sy);

    System.out.println(maxDist);
    System.out.println(meanSqDist);
  }

}
