package old.stitcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import old.storage.Point;
import old.storage.QuadTreeAccessor2;

import stitcher.StitchInfo;

public class Demosaicer {

  public static float[][] render(float x0, float y0, float x1, float y1, float scale,
      List<Point> points) {
    int sx = (int) (scale * (x1 - x0));
    int sy = (int) (scale * (y1 - y0));

    float[][] values = new float[sy][sx];
    float[][] weights = new float[sy][sx];

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        weights[y][x] = Float.MAX_VALUE;

    final float k = scale * 2;

    for (Point point : points) {
      float xx = scale * (point.x - x0);
      float yy = scale * (point.y - y0);

      for (int y = (int) (yy - k); y <= yy + k; ++y)
        for (int x = (int) (xx - k); x <= xx + k; ++x)
          if (x >= 0 && y >= 0 && x < values[0].length && y < values.length) {
            float dx = x - xx;
            float dy = y - yy;
            float dd = dx * dx + dy * dy;
            if (dd < weights[y][x]) {
              weights[y][x] = dd;
              values[y][x] = point.val;
            }
          }
    }

    return values;
  }

  public static double computeDiff(float[][] values) {
    double sum = 0;
    for (int y = 0; y != values.length; ++y)
      for (int x = 0; x != values[0].length; ++x) {
        if (values[y][x] == 0)
          continue;

        if (y + 1 != values.length && values[y + 1][x] != 0) {
          double diff = values[y + 1][x] - values[y][x];
          sum += diff * diff;
        }

        if (x + 1 != values[0].length && values[y][x + 1] != 0) {
          double diff = values[y][x + 1] - values[y][x];
          sum += diff * diff;
        }
      }

    return sum;
  }

  public static BufferedImage getImage(float[][][] values) {
    BufferedImage image = new BufferedImage(values[0][0].length, values[0].length,
        BufferedImage.TYPE_INT_RGB);
    WritableRaster raster = image.getRaster();

    int[] pixel = new int[3];
    for (int y = 0; y != values[0].length; ++y)
      for (int x = 0; x != values[0][0].length; ++x) {
        for (int i = 0; i != 3; ++i)
          pixel[i] = (int) (values[i][y][x]);
        raster.setPixel(x, y, pixel);
      }

    return image;
  }

  public static BufferedImage produceImage(QuadTreeAccessor2 accessor, final double[] factors,
      float scale, float x0, float y0, float x1, float y1) throws IOException {

    final TreeMap<Integer, ArrayList<Point>[]> map = new TreeMap<Integer, ArrayList<Point>[]>();

    final int[] counter = new int[1];

    accessor.selectRectangle(new QuadTreeAccessor2.SimpleHandler() {
      public void handle(ByteBuffer file) {
        Point point = new Point();
        point.x = file.getFloat();
        point.y = file.getFloat();
        point.val = file.getFloat();
        point.flags = file.getInt();

        int s = ((point.flags % 4) + 4) % 4;
        int c = (s / 2) + (s % 2);
        int uv = (point.flags >> 2) % 64;

        point.val = (float) (factors[s] * point.val);

        if (!map.containsKey(uv)) {
          ArrayList<Point>[] lists = new ArrayList[3];
          for (int i = 0; i != 3; ++i)
            lists[i] = new ArrayList<Point>();
          map.put(uv, lists);
        }

        map.get(uv)[c].add(point);

        ++counter[0];
      }
    }, (int) x0, (int) y0, (int) x1 + 1, (int) y1 + 1);

    System.out.println(counter[0] + " pixels loaded");

    double maxDiff = 0;
    ArrayList<Point>[] bestLists = null;

    for (ArrayList<Point>[] lists : map.values()) {
      int numPoints = 0;
      for (int j = 0; j != 3; ++j)
        numPoints += lists[j].size();

      if (numPoints < 0.5 * (x1 - x0) * (y1 - y0))
        continue;

      float[][][] values = new float[3][][];
      double diff = 0;

      for (int j = 0; j != 3; ++j) {
        values[j] = render(x0, y0, x1, y1, 1, lists[j]);
        diff += computeDiff(values[j]);
      }

      System.out.println("diff " + diff);

      if (diff > maxDiff) {
        maxDiff = diff;
        bestLists = lists;
      }
    }

    float[][][] values = new float[3][][];
    for (int j = 0; j != 3; ++j)
      values[j] = render(x0, y0, x1, y1, scale, bestLists[j]);
    return getImage(values);
  }

