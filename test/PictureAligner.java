package test;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;

public class PictureAligner {

  public static BufferedImage[] view = new BufferedImage[2];
  public static int[][][] image = new int[2][][];
  public static int[][][] sums = new int[2][][];

  public static int sx;
  public static int sy;

  public static final int OVERLAP = 256;

  public static int getRectDifference(int[][] array, int x0, int y0, int x1, int y1) {
    return array[y1][x1] + array[y0][x0] - array[y1][x0] - array[y0][x1];
  }

  public static double computeDifference(int x0, int y0, int x1, int y1, int width, int height) {
    double sum0 = getRectDifference(sums[0], x0, y0, x0 + width, y0 + height)
        / (double) (width * height);
    double sum1 = getRectDifference(sums[1], x1, y1, x1 + width, y1 + height)
        / (double) (width * height);

    int value = 0;
    for (int y = 0; y != height; ++y)
      for (int x = 0; x != width; ++x) {
        double diff = image[0][y0 + y][x0 + x] / sum0 - image[1][y1 + y][x1 + x] / sum1;
        value += diff * diff;
      }

    return value / (width * height);
  }

  public static void drawImage(Graphics g, double zoom, BufferedImage image, int x, int y, int x0, int y0, int x1, int y1) {
    Rectangle r = g.getClipBounds();

    int dx0 = r.x;
    int dy0 = r.y;
    int dx1 = dx0 + r.width;
    int dy1 = dy0 + r.height;

    int sx0 = (int) (dx0 / zoom);
    int sy0 = (int) (dy0 / zoom);
    int sx1 = (int) (dx1 / zoom) + 1;
    int sy1 = (int) (dy1 / zoom) + 1;

    sx0 = Math.max(x + x0, sx0);
    sy0 = Math.max(y + y0, sy0);
    sx1 = Math.min(x + x1, sx1);
    sy1 = Math.min(y + y1, sy1);
    
    dx0 = (int) (sx0 * zoom);
    dy0 = (int) (sy0 * zoom);
    dx1 = (int) (sx1 * zoom);
    dy1 = (int) (sy1 * zoom);

    g.drawImage(image, dx0, dy0, dx1, dy1, sx0 - x, sy0 - y, sx1 - x, sy1 - y, null);
  }

  public static void main(String[] args) throws IOException {
    for (int i = 0; i != 2; ++i) {
      image[i] = Tools.readColorImageNoMetal(i, 0);
      // view[i] = ImageIO.read(new File(DataTools.DIR + "scaled/0-0.png"));

      sx = image[0][0].length;
      sy = image[0].length;

      for (int y = 0; y != sy; ++y)
        for (int x = 0; x != sx; ++x)
          if ((x + y) % 2 == 0) {
            int weight = 0;
            int value = 0;
            for (int j = 0; j != 4; ++j) {
              int xx = x + (j % 2) * (2 * (j / 2) - 1);
              int yy = y + ((j + 1) % 2) * (2 * (j / 2) - 1);
              if (xx >= 0 && yy >= 0 && xx < sx && yy < sy) {
                ++weight;
                value += image[i][yy][xx];
              }
            }
            image[i][y][x] = (value + 2) / weight;
            //image[i][y][x] = 0;
          }

      view[i] = Tools.getGreyscaleImageFromMatrix(image[i]);
    }

    System.out.println("loaded");

    Tools.display(new PointChooser(view[0], new PointChooser.Handler() {
      int ax = 2788, ay = 0;
      boolean show = false;
      int dir = 0;

      public boolean click(int button, int x, int y) {
        if (button == 2) {
          show = !show;
          return true;
        }

        if (button == 1) {
          ax += 2 * (dir / 2) - 1;
          ay += 2 * (((dir + 3) % 4) / 2) - 1;
          System.out.println("(" + ax + ", " + ay + ")");
          return true;
        }

        if (button == 3) {
          dir = (dir + 1) % 4;
          System.out.println("dir " + dir);
          return false;
        }

        return false;
      }

      public void draw(Graphics g, double zoom) {
        if (show)
          drawImage(g, zoom, view[1], ax, ay, 232, 0, sx, sy);
      }
    }));

    /*
     * for (int i = 0; i != 2; ++i) { sums[i] = new int[sy][sx]; for (int y = 0;
     * y != sy; ++y) for (int x = 0; x != sx; ++x) { int val = image[i][y][x];
     * if (x != 0) val += sums[i][y][x - 1]; if (y != 0) val += sums[i][y -
     * 1][x]; if (x != 0 && y != 0) val -= sums[i][y - 1][x - 1]; sums[i][y][x] =
     * val; } }
     * 
     * int[][] sumsDiffX = new int[sy][OVERLAP]; int[][] sumsDiffY = new
     * int[sy][OVERLAP];
     * 
     * for (int y = 0; y != sy; ++y) for (int x = 0; x != OVERLAP; ++x) { if (y !=
     * 0) { int temp = image[0][y][x + sx - OVERLAP] - image[0][y - 1][x + sx -
     * OVERLAP]; int val = temp * temp + sumsDiffY[y - 1][x]; if (x != 0) val +=
     * sumsDiffY[y][x - 1] - sumsDiffY[y - 1][x - 1]; sumsDiffY[y][x] = val; }
     * 
     * if (x != 0) { int temp = image[0][y][x + sx - OVERLAP] - image[0][y][x +
     * sx - OVERLAP - 1]; int val = temp * temp + sumsDiffX[y][x - 1]; if (y !=
     * 0) val += sumsDiffX[y - 1][x] - sumsDiffX[y - 1][x - 1]; sumsDiffX[y][x] =
     * val; } }
     * 
     * System.out.println("Sums built");
     * 
     * final int k = 64;
     * 
     * int maxC = 0; int maxX = 0, maxY = 0; for (int y = 200; y < sy - k; ++y)
     * for (int x = 0; x < OVERLAP - k; ++x) { int a =
     * getRectDifference(sumsDiffX, x, y, x + k, y + k); int b =
     * getRectDifference(sumsDiffY, x, y, x + k, y + k); int c = Math.min(a, b);
     * if (c > maxC) { maxC = c; maxX = x; maxY = y; } }
     * 
     * System.out.println("max: " + maxX + ", " + maxY);
     * 
     * double minVal = 1000000; int minX = 0, minY = 0; for (int y = 0; y != sy -
     * k - 1; ++y) { if (y % 10 == 0) System.out.println(y);
     * 
     * for (int x = 0; x < OVERLAP - k - 1; ++x) { double val =
     * computeDifference(maxX + sx - OVERLAP, maxY, x, y, k, k); if (val <
     * minVal) { minVal = val; minX = x; minY = y; } } }
     * 
     * Tools.markRectangle(view[0], maxX + sx - OVERLAP, maxY, maxX + k + sx -
     * OVERLAP, maxY + k); Tools.markRectangle(view[1], minX, minY, minX + k,
     * minY + k);
     */
  }
}
