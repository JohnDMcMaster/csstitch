/*
Copyright Christian Sattler <sattler.christian@gmail.com>
Modifications by John McMaster <JohnDMcMaster@gmail.com>
*/

package hm;

import gaussian.Utils;
import general.collections.Pair;

import java.io.IOException;

import cache.Cache;

import map.properties.StitchStackProperties;

import realmetal.BinaryImage;
import realmetal.Images;
import segment.Segment;
import segment.Sharpness;

public class BClass {
  
  public static BinaryImage computeLogLvLvLvvAdjustedStitch(int stitch, int color, double sigma,
      int scale, double threshold) throws IOException {
    BinaryImage result =
        Cache.cache("log-lvlvlvv-adjusted-stitch/%d/%d/%f/%d/%f", stitch, color, sigma, scale, threshold);
    if (result != null)
      return result;
    
    StitchStackProperties stack = Images.getStitchStackProperties();
    
    int boundary = Utils.getLvLvLvvBoundary(sigma, scale);
    Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges =
        Sharpness.computeEdges(stitch, 1, boundary, true);
    
    BinaryImage[] images = new BinaryImage[stack.getImageSetProperties(stitch).getNumImages()];
    for (int image = 0; image != images.length; ++image) {
      System.err.println("image " + image + "...");
      
      images[image] =
          Utils.computeLogLvLvLvvAdjusted(Images.getColorComponent(stitch, image, color), sigma,
              scale, threshold);
    }
    
    return Sharpness.render(scale, stack, stitch, 1, images, edges);
  }
}