  public static class Panel extends JPanel implements KeyListener, MouseInputListener {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private float scale = 1;

    private QuadTreeAccessor2[] accessors;
    private double[][] factors;

    private BufferedImage[] images;
    private float[][] coords;

    private int cursor;

    private int[] selection = new int[2];

    public Panel() throws IOException {
      accessors = new QuadTreeAccessor2[StitchInfo.NUM_STITCHES];
      factors = new double[StitchInfo.NUM_STITCHES][];

      for (int i = 0; i != StitchInfo.NUM_STITCHES; ++i) {
        accessors[i] = new QuadTreeAccessor2(StitchInfo.getFilename(i));
        factors[i] = StitchInfo.getFactors(i);
      }

      images = new BufferedImage[StitchInfo.NUM_STITCHES];
      coords = new float[StitchInfo.NUM_STITCHES][2];

      for (int i = 0; i != images.length; ++i)
        renderImage(i);

      Dimension dimension = new Dimension(WIDTH, HEIGHT);
      setMinimumSize(dimension);
      setMaximumSize(dimension);
      setSize(dimension);
      setPreferredSize(dimension);

      addMouseListener(this);

      final JFrame frame = new JFrame();
      frame.getContentPane().add(this);
      frame.pack();

      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setFocusTraversalKeysEnabled(false);

      frame.addKeyListener(this);

      frame.setVisible(true);
    }

    private void renderImage(int i) throws IOException {
      images[i] = produceImage(accessors[i], factors[i], scale, coords[i][0], coords[i][1],
          coords[i][0] + WIDTH / scale, coords[i][1] + HEIGHT / scale);
    }

    public void paintComponent(Graphics g) {
      Rectangle r = g.getClipBounds();
      g.drawImage(images[cursor], r.x, r.y, r.width, r.height, r.x, r.y, r.width, r.height, null);

      g.setColor(Color.RED);
      g.drawLine(selection[0] - 16, selection[1], selection[0] + 16, selection[1]);
      g.drawLine(selection[0], selection[1] - 16, selection[0], selection[1] + 16);
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
      int c = e.getKeyCode();

      if (c == KeyEvent.VK_TAB) {
        if (e.isShiftDown())
          --cursor;
        else
          ++cursor;
        cursor = (cursor + images.length) % images.length;

        invalidate();
        repaint();
      } else if (c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT || c == KeyEvent.VK_UP
          || c == KeyEvent.VK_DOWN) {

        float factor = 1 / scale;
        if (e.isControlDown())
          factor *= 100;
        if (e.isShiftDown())
          factor *= 10;

        if (c == KeyEvent.VK_LEFT)
          coords[cursor][0] -= factor;
        else if (c == KeyEvent.VK_RIGHT)
          coords[cursor][0] += factor;
        else if (c == KeyEvent.VK_UP)
          coords[cursor][1] -= factor;
        else
          coords[cursor][1] += factor;

        try {
          renderImage(cursor);
        } catch (IOException e1) {
          throw new RuntimeException(e1);
        }

        invalidate();
        repaint();
      } else if (c == KeyEvent.VK_PAGE_UP || c == KeyEvent.VK_PAGE_DOWN) {
        for (int i = 0; i != images.length; ++i) {
          coords[i][0] += selection[0] / scale;
          coords[i][1] += selection[1] / scale;
        }

        if (c == KeyEvent.VK_PAGE_UP)
          scale *= 2;
        else
          scale /= 2;

        for (int i = 0; i != images.length; ++i) {
          coords[i][0] -= selection[0] / scale;
          coords[i][1] -= selection[1] / scale;
        }

        for (int i = 0; i != images.length; ++i)
          try {
            renderImage(i);
          } catch (IOException e1) {
            throw new RuntimeException(e1);
          }

        invalidate();
        repaint();
      }

      System.out.println(e);
    }

    public void keyReleased(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
      if (e.getButton() == 1) {
        selection[0] = e.getX();
        selection[1] = e.getY();

        invalidate();
        repaint();
      }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

  }

  public static void main(String[] args) throws IOException {
    Panel panel = new Panel();
  }

}
