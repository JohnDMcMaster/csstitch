package confocal;

import java.io.IOException;

import operations.image.ImageOpsDouble;

public class Test2 {
  
  public static final String DIR = "/home/noname/di/confocal/";
  
  public static void main(String[] args) throws IOException {
    for (int i = 0; i != 4; ++i) {
      double[][] a = Test.loadDouble(DIR + String.format("Chips_All_lasers_LOC2_ch%02d.tif", i));
      System.err.println(ImageOpsDouble.mean(a));
    }
  }
  
}
