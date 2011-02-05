package map.properties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Scanner;

public class ImageSetProperties implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private ImageSize size;
  private ImagePosition[] positions;
  private OpticalProperties[] opticalProperties;
  private PerspectiveProperties perspectiveProperties;
  
  public ImageSetProperties(ImageSize size, ImagePosition[] positions,
      OpticalProperties[] opticalProperties, PerspectiveProperties perspectiveProperties) {
    this.size = size;
    this.positions = positions.clone();
    this.opticalProperties = opticalProperties.clone();
    this.perspectiveProperties = perspectiveProperties;
  }
  
  public ImageSetProperties(String filename) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(filename));
    Scanner scanner = new Scanner(in);
    scanner.useDelimiter("[ \t\n,;()]+");
    
    size = new ImageSize(scanner.nextInt(), scanner.nextInt());
    
    positions = new ImagePosition[scanner.nextInt()];
    for (int i = 0; i != positions.length; ++i)
      positions[i] = new ImagePosition(scanner.nextDouble(), scanner.nextDouble());
    
    opticalProperties = new OpticalProperties[scanner.nextInt()];
    int numOpticalParameters = scanner.nextInt();
    for (int i = 0; i != opticalProperties.length; ++i) {
      double[] args = new double[numOpticalParameters];
      for (int j = 0; j != numOpticalParameters; ++j)
        args[j] = scanner.nextDouble();
      
      opticalProperties[i] = new OpticalProperties(args);
    }
    
    perspectiveProperties = new PerspectiveProperties(scanner.nextDouble(), scanner.nextDouble());
  }
  
  public ImageSize getSize() {
    return size;
  }
  
  public int getNumImages() {
    return positions.length;
  }
  
  public ImagePosition getPosition(int i) {
    return positions[i];
  }
  
  public int getNumChannels() {
    return opticalProperties.length;
  }
  
  public OpticalProperties getOpticalProperties(int i) {
    return opticalProperties[i];
  }
  
  public PerspectiveProperties getPerspectiveProperties() {
    return perspectiveProperties;
  }
  
  public void print(PrintStream out) throws IOException {
    out.println(size.getSx() + ", " + size.getSy());
    out.println();
    
    out.println(positions.length);
    for (int i = 0; i != positions.length; ++i)
      out.printf("(%.4f, %.4f)\n", positions[i].getX(), positions[i].getY());
    out.println();
    
    out.println(opticalProperties.length);
    out.println(opticalProperties[0].getNumCoefs());
    for (int i = 0; i != opticalProperties.length; ++i) {
      for (int j = 0; j != opticalProperties[0].getNumCoefs(); ++j) {
        if (j != 0)
          out.print(", ");
        
        out.print(opticalProperties[i].getCoef(j));
      }
      
      out.println();
    }
    out.println();
    
    out.println(perspectiveProperties.getCoefX() + ", " + perspectiveProperties.getCoefY());
    out.println();
  }
  
  public void print(String filename) throws IOException {
    PrintStream out = new PrintStream(filename);
    print(out);
    out.close();
  }
  
}
