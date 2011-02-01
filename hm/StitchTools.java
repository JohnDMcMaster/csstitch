package hm;

import general.collections.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import map.Utils;
import map.properties.ImagePosition;
import map.properties.ImageSetProperties;
import map.properties.OpticalProperties;

public class StitchTools {
  
  public static ArrayList<double[]> readControlPoints(File file) throws IOException {
    ArrayList<double[]> result = new ArrayList<double[]>();
    
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    while ((line = in.readLine()) != null) {
      if (!line.startsWith("c"))
        continue;
      
      String[] entries = line.split(" ");
      
      int n0 = Integer.valueOf(entries[1].substring(1));
      int n1 = Integer.valueOf(entries[2].substring(1));
      
      double x0 = Double.valueOf(entries[3].substring(1));
      double y0 = Double.valueOf(entries[4].substring(1));
      
      double x1 = Double.valueOf(entries[5].substring(1));
      double y1 = Double.valueOf(entries[6].substring(1));
      
      double[] point;
      if (n0 == 0 && n1 == 1)
        point = new double[] {x0, y0, x1, y1};
      else if (n0 == 1 && n1 == 0)
        point = new double[] {x1, y1, x0, y0};
      else
        throw new IOException("encountered more than 2 images");
      
      result.add(point);
    }
    
    return result;
  }
  
  public static TreeMap<Pair<String, String>, ArrayList<double[]>>
      readControlPointsMulti(File file) throws IOException {
    TreeMap<Pair<String, String>, ArrayList<double[]>> result =
        new TreeMap<Pair<String, String>, ArrayList<double[]>>();
    
    ArrayList<String> images = new ArrayList<String>();
    
    Pattern imagePattern = Pattern.compile(".*n\"([^\"]*)\".*");
    
    BufferedReader in = new BufferedReader(new FileReader(file));
    String line;
    while ((line = in.readLine()) != null) {
      if (line.startsWith("i")) {
        Matcher matcher = imagePattern.matcher(line);
        if (!matcher.matches())
          throw new IOException("cannot read image line: " + line);
        
        images.add(matcher.group(1));
      } else if (line.startsWith("c")) {
        String[] entries = line.split(" ");
        
        int n0 = Integer.valueOf(entries[1].substring(1));
        int n1 = Integer.valueOf(entries[2].substring(1));
        
        double x0 = Double.valueOf(entries[3].substring(1));
        double y0 = Double.valueOf(entries[4].substring(1));
        
        double x1 = Double.valueOf(entries[5].substring(1));
        double y1 = Double.valueOf(entries[6].substring(1));
        
        double[] point;
        if (!(n0 < n1)) {
          int nn = n0;
          n0 = n1;
          n1 = nn;
          
          double xx = x0;
          x0 = x1;
          x1 = xx;
          
          double yy = y0;
          y0 = y1;
          y1 = yy;
        }
        
        Pair<String, String> key = new Pair<String, String>(images.get(n0), images.get(n1));
        ArrayList<double[]> list = result.get(key);
        if (list == null) {
          list = new ArrayList<double[]>();
          result.put(key, list);
        }
        
        point = new double[] {x0, y0, x1, y1};
        list.add(point);
      }
    }
    
    return result;
  }
  
  public static <A, B> TreeMap<B, A> reverse(TreeMap<A, B> map) {
    TreeMap<B, A> result = new TreeMap<B, A>();
    for (Entry<A, B> entry : map.entrySet())
      result.put(entry.getValue(), entry.getKey());
    
    return result;
  }
  
  public static <A> TreeMap<A, Integer> getIndices(Set<A> set) {
    TreeMap<A, Integer> result = new TreeMap<A, Integer>();
    
    int i = 0;
    for (A key : set)
      result.put(key, i++);
    
    return result;
  }
  
  public static <A> TreeMap<Integer, A> getIndexLookup(Set<A> set) {
    TreeMap<Integer, A> result = new TreeMap<Integer, A>();
    
    int i = 0;
    for (A key : set)
      result.put(i++, key);
    
    return result;
  }
  
  public static TreeSet<Integer> scaleSelection(Set<Integer> selection) {
    TreeSet<Integer> result = new TreeSet<Integer>();
    for (int i : selection) {
      result.add(2 * i + 0);
      result.add(2 * i + 1);
    }
    
    return result;
  }
  
  public static ImageSetProperties normalizeImageSetProperties(ImageSetProperties imageSet) {
    double[] boundary = Utils.getBoundary(imageSet);
    
    ImagePosition[] positions = new ImagePosition[imageSet.getNumImages()];
    for (int i = 0; i != positions.length; ++i) {
      ImagePosition p = imageSet.getPosition(i);
      positions[i] = new ImagePosition(p.getX() - boundary[0], p.getY() - boundary[1]);
    }
    
    OpticalProperties[] opticalProperties = new OpticalProperties[imageSet.getNumChannels()];
    for (int i = 0; i != opticalProperties.length; ++i)
      opticalProperties[i] = imageSet.getOpticalProperties(i);
    
    return new ImageSetProperties(imageSet.getSize(), positions, opticalProperties,
        imageSet.getPerspectiveProperties());
  }
  
  public static TreeMap<Integer, String> readLookup(String filename) throws IOException {
    TreeMap<Integer, String> result = new TreeMap<Integer, String>();
    
    Pattern pattern = Pattern.compile("\\((.*), (.*)\\)");
    
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    String line;
    
    while ((line = reader.readLine()) != null) {
      Matcher matcher = pattern.matcher(line);
      matcher.matches();
      result.put(Integer.parseInt(matcher.group(1)), matcher.group(2));
    }
    
    return result;
  }
}
