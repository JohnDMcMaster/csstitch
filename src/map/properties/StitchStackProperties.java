package map.properties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import map.AffineTransform;

public class StitchStackProperties implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private ImageSetProperties[] imageSetProperties;
  private AffineTransform[] transforms;
  private int x0, y0, x1, y1;
  
  public StitchStackProperties(String[] filenames, String transformsFilename) throws IOException {
    imageSetProperties = new ImageSetProperties[filenames.length];
    for (int i = 0; i != imageSetProperties.length; ++i)
      imageSetProperties[i] = new ImageSetProperties(filenames[i]);
    
    BufferedReader in = new BufferedReader(new FileReader(transformsFilename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()\\[\\]]+");
    
    x0 = scanner.nextInt();
    y0 = scanner.nextInt();
    x1 = scanner.nextInt();
    y1 = scanner.nextInt();
    
    transforms = new AffineTransform[imageSetProperties.length];
    for (int i = 0; i != imageSetProperties.length; ++i)
      transforms[i] =
          new AffineTransform(scanner.nextDouble(), scanner.nextDouble(), scanner.nextDouble(),
              scanner.nextDouble(), scanner.nextDouble(), scanner.nextDouble());
    
    in.close();
  }
  
  public StitchStackProperties(ImageSetProperties[] imageSetProperties,
      AffineTransform[] transforms, int x0, int y0, int x1, int y1) {
    if (imageSetProperties.length != transforms.length)
      throw new IllegalArgumentException();
    
    this.imageSetProperties = imageSetProperties.clone();
    this.transforms = transforms.clone();
    
    this.x0 = x0;
    this.y0 = y0;
    this.x1 = x1;
    this.y1 = y1;
  }
  
  public int getNumImageSets() {
    return imageSetProperties.length;
  }
  
  public ImageSetProperties getImageSetProperties(int i) {
    return imageSetProperties[i];
  }
  
  public List<ImageSetProperties> getImageSetProperties() {
    return Collections.unmodifiableList(Arrays.asList(imageSetProperties));
  }
  
  public AffineTransform getTransform(int i) {
    return transforms[i];
  }
  
  public int getX0() {
    return x0;
  }
  
  public int getY0() {
    return y0;
  }
  
  public int getX1() {
    return x1;
  }
  
  public int getY1() {
    return y1;
  }
  
  public int getCorner(int i) {
    return i < 2 ? (i == 0 ? x0 : y0) : (i == 2 ? x1 : y1);
  }
  
  public int[] getCorners() {
    return new int[] {x0, y0, x1, y1};
  }
  
  public void print(PrintStream out) throws IOException {
    out.println(x0 + ", " + y0 + ", " + x1 + ", " + y1);
    out.println();
    
    for (AffineTransform transform : transforms)
      out.println(transform.toString());
  }
  
  public void print(String filename) throws IOException {
    PrintStream out = new PrintStream(filename);
    print(out);
    out.close();
  }
  
}
