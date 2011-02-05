package test;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class ViewHisto {

  public static void main(String[] args) throws IOException {
    final String filename = DataTools.DIR + "raw-devig.png";
    BufferedImage image = ImageIO.read(new File(filename));
    WritableRaster raster = image.getRaster();
    
    int[][] matrix = new int[image.getHeight()][image.getWidth()];
    for (int v = 0; v != matrix.length; ++v)
      for (int u = 0; u != matrix[0].length; ++u)
        matrix[v][u] = raster.getPixel(v, u, (int[]) null)[0];
    
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      int limit = Integer.parseInt(in.readLine());
      int[] pixel = new int[1];
      
      final int sx = 3250;
      final int sy = 2450;
      
      for (int v = 0; v != matrix.length; ++v)
        for (int u = 0; u != matrix[0].length; ++u) {
          pixel[0] = matrix[v][u] < limit ? 0 : matrix[v][u];
          raster.setPixel(v, u, pixel);
        }
      
      Tools.display(new PointChooser(image));
    }
  }

}
