package map;

import java.io.IOException;

import realmetal.Images;
import map.properties.ImagePosition;
import map.properties.ImageSetProperties;
import map.properties.ImageSize;
import map.properties.OpticalProperties;
import map.properties.PerspectiveProperties;
import map.properties.StitchStackProperties;

public class Utils {
  
  public static double[] initBoundary() {
    return new double[] {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
  }
  
  public static void getBoundary(ImageSize size, Map map, double[] boundary) {
    int sx = size.getSx();
    int sy = size.getSy();
    
    double[] in = new double[2];
    double[] out = new double[2];
    
    for (int y = 0; y <= sy; ++y) {
      for (int i = 0; i != 2; ++i) {
        in[0] = i * sx;
        in[1] = y;
        
        map.map(in, out);
        
        for (int j = 0; j != 2; ++j) {
          boundary[0 + j] = Math.min(boundary[0 + j], out[j] - 0.1);
          boundary[2 + j] = Math.max(boundary[2 + j], out[j] + 0.1);
        }
      }
    }
    
    for (int x = 0; x <= sx; ++x) {
      for (int i = 0; i != 2; ++i) {
        in[0] = x;
        in[1] = i * sy;
        
        map.map(in, out);
        
        for (int j = 0; j != 2; ++j) {
          boundary[0 + j] = Math.min(boundary[0 + j], out[j] - 0.1);
          boundary[2 + j] = Math.max(boundary[2 + j], out[j] + 0.1);
        }
      }
    }
  }
  
  public static double[] getBoundary(ImageSize size, Map map) {
    double[] boundary = initBoundary();
    getBoundary(size, map, boundary);
    return boundary;
  }
  
  // only valid for non-rotating maps
  public static double[] getInwardsBoundary(ImageSize size, Map map, double distance) {
    int sx = size.getSx();
    int sy = size.getSy();
    
    double[] boundary =
        new double[] {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY};
    
    double[] in = new double[2];
    double[] out = new double[2];
    
    for (int y = 0; y <= sy; ++y) {
      for (int i = 0; i != 2; ++i) {
        in[0] = i == 0 ? distance : sx - distance;
        in[1] = y;
        
        map.map(in, out);
        
        if (i == 0)
          boundary[0] = Math.max(boundary[0], out[0] + 0.1);
        else
          boundary[2] = Math.min(boundary[2], out[0] - 0.1);
      }
    }
    
    for (int x = 0; x <= sx; ++x) {
      for (int i = 0; i != 2; ++i) {
        in[0] = x;
        in[1] = i == 0 ? distance : sy - distance;
        
        map.map(in, out);
        
        if (i == 0)
          boundary[1] = Math.max(boundary[1], out[1] + 0.1);
        else
          boundary[3] = Math.min(boundary[3], out[1] - 0.1);
      }
    }
    
    return boundary;
  }
  
  public static Map getMap(ImageSize size, OpticalProperties opticalProperties, PerspectiveProperties perspectiveProperties, AffineTransform transform) {
    if (opticalProperties.getNumCoefs() == 2)
      return new SampleMap2(size, opticalProperties, perspectiveProperties, transform);
    
    return new SampleMap(size, opticalProperties, perspectiveProperties, transform);
  }
  
  public static Map getMapFromImageSet(ImageSetProperties imageSet, int image, int channel,
      AffineTransform transform) {
    ImagePosition position = imageSet.getPosition(image);
    transform = transform.after(AffineTransform.getTranslation(position.getX(), position.getY()));
    return getMap(imageSet.getSize(), imageSet.getOpticalProperties(channel), imageSet.getPerspectiveProperties(), transform);
  }
  
  public static Map getMapFromStack(StitchStackProperties stack, int stitch, int image,
      int channel, boolean adjust) {
    ImageSetProperties imageSet = stack.getImageSetProperties(stitch);
    AffineTransform transform = stack.getTransform(stitch);
    
    if (adjust)
      transform = AffineTransform.getTranslation(-stack.getX0(), -stack.getY0()).after(transform);
    
    return getMapFromImageSet(imageSet, image, channel, transform);
  }
  
  public static Map[] getMapsFromStack(StitchStackProperties stack, int stitch, int channel,
      boolean adjust) {
    Map[] maps = new Map[stack.getImageSetProperties(stitch).getNumImages()];
    for (int image = 0; image != maps.length; ++image)
      maps[image] = map.Utils.getMapFromStack(stack, stitch, image, channel, adjust);
    
    return maps;
  }
  
  public static void getBoundary(ImageSetProperties imageSet, AffineTransform transform,
      double[] boundary) {
    ImageSize size = imageSet.getSize();
    
    for (int image = 0; image != imageSet.getNumImages(); ++image) {
      for (int channel = 0; channel != imageSet.getNumChannels(); ++channel) {
        Map map = Utils.getMapFromImageSet(imageSet, image, channel, transform);
        getBoundary(size, map, boundary);
      }
    }
  }
  
  public static double[] getBoundary(ImageSetProperties imageSet, AffineTransform transform) {
    double[] boundary = initBoundary();
    getBoundary(imageSet, transform, boundary);
    return boundary;
  }
  
  public static double[] getBoundary(ImageSetProperties imageSet) {
    return getBoundary(imageSet, AffineTransform.ID);
  }
  
  public static double[] getBoundary(StitchStackProperties stack) {
    double[] boundary = initBoundary();
    for (int stitch = 0; stitch != stack.getNumImageSets(); ++stitch)
      getBoundary(stack.getImageSetProperties(stitch), stack.getTransform(stitch), boundary);
    
    return boundary;
  }
  
  public static StitchStackProperties normalize(StitchStackProperties stack) {
    double[] boundaries = getBoundary(stack);
    AffineTransform translation = AffineTransform.getTranslation(-boundaries[0], -boundaries[1]);
    
    AffineTransform[] transforms = new AffineTransform[stack.getNumImageSets()];
    for (int i = 0; i != transforms.length; ++i)
      transforms[i] = translation.after(stack.getTransform(i));
    
    System.out.println("Bad compile");
    System.exit(1);
	//return new StitchStackProperties(stack.getImageSetProperties().toArray(
    //    new ImageSetProperties[] {}), transforms);
  	return null;
  }
  
  public static int[] getSize(StitchStackProperties stack) {
    double[] boundaries = getBoundary(stack);
    
    int[] size = new int[2];
    for (int i = 0; i != 2; ++i)
      size[i] = (int) (boundaries[2 + i]) + 1;
    
    return size;
  }
  
}
