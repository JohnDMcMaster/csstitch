package other;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;

import data.Tools;

public class Reconstruction {
  
  public static class Segment {
    int id;
    boolean plus;
    int type;
    int[][] points;
    
    public Segment(String line) {
      String[] entries = line.substring(line.indexOf('[') + 1, line.indexOf(']')).split(",");
      for (int i = 0; i != entries.length; ++i)
        entries[i] = entries[i].trim();
      
      id = Integer.parseInt(entries[0]);
      plus = entries[1].charAt(1) == '+';
      type = Integer.parseInt(entries[2]);
      
      points = new int[(entries.length - 3) / 2][2];
      for (int i = 0; i != points.length; ++i)
        for (int j = 0; j != 2; ++j)
          points[i][j] = Integer.parseInt(entries[3 + 2 * i + j]);
    }
    
    public void draw(Graphics2D graphics) {
      Polygon p = new Polygon();
      for (int[] point : points)
        p.addPoint(point[0], 10000 - point[1]);
      
      graphics.setColor(Color.RED);
      graphics.fillPolygon(p);
    }
  }
  
  static ArrayList<Segment> segments = new ArrayList<Segment>();
  
  public static void main(String[] args) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader("/home/noname/di/other/segments.txt"));
    String line;
    while ((line = in.readLine()) != null)
      segments.add(new Segment(line));
    
    TreeSet<Integer> types = new TreeSet<Integer>();
    for (Segment s : segments)
      types.add(s.type);
    System.err.println(types);
    
    BufferedImage[] images = new BufferedImage[6];
    
    for (int j = 0; j != images.length; ++j) {
      BufferedImage image = new BufferedImage(10000, 10000, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      
      for (int i = 0; i != segments.size(); ++i) {
        Segment s = segments.get(i);
        if (s.type == j)
          s.draw(graphics);
      }
      
      graphics.dispose();
      images[j] = image;
    }
    
    Tools.displayImages(images);
  }
  
}
