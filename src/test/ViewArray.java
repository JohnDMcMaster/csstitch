package test;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class ViewArray {

  public static void main(String[] args) throws IOException {
    viewArray(DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat")));
  }
  
  public static void viewArray(final double[][] matrix) {
    final int sx = matrix.length == 0 ? 0 : matrix[0].length;
    final int sy = matrix.length;
    
    double m = 0, n = 1.E10;
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        m = Math.max(m, matrix[y][x]);
        n = Math.min(n, matrix[y][x]);
      }
    final double max = m;
    final double min = n;
    
    BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image.getRaster();
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        raster.setPixel(x, y, new int[] {(int) ((255 * (matrix[y][x] - min)) / (max - min))});
    
    Tools.display(new PointChooser(image, new PointChooser.Handler() {
      private int focus = 0;
      private int scale = 1;
      
      public void draw(Graphics g, double zoom) {
        g.setColor(Color.RED);
        for (int y = 0; y != sy; ++y)
          for (int x = 0; x != sx; ++x)
            if ((int) ((255 * (matrix[y][x] - min)) / max) == focus)
              g.drawLine((int) (zoom * x), (int) (zoom * y), (int) (zoom * (x + 1)), (int) (zoom * (y + 1)));
      }
      
      public boolean click(int button, int x, int y) {
        if (button == 1) {
          focus += scale;
          return true;
        }
        
        if (button == 3) {
          focus -= scale;
          return true;
        }
        
        System.out.println("focus " + focus);
        return false;
      }
    }));
  }

}
