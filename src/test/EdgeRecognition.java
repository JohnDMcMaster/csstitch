package test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import tools.PointChooser;

import data.DataTools;
import data.Tools;
import general.Streams;
import general.collections.Pair;

public class EdgeRecognition {
  
  public static BufferedImage image;
  public static int[][] matrix;
  
  public static int sx, sy;
  
  public static void drawPixel(Graphics g, double zoom, Color color, int x, int y) {
    int sx0 = x;
    int sy0 = y;
    int sx1 = sx0 + 1;
    int sy1 = sy0 + 1;
    
    int dx0 = (int) (zoom * sx0);
    int dy0 = (int) (zoom * sy0);
    int dx1 = (int) (zoom * sx1);
    int dy1 = (int) (zoom * sy1);
    
    g.setColor(color);
    for (int i = 0; i != 3; ++i)
      g.drawRect(dx0 + i, dy0 + i, dx1 - dx0 - 2 * i, dy1 - dy0 - 2 * i);
  }
  
  //138.376351389621
  //162.52613132463694
  //163.9888845802353
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException {
    matrix = Tools.readColorImage(DataTools.DIR + "scaled-5-3-excerpt.png");
    Tools.eraseNonGreen(matrix);
    image = Tools.getGreyscaleImageFromMatrix(matrix);
    
    sx = image.getWidth();
    sy = image.getHeight();
    
    final int dx = 1420;
    final int dy = 0;
    
    final TreeSet<Pair<Integer, Integer>> vias = new TreeSet<Pair<Integer, Integer>>();
    for (Pair<Integer, Integer> via : (TreeSet<Pair<Integer, Integer>>) Streams
        .readObject(DataTools.DIR + "sel-vias/3-5.dat")) {
      int x = via.getA() - dx;
      int y = via.getB() - dy;
      if (x >= 0 && y >= 0 && x < sx && y < sy)
        vias.add(new Pair<Integer, Integer>(x, y));
    }
    
    final int EDGE_THRESHOLD = 100;
    final int MIN_EDGE_LENGTH = 5;
    
    final ArrayList<int[]> edges = new ArrayList<int[]>();
    
    for (int x = 0; x != sx; ++x) {
      boolean inEdge = false;
      int edgeStart = 0;
      for (int y = (x + 1) % 2; y < sy; y += 2) {
        boolean isEdge = matrix[y][x] < EDGE_THRESHOLD;
        if (isEdge && !inEdge)
          edgeStart = y;
        else if (!isEdge && inEdge) {
          int length = y - edgeStart;
          if (length >= MIN_EDGE_LENGTH)
            edges.add(new int[] {x, edgeStart, x, y});
        }
        
        inEdge = isEdge;
      }
      
    }
    
    Tools.display(new PointChooser(image, new PointChooser.Handler() {
      int threshold = 105;
      
      public boolean click(int button, int x, int y) {
        if (button == 1 || button == 3) {
          threshold += (button - 2);
          System.out.println("threshold " + threshold);
          return true;
        }
        
        return false;
      }
      
      public void draw(Graphics g, double zoom) {
        //int[] edge = edges.get(threshold);
        //for (int y = edge[1]; y <= edge[3]; ++y)
        //  drawPixel(g, zoom, Color.YELLOW, edge[0], y);
        
        //for (Pair<Integer, Integer> via : vias)
        //  drawPixel(g, zoom, Color.YELLOW, via.getFirst(), via.getSecond());
        
        for (int y = 0; y != sy; ++y)
          for (int x = (y + 1) % 2; x < sx; x += 2)
            if (matrix[y][x] < threshold)
              drawPixel(g, zoom, Color.YELLOW, x, y);
        
      }
    }));
  }
}
