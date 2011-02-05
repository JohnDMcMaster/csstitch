package tools;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class PointChooser extends JScrollPane {
  private static double[] ZOOM_LEVELS = {0.0625, 0.0875, 0.125, 0.175, 0.25, 0.35, 0.5, 0.7, 1.0,
      1.4, 2.0, 2.8, 4, 5.6, 8, 11.2, 16, 22.4, 32, 44.8, 64};
  
  private PointChooser self;
  private JViewport viewport;
  private ImagePanel panel;
  private Image[] images;
  private String[] titles;
  private int cursor;
  
  private Handler handler;
  
  private int zoomLevelLow;
  private int zoomLevelHigh;
  private double zoom = 1.0;
  
  public static interface Handler {
    public boolean click(int button, int x, int y);
    
    public void draw(Graphics g, double zoom);
  }
  
  public static class DefaultHandler implements Handler {
    public boolean click(int button, int x, int y) {
      return false;
    }
    
    public void draw(Graphics g, double zoom) {
    }
  }
  
  private class ImagePanel extends JPanel {
    public ImagePanel() {
      addMouseListener(new Listener());
      addMouseWheelListener(new WheelListener());
    }
    
    public void paintComponent(Graphics g) {
      Rectangle r = g.getClipBounds();
      
      int dx0 = r.x;
      int dy0 = r.y;
      int dx1 = dx0 + r.width;
      int dy1 = dy0 + r.height;
      
      int sx0 = (int) (dx0 / zoom);
      int sy0 = (int) (dy0 / zoom);
      int sx1 = (int) (dx1 / zoom) + 1;
      int sy1 = (int) (dy1 / zoom) + 1;
      
      dx0 = (int) (sx0 * zoom);
      dy0 = (int) (sy0 * zoom);
      dx1 = (int) (sx1 * zoom);
      dy1 = (int) (sy1 * zoom);
      
      g.drawImage(images[cursor], dx0, dy0, dx1, dy1, sx0, sy0, sx1, sy1, null);
      
      handler.draw(g, zoom);
    }
  }
  
  private class Listener implements MouseListener {
    public void mouseClicked(MouseEvent e) {
      if (handler.click(e.getButton(), (int) (e.getX() / zoom), (int) (e.getY() / zoom))) {
        self.getViewport().invalidate();
        self.getViewport().repaint();
      }
    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
    }
    
    public void mouseReleased(MouseEvent e) {
    }
  }
  
  private class WheelListener implements MouseWheelListener {
    public void mouseWheelMoved(MouseWheelEvent e) {
      int r = e.getWheelRotation();
      int level = -1;
      
      for (; r < 0; ++r)
        level = ++zoomLevelLow;
      
      for (; r > 0; --r)
        level = --zoomLevelHigh;
      
      if (level < 0)
        level = 0;
      
      if (level >= ZOOM_LEVELS.length)
        level = ZOOM_LEVELS.length - 1;
      
      setZoom(ZOOM_LEVELS[level], e.getPoint().x, e.getPoint().y);
    }
  }
  
  public PointChooser(Image image) {
    this(image, new DefaultHandler());
  }
  
  public PointChooser(Image image, Handler handler) {
    this(new Image[] {image}, handler);
  }
  
  public PointChooser(Image[] images, Handler handler) {
    this(images, null, handler);
  }
  
  public PointChooser(Image[] images, String[] titles, Handler handler) {
    self = this;
    this.images = images.clone();
    if (titles != null)
      this.titles = titles.clone();
    this.handler = handler;
    
    panel = new ImagePanel();
    viewport = getViewport();
    setViewportView(panel);
    
    findZoomLevel();
    setSize();
    setTitle();
  }
  
  public void setImage(Image[] images) {
    this.images = images;
    if (cursor >= images.length)
      cursor = 0;
  }
  
  public double getZoom() {
    return zoom;
  }
  
  public void setZoom(double zoom) {
    setZoom(zoom, 0, 0);
  }
  
  public void setZoom(double zoom, int x, int y) {
    int cx = x + panel.getX();
    int cy = y + panel.getY();
    
    int dx = (int) (x / this.zoom * zoom);
    int dy = (int) (y / this.zoom * zoom);
    
    this.zoom = zoom;
    findZoomLevel();
    setSize();
    
    panel.setLocation(cx - dx, cy - dy);
    
    invalidate();
    repaint();
  }
  
  public void focus(int x, int y) {
    int dx = (int) (x / this.zoom);
    int dy = (int) (y / this.zoom);
    
    Dimension size = viewport.getSize();
    
    panel.setLocation(size.width / 2 - x, size.height / 2 - y);
  }
  
  public void rotate(int c) {
    cursor += c;
    cursor = ((cursor % images.length) + images.length) % images.length;
    setTitle();
  }
  
  public void redraw() {
    invalidate();
    repaint();
  }
  
  private void findZoomLevel() {
    for (zoomLevelLow = 0; zoomLevelLow < ZOOM_LEVELS.length; ++zoomLevelLow) {
      if (ZOOM_LEVELS[zoomLevelLow] == zoom) {
        zoomLevelHigh = zoomLevelLow;
        break;
      }
      
      if (ZOOM_LEVELS[zoomLevelLow] > zoom) {
        zoomLevelHigh = zoomLevelLow;
        --zoomLevelLow;
        break;
      }
    }
  }
  
  public void setTitle() {
    if (titles != null) {
      Component frame = SwingUtilities.getRoot(this);
      if (frame != null)
        ((JFrame) frame).setTitle(titles[cursor]);
    }
  }
  
  private void setSize() {
    Dimension size =
        new Dimension((int) (images[cursor].getWidth(null) * zoom) + 1,
            (int) (images[cursor].getHeight(null) * zoom) + 1);
    
    panel.setPreferredSize(size);
    panel.setMinimumSize(size);
    panel.setMaximumSize(size);
    panel.setSize(size);
  }
}
