package realmetal;

import general.Streams;
import general.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.util.TreeSet;

import map.Map;
import map.properties.StitchStackProperties;

import stitcher.StitchInfo;

import cache.Cache;

public class Vias {
  
  public static TreeSet<Pair<Integer, Integer>> getSelectedVias(final int image) throws IOException {
    TreeSet<Pair<Integer, Integer>> result = Cache.cache("vias-selected/%d-%d", image);
    if (result != null)
      return result;
    
    final String path = "sel-vias/" + image + ".dat";
    if (!new File(path).exists())
      return null;
    
    return Streams.readObject(path);
  }
  
  public static TreeSet<Pair<Integer, Integer>> clipSelectedVias(final int image, final int sx,
      final int sy, final int clip) throws IOException {
    final TreeSet<Pair<Integer, Integer>> vias = getSelectedVias(image);
    for (final Pair<Integer, Integer> p : vias.toArray(new Pair[] {}))
      if (!(p.getA() >= clip && p.getB() >= clip && p.getA() < sx - clip && p.getB() < sy - clip))
        vias.remove(p);
    
    return vias;
  }
  
  public static TreeSet<Pair<Float, Float>> getMappedSelectedVias() throws IOException {
    TreeSet<Pair<Float, Float>> result = Cache.cache("vias-selected-mapped");
    if (result != null)
      return result;
    
    final int stitch = 0;
    StitchStackProperties stack = Images.getStitchStackProperties();
    
    result = new TreeSet<Pair<Float, Float>>();
    
    for (int image = 0; image != StitchInfo.NUM_IMAGES[stitch]; ++image) {
      final TreeSet<Pair<Integer, Integer>> vias = getSelectedVias(image);
      final Map m = map.Utils.getMapFromStack(stack, stitch, image, 1, true);
      
      if (vias != null)
        for (final Pair<Integer, Integer> via : getSelectedVias(image)) {
          double[] coords = new double[] {via.getA() + 1, via.getB()};
          m.map(coords, coords);
          result.add(new Pair<Float, Float>((float) coords[0], (float) coords[1]));
        }
    }
    
    return result;
  }
  
  public static TreeSet<Pair<Float, Float>> getUnmappedSelectedVias(final int stitch,
      final int image, final boolean good) throws IOException {
    TreeSet<Pair<Float, Float>> result =
        Cache.cache("vias-selected-unmapped/%3$b/%1$d/%2$d", stitch, image, good);
    if (result != null)
      return result;
    
    if (good == true)
      throw new RuntimeException("selection of good vias currently unsupported");
    
    Map m = map.Utils.getMapFromStack(Images.getStitchStackProperties(), stitch, image, 1, true);
    
    final float d = 16;
    
    final int sx = StitchInfo.IMAGE_DIMENSIONS[stitch][0];
    final int sy = StitchInfo.IMAGE_DIMENSIONS[stitch][1];
    
    result = new TreeSet<Pair<Float, Float>>();
    
    for (final Pair<Float, Float> via : getMappedSelectedVias()) {
      double[] coords = new double[] {via.getA(), via.getB()};
      m.unmap(coords, coords);
      
      if (coords[0] >= d && coords[1] >= d && coords[0] < sx - d && coords[1] < sy - d
          && (!good || false)) // HACK
        result.add(new Pair<Float, Float>((float) coords[0], (float) coords[1]));
    }
    
    return result;
  }
}
