package old.block_compression;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageStreams {
  
  public static void link(ImageInput input, ImageOutput output) throws IOException {
    input.init();
    
    int sx = input.getSX();
    int sy = input.getSY();
    
    output.init(sx, sy);
    
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        output.write(input.read());
  }
  
  public static float[][] read(ImageInput input) throws IOException {
    return new ImageReader(input).getImage();
  }
  
  public static void write(ImageOutput output, float[][] image) throws IOException {
    new ImageWriter(image, output);
  }
  
  public static int[] getExponentProperties(float[][] image) {
    int minExponent = Integer.MAX_VALUE;
    int maxExponent = Integer.MIN_VALUE;
    for (float[] row : image)
      for (float value : row) {
        if (!(value > 0 && value <= Float.MAX_VALUE))
          throw new RuntimeException("non-positive pixel value encountered: " + value);
        
        int exponent =
            (Float.floatToRawIntBits(value) >>> Constants.NUM_FRACTION_BITS)
                & Constants.EXPONENT_MASK;
        if (exponent < minExponent)
          minExponent = exponent;
        if (exponent > maxExponent)
          maxExponent = exponent;
      }
    
    final int numExponentBits =
        Integer.bitCount((Integer.highestOneBit(maxExponent - minExponent + 1) << 1) - 1);
    return new int[] {numExponentBits, minExponent};
  }
  
  public static float[][] readCompressed(String filename) throws IOException {
    ImageInflater in = new ImageInflater(new BufferedInputStream(new FileInputStream(filename)));
    float[][] result = read(in);
    in.close();
    return result;
  }
  
  public static void writeCompressed(String filename, float[][] image) throws IOException {
    int[] expProp = getExponentProperties(image);
    ImageDeflater out =
        new ImageDeflater(new BufferedOutputStream(new FileOutputStream(filename)), expProp[0],
            expProp[1]);
    write(out, image);
    out.close();
  }
}
