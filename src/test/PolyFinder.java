package test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class PolyFinder {

  public static void main(String[] args) throws IOException {
    BufferedImage image = ImageIO.read(new File(DataTools.DIR + "scaled/3-5.png"));
    final double[][] matrix = Tools.getMatrixFromImage(image);

    final int sx = image.getWidth();
    final int sy = image.getHeight();

    Tools.display(new PointChooser(image, new PointChooser.Handler() {
      boolean show = true;

      public boolean click(int button, int x, int y) {
        if (button == 2) {
          show = !show;
          return true;
        }

        return false;
      }

      public void draw(Graphics graphics, double zoom) {
        if (show)
          for (int y = 2; y != sy - 2; ++y)
            for (int x = 2 + (y + 1) % 2; x < sx - 2; x += 2) {
              double g = matrix[y][x];
              
              double h = (matrix[y][x - 1] + matrix[y][x + 1]) / 2;
              double v = (matrix[y - 1][x] + matrix[y + 1][x]) / 2;
              
              double r = y % 2 == 0 ? h : v;
              double b = x % 2 == 0 ? h : v;
              
              if (2 * g - r - b >= 25)
                EdgeRecognition.drawPixel(graphics, zoom, Color.YELLOW, x, y);
            }
      }
    }));
  }

}
