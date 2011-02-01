package realmetal;

import general.collections.Pair;

import java.io.IOException;

import map.properties.StitchStackProperties;
import metal.MetalTools;

import operations.image.ImageOpsDouble;

import segment.Segment;
import segment.Sharpness;
import stitcher.StitchInfo;

import cache.Cache;

public class PolyDirt {
  
  public static BinaryImage findDirt(final int image, final int channel) throws IOException {
    BinaryImage result = Cache.cache("poly-dirt/%d/%d", image, channel);
    //if (result != null)
    //  return result;
    
    final int stitch = 0;
    
    final int sx = StitchInfo.IMAGE_DIMENSIONS[stitch][0];
    final int sy = StitchInfo.IMAGE_DIMENSIONS[stitch][1];
    
    result = new BinaryImage(sx, sy);
    final double[] filter = new double[] {1. / 3, 1. / 3, 1. / 3};
    
    double[][][] components = Decomp.computeComponents(stitch, image, channel, 5);
    BinaryImage[] masks = new BinaryImage[2];
    for (int i = 0; i != 2; ++i)
      masks[i] =
          new BinaryImage(MetalTools.binarize(ImageOpsDouble.convolve(components[i], filter, -1),
              -0.45, false));
    
    return BinaryImageTools.inflate8(BinaryImageTools.embedComponent(
        BinaryImageTools.or(masks[0], masks[1]), channel));
  }
  
  public static BinaryImage findDirtStitch() throws IOException {
    BinaryImage result = Cache.cache("stitch-poly-dirt");
    if (result != null)
      return result;
    
    StitchStackProperties stack = Images.getStitchStackProperties();
    
    int sx = stack.getX1() - stack.getX0();
    int sy = stack.getY1() - stack.getY0();
    
    result = new BinaryImage(sx, sy);
    
    for (int channel = 0; channel != 4; ++channel) {
      Pair<Segment<Pair<Double, Double>>, Pair<Integer, Integer>>[] edges =
          Sharpness.computeEdges(0, channel / 2 + channel % 2, 0, true);
      
      BinaryImage[] images = new BinaryImage[stack.getImageSetProperties(0).getNumImages()];
      for (int image = 0; image != images.length; ++image)
        images[image] = findDirt(image, channel);
      
      BinaryImage mask = Sharpness.render(1, stack, 0, channel, images, edges);
      result = BinaryImageTools.or(result, mask);
    }
    
    return result;
  }
  
}
