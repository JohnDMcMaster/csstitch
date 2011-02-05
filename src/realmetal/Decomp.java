package realmetal;

import java.io.IOException;

import operations.image.ImageOpsDouble;

import cache.Cache;

public final class Decomp {
  
  public static double[][][] computeComponents(final int stitch, final int image,
      final int component, final int minSegmentLength) throws IOException {
    double[][][] result =
        Cache.cache("decomp/%d/%d/%d/%d", stitch, image, component, minSegmentLength);
    if (result != null)
      return result;
    
    final double[][] img = Images.getImageComponent(stitch, image, component);
    return Decomposer.decompose(ImageOpsDouble.log(img), 0, minSegmentLength, false);
  }
  
  public static double[][] computeFiltered(final int stitch, final int image,
      final int component, final int minSegmentLength) throws IOException {
    double[][][] components = computeComponents(stitch, image, component, minSegmentLength);
    return ImageOpsDouble.exp(ImageOpsDouble.add(components[0], components[1]));
  }
  
}
