package tools;

import general.collections.Pair;
import general.Statistics;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;

import data.Tools;

import tools.PointChooser.Handler;

public class RectLister {

  private PointChooser chooser;
  private TreeMap<Pair<Integer, Integer>, Rectangle> rectangles = new TreeMap<Pair<Integer, Integer>, Rectangle>();
  private Pair<Integer, Integer> cursor = null;

  private class ClickHandler implements PointChooser.Handler {

    public boolean click(int button, int x, int y) {
      Pair<Integer, Integer> p = new Pair<Integer, Integer>(x, y);

      if (button == 2) {
        System.out.println("current state:");
        System.out.println("cursor = " + cursor);
        System.out.println("number of rectangles = " + rectangles.size());
        Statistics.printMap(rectangles);
      }

      if (button == 3 && cursor != null) {
        cursor = null;
        return true;
      }

      if (button == 3 && rectangles.containsKey(p)) {
        rectangles.remove(p);
        return true;
      }

      if (button == 1 && cursor != null) {
        int xx = cursor.getA();
        int yy = cursor.getB();
        int x0 = Math.min(x, xx);
        int y0 = Math.min(y, yy);
        int x1 = Math.max(x, xx) + 1;
        int y1 = Math.max(y, yy) + 1;
        rectangles.put(new Pair<Integer, Integer>(x0, y0), new Rectangle(x0, y0, x1 - x0, y1 - y0));
        cursor = null;
        return true;
      }

      if (button == 1 && cursor == null) {
        cursor = p;
        return true;
      }

      return false;
    }

    public void draw(Graphics g, double zoom) {
      Rectangle clip = g.getClipBounds();
      g.setColor(Color.BLUE);
      for (Rectangle r : rectangles.values()) {
        int x0 = (int) (r.x * zoom);
        int y0 = (int) (r.y * zoom);
        int x1 = (int) ((r.width + r.x) * zoom);
        int y1 = (int) ((r.height + r.y) * zoom);

        Rectangle dr = new Rectangle(x0, y0, x1 - x0, y1 - y0);
        if (clip.intersects(dr))
          g.drawRect(dr.x, dr.y, dr.width, dr.height);
      }

      if (cursor != null) {
        g.setColor(Color.RED);

        int x = (int) (cursor.getA() * zoom);
        int y = (int) (cursor.getB() * zoom);
        g.drawLine(x - 3, y, x + 3, y);
        g.drawLine(x, y - 3, x, y + 3);
      }
    }
  }

  public RectLister(Image image, Rectangle[] rects) {
    if (rects != null)
      for (Rectangle r : rects)
        rectangles.put(new Pair<Integer, Integer>(r.x, r.y), r);

    chooser = new PointChooser(image, new ClickHandler());
  }

  public Map<Pair<Integer, Integer>, Rectangle> getRectangleMap() {
    return Collections.unmodifiableMap(rectangles);
  }

  public Rectangle[] getRectangles() {
    return rectangles.values().toArray(new Rectangle[] {});
  }

  public PointChooser getChooser() {
    return chooser;
  }

  public static Rectangle[] doSelection(Image image) {
    return doSelection(image, new Rectangle[] {});
  }

  public static Rectangle[] doSelection(Image image, Rectangle[] rects) {
    RectLister lister = new RectLister(image, rects);

    PointChooser chooser = lister.getChooser();
    Dimension size = new Dimension(512, 378);
    chooser.setPreferredSize(size);
    chooser.setVisible(true);

    Tools.display(chooser);

    return lister.getRectangles();
  }

}
