package stitcher;

import java.io.File;

public class StitchInfo {
  
  public static final int NUM_STITCHES = 4;
  
  public static String getStitchDir() {
    if (new File("").getAbsolutePath().contains(".eclipse_workspace"))
      return "/media/book/decapsulation/";
    
    return new File("").getAbsolutePath() + "/";
  }
  
  public static final String[] SUFFICES = {"", "-cpu-5", "-cpu-3", "-ppu", "-combined"};
  
  public static final String getFilename(int stitch) {
    return "stitch" + SUFFICES[stitch] + ".dat";
  }
  
  public static final double[][] MEANS = {
      {574.1324182879464, 714.8272785917254, 696.2993459038588, 348.4608755467648},
      {777.3101487429586, 1003.4034229418198, 978.2656045007932, 511.12464846261764},
      {516.8639671997719, 615.6375325723697, 601.227664373054, 298.30339046293085},
      {420.7105265230326, 556.1755576645592, 543.3423627216405, 264.05367291309886}};
  
  public static final double[][] MAXIMUMS = {
      {932.8594970703125, 1103.447021484375, 1066.49426269531255, 664.3341064453125},
      {2465.12109375, 3113.622802734375, 3031.559814453125, 1508.9423828125},
      {1672.125, 2061.46337890625, 2008.2626953125, 906.8636474609375},
      {1435.3631591796875, 1706.0277099609375, 1674.4476318359375, 753.2068481445312}};
  
  public static double getSafeFactor(int i) {
    double min = Double.MAX_VALUE;
    for (int j = 0; j != 4; ++j)
      min = Math.min(min, 255.99 * MEANS[i][j] / MAXIMUMS[i][j]);
    return min;
  }
  
  public static double[] getFactors(int i) {
    double[] result = new double[4];
    double factor = getSafeFactor(i);
    for (int j = 0; j != 4; ++j)
      result[j] = factor / MEANS[i][j];
    return result;
  }
  
  public static final int[][] BOUNDARIES = new int[][] { {13534, 12919}, {13427, 12851},
      {13617, 12827}, {13570, 12622}, {13607, 12914}};
  
  public static final int NUM_IMAGES[] = new int[] {35, 35, 30, 30};
  
  public static final int IMAGE_DIMENSIONS[][] = new int[][] { {3250, 2450}, {3250, 2450},
      {3250, 2450}, {3250, 2450}};
  
}
