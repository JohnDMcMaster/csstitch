package data;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

import tools.PointChooser;

public class Tools {
  
  public static final int[][] DEAD_PIXELS = { {2782, 575}, {3089, 1432}};
  
  public static double[][] getMatrixFromImage(BufferedImage source) {
    WritableRaster raster = source.getRaster();
    int sx = source.getWidth();
    int sy = source.getHeight();
    
    double[][] matrix = new double[sy][sx];
    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        matrix[y][x] = pixel[0] + pixel[1] + pixel[2];
      }
    
    return matrix;
  }
  
  public static double[][] getMatrixFromImage(String filename) throws IOException {
    return getMatrixFromImage(ImageIO.read(new File(filename)));
  }
  
  public static int[][][] getMatrixFromRenderedImage(BufferedImage source) throws IOException {
    WritableRaster raster = source.getRaster();
    int sx = source.getWidth();
    int sy = source.getHeight();
    
    int[][][] matrix = new int[sy][sx][3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        raster.getPixel(x, y, matrix[y][x]);
    
    return matrix;
  }
  
  public static int[][][] getMatrixFromRenderedImage(String filename) throws IOException {
    return getMatrixFromRenderedImage(ImageIO.read(new File(filename)));
  }
  
  public static BufferedImage getGreyscaleImageFromMatrix(double[][] matrix, int limit) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    BufferedImage image =
        new BufferedImage(sx, sy, limit == 256 ? BufferedImage.TYPE_BYTE_GRAY
            : BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster raster = image.getRaster();
    int[] pixel = {0};
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        pixel[0] = (int) matrix[y][x];
        if (pixel[0] >= limit)
          pixel[0] = limit - 1;
        raster.setPixel(x, y, pixel);
      }
    
    return image;
  }
  
  public static BufferedImage getGreyscaleImageFromMatrix(double[][] matrix) {
    return getGreyscaleImageFromMatrix(matrix, 256);
  }
  
  public static BufferedImage getGreyscaleImageFromMatrix(int[][] matrix) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_BYTE_GRAY);
    WritableRaster raster = image.getRaster();
    int[] pixel = {0};
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        pixel[0] = matrix[y][x];
        if (pixel[0] >= 256)
          pixel[0] = 255;
        raster.setPixel(x, y, pixel);
      }
    
    return image;
  }
  
  public static BufferedImage getGreyscaleColorImageFromMatrix(double[][] matrix) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        int value = Math.min(255, (int) matrix[y][x]);
        for (int i = 0; i != 3; ++i)
          pixel[i] = value;
        raster.setPixel(x, y, pixel);
      }
    
    return image;
  }
  
  public static BufferedImage getColorImageFromMatrix(double[][] matrix) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        for (int i = 0; i != 3; ++i)
          pixel[i] = 0;
        pixel[(x % 2) + (y % 2)] = Math.min(255, (int) matrix[y][x]);
        raster.setPixel(x, y, pixel);
      }
    
    return image;
  }
  
  public static BufferedImage getColorImageFromMatrix(double[][][] matrix) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = image.getRaster();
    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        for (int i = 0; i != 3; ++i)
          pixel[i] = Math.min(255, (int) matrix[y][x][i]);
        
        raster.setPixel(x, y, pixel);
      }
    
    return image;
  }
  
  public static int[][] readColorImage(String filename) throws IOException {
    BufferedImage image = ImageIO.read(new File(filename));
    WritableRaster raster = image.getRaster();
    
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    int[][] matrix = new int[sy][sx];
    
    int[] pixel = new int[4];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        matrix[y][x] = pixel[(x % 2) + (y % 2)];
      }
    
    return matrix;
  }
  
  public static int[][] readColorImageNoMetal(int x, int y) throws IOException {
    return readColorImage(DataTools.DIR + "scaled/" + x + "-" + y + ".png");
  }
  
  public static double[][][] readRenderedImage(String filename) throws IOException {
    BufferedImage image = ImageIO.read(new File(filename));
    WritableRaster raster = image.getRaster();
    
    int sx = image.getWidth();
    int sy = image.getHeight();
    
    double[][][] matrix = new double[sy][sx][3];
    
    int[] pixel = new int[3];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        raster.getPixel(x, y, pixel);
        for (int i = 0; i != 3; ++i)
          matrix[y][x][i] = pixel[i];
      }
    
    return matrix;
  }
  
  public static BufferedImage readImage(DataInput in) throws IOException {
    byte[] array = new byte[in.readInt()];
    in.readFully(array);
    return ImageIO.read(new ByteArrayInputStream(array));
  }
  
  public static <T extends OutputStream & DataOutput> void writeImage(T out, BufferedImage image)
      throws IOException {
    ByteArrayOutputStream array = new ByteArrayOutputStream(65536);
    ImageIO.write(image, "png", array);
    out.writeInt(array.size());
    array.writeTo(out);
    out.flush();
  }
  
  public static void writePNG(BufferedImage image, String filename) throws IOException {
    ImageIO.write(image, "PNG", new File(filename));
  }
  
  public static void eraseNonGreen(int[][] matrix) {
    for (int y = 0; y != matrix.length; ++y)
      for (int x = (y % 2); x < matrix[0].length; x += 2)
        matrix[y][x] = 0;
  }
  
  public static double[][] copyMatrix(double[][] matrix) {
    double[][] result = new double[matrix.length][matrix[0].length];
    for (int y = 0; y != matrix.length; ++y)
      for (int x = 0; x != matrix[0].length; ++x)
        result[y][x] = matrix[y][x];
    
    return result;
  }
  
  public static int[][] copyMatrix(int[][] matrix) {
    int[][] result = new int[matrix.length][matrix[0].length];
    for (int y = 0; y != matrix.length; ++y)
      for (int x = 0; x != matrix[0].length; ++x)
        result[y][x] = matrix[y][x];
    
    return result;
  }
  
  public static boolean isPixelDead(int x, int y) {
    for (int[] pixel : DEAD_PIXELS)
      if (pixel[0] == x && pixel[1] == y)
        return true;
    
    return false;
  }
  
  public static void interpolateDeadPixels(double[][] image) {
    int sx = image[0].length;
    int sy = image.length;
    
    for (int i = 0; i != DEAD_PIXELS.length; ++i) {
      int x = DEAD_PIXELS[i][0];
      int y = DEAD_PIXELS[i][1];
      
      ArrayList<int[]> neighbours = new ArrayList<int[]>();
      if ((x % 2) + (y % 2) == 1)
        for (int j = 0; j != 4; ++j) {
          int xx = x + (2 * (j / 2) - 1);
          int yy = y + (2 * (j % 2) - 1);
          neighbours.add(new int[] {xx, yy});
        }
      else
        for (int j = 0; j != 4; ++j) {
          int xx = x + (j % 2) + (j / 2) - 1;
          int yy = y + ((j + 1) % 2) + ((j + 1) / 2) - 1;
          neighbours.add(new int[] {xx, yy});
        }
      
      int weight = 0;
      double value = 0;
      for (int[] p : neighbours)
        if (p[0] >= 0 && p[1] >= 0 && p[0] < sx && p[1] < sy && !isPixelDead(p[0], p[1])) {
          value += image[p[1]][p[0]];
          ++weight;
        }
      
      if (weight == 0)
        throw new RuntimeException("Dead pixel surrounded by dead pixels!");
      
      image[y][x] = value / weight;
    }
  }
  
  public static double[][] shortenMatrix(double[][] matrix, int sx, int sy) {
    double[][] result = new double[sy][sx];
    
    int dx = (matrix[0].length - sx) / 2;
    int dy = (matrix.length - sy) / 2;
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        result[y][x] = matrix[y + dy][x + dx];
    
    return result;
  }
  
  public static void evenLighting(double[][] image, double[] means) throws IOException {
    int sx = image[0].length;
    int sy = image.length;
    
    double[][] matrix =
        DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat"));
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        if (x % 2 == 1 && y % 2 == 0)
          image[y][x] *= 0.9757262;
        
        image[y][x] = (image[y][x] / matrix[y][x]) * (128 / means[(x % 2) + (y % 2)]);
      }
    
    interpolateDeadPixels(image);
  }
  
  public static void evenLightingGreen(double[][] image, double mean) throws IOException {
    int sx = image[0].length;
    int sy = image.length;
    
    double[][] matrix =
        DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat"));
    
    for (int y = 0; y != sy; ++y)
      for (int x = ((y + 1) % 2); x < sx; x += 2) {
        if (x % 2 == 1)
          image[y][x] *= 0.9757262;
        
        image[y][x] = (image[y][x] / matrix[y][x]) * (128 / mean);
      }
    
    interpolateDeadPixels(image);
  }
  
  public static void evenLightingRendered(double[][][] image, double mean) throws IOException {
    int sx = image[0].length;
    int sy = image.length;
    
    double[][] matrix =
        DataTools.readMatrixDouble(DataTools.openReading(DataTools.DIR + "light-dist-20.dat"));
    matrix = shortenMatrix(matrix, sx, sy);
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x < sx; ++x)
        for (int i = 0; i != 3; ++i)
          image[y][x][i] = (image[y][x][i] / matrix[y][x]) * (128 / mean);
  }
  
  public static double[][] evenLighting(String filename, double[] means) throws IOException {
    double[][] image = getMatrixFromImage(filename);
    evenLighting(image, means);
    return image;
  }
  
  public static double[][] evenLightingGreen(String filename, double mean) throws IOException {
    double[][] image = getMatrixFromImage(filename);
    evenLightingGreen(image, mean);
    return image;
  }
  
  public static double[][][] evenLightingRendered(String filename, double mean) throws IOException {
    double[][][] image = readRenderedImage(filename);
    evenLightingRendered(image, mean);
    return image;
  }
  
  public static double[][] evenLightingNoMetal(String filename) throws IOException {
    return evenLighting(filename, new double[] {675.26785732587, 819.6843105031993,
        409.90028051563456});
  }
  
  public static double[][] evenLightingGreenNoMetal(String filename) throws IOException {
    return evenLightingGreen(filename, 819.6845655088439);
  }
  
  public static double[][] rotateMatrix(double[][] matrix) {
    int sx = matrix[0].length;
    int sy = matrix.length;
    
    double[][] result = new double[(sx + sy + 2) / 2][(sx + sy + 2) / 2];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        if ((x + y) % 2 == 1) {
          int u = (x + (y + 1)) / 2;
          int v = (sy - (y + 1) + x) / 2;
          result[v][u] = matrix[y][x];
        }
    
    return result;
  }
  
  public static double findMean(double[][] matrix) {
    double sum = 0;
    for (int y = 0; y != matrix.length; ++y) {
      double rowSum = 0;
      for (int x = 0; x != matrix[0].length; ++x)
        rowSum += matrix[y][x];
      
      sum += rowSum;
    }
    
    return sum / matrix.length / matrix[0].length;
  }
  
  public static double findMean(int[][] matrix) {
    double sum = 0;
    for (int y = 0; y != matrix.length; ++y) {
      double rowSum = 0;
      for (int x = 0; x != matrix[0].length; ++x)
        rowSum += matrix[y][x];
      
      sum += rowSum;
    }
    
    return sum / matrix.length / matrix[0].length;
  }
  
  public static void scaleMatrix(double[][] matrix, double factor) {
    for (int y = 0; y != matrix.length; ++y)
      for (int x = 0; x != matrix[0].length; ++x)
        matrix[y][x] *= factor;
  }
  
  public static void scaleMatrix(int[][] matrix, double factor) {
    for (int y = 0; y != matrix.length; ++y)
      for (int x = 0; x != matrix[0].length; ++x)
        matrix[y][x] = (int) Math.round(factor * matrix[y][x]);
  }
  
  public static BufferedImage getGreenComponent(BufferedImage source, double scale) {
    Raster sourceRaster = source.getRaster();
    
    int sx = source.getWidth();
    int sy = source.getHeight();
    
    BufferedImage image =
        new BufferedImage((sx + sy) / 2, (sx + sy) / 2, BufferedImage.TYPE_USHORT_GRAY);
    WritableRaster raster = image.getRaster();
    
    for (int y = 0; y != sy; ++y) {
      for (int x = 0; x != sx; ++x) {
        if ((x + y) % 2 == 0)
          continue;
        
        int u = (x + (y + 1)) / 2;
        int v = (sy - (y + 1) + x) / 2;
        
        int[] pixel = sourceRaster.getPixel(x, y, (int[]) null);
        raster.setPixel(u, v, new int[] {(int) (scale * pixel[1])});
      }
    }
    
    return image;
  }
  
  public static void waitForClose(JFrame arg) {
    final JFrame frame = arg;
    final boolean[] closed = new boolean[] {false};
    
    frame.addWindowListener(new WindowListener() {
      
      public void windowOpened(WindowEvent e) {
      }
      
      public void windowIconified(WindowEvent e) {
      }
      
      public void windowDeiconified(WindowEvent e) {
      }
      
      public void windowDeactivated(WindowEvent e) {
      }
      
      public void windowClosing(WindowEvent e) {
        frame.dispose();
      }
      
      public void windowClosed(WindowEvent e) {
        synchronized (closed) {
          if (closed[0])
            return;
          
          closed[0] = true;
          closed.notify();
        }
      }
      
      public void windowActivated(WindowEvent e) {
      }
    });
    
    synchronized (closed) {
      if (!closed[0])
        try {
          closed.wait();
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
    }
    
  }
  
  public static void display(JComponent component) {
    display(component, null);
  }
  
  public static void display(JComponent component, KeyListener keyListener) {
    display(component, keyListener, null);
  }
  
  public static void display(JComponent component, KeyListener keyListener, Runnable callback) {
    final JFrame frame = new JFrame();
    frame.getContentPane().add(component);
    frame.pack();
    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    frame.setVisible(true);
    
    if (keyListener != null) {
      frame.setFocusTraversalKeysEnabled(false);
      frame.addKeyListener(keyListener);
    }
    
    if (callback != null)
      callback.run();
    
    waitForClose(frame);
  }
  
  public static void displayImages(Image... images) {
    displayImages(images, null);
  }
  
  public static void displayImages(Image[] images, String[] titles) {
    final PointChooser chooser =
        new PointChooser(images, titles, new PointChooser.DefaultHandler());
    display(chooser, new KeyListener() {
      public void keyTyped(KeyEvent e) {
      }
      
      public void keyReleased(KeyEvent e) {
      }
      
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
          chooser.rotate(e.isShiftDown() ? -1 : 1);
          chooser.redraw();
        } else if (e.getKeyCode() == KeyEvent.VK_R)
          chooser.redraw();
      }
    }, new Runnable() {
      public void run() {
        chooser.setTitle();
      }
    });
  }
  
  public static void drawRect(Graphics g, double zoom, Color color, int x0, int y0, int x1, int y1) {
    int dx0 = (int) (x0 * zoom);
    int dy0 = (int) (y0 * zoom);
    int dx1 = (int) (x1 * zoom);
    int dy1 = (int) (y1 * zoom);
    
    g.setColor(color);
    for (int i = 0; i != 3; ++i)
      g.drawRect(dx0 + i, dy0 + i, dx1 - dx0 - 2 * i, dy1 - dy0 - 2 * i);
  }
  
  public static void markRectangle(BufferedImage image, final int x0, final int y0, final int x1,
      final int y1) {
    Tools.display(new PointChooser(image, new PointChooser.Handler() {
      
      public void draw(Graphics g, double zoom) {
        drawRect(g, zoom, Color.BLUE, x0, y0, x1, y1);
      }
      
      public boolean click(int button, int x, int y) {
        return false;
      }
    }));
  }
  
  public static void ensurePath(String path) throws IOException {
    int index = path.lastIndexOf('/');
    if (index != -1) {
      String dir = path.substring(0, index);
      if (dir.length() > 0)
        new File(dir).mkdirs();
    }
  }
  
}
