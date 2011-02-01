package old.block_compression;

import java.io.EOFException;
import java.io.IOException;

public class ImageWriter extends ImageSizeIndexImpl implements ImageInput {
  
  float[][] original, image;
  
  public ImageWriter(float[][] image, ImageOutput output) throws IOException {
    this(image);
    ImageStreams.link(this, output);
  }
  
  public ImageWriter(float[][] image) {
    if (image.length <= 0 || image[0].length <= 0)
      throw new RuntimeException("invalid image size: (" + image[0].length + ", " + image.length
          + ")");
    
    original = image;
    
    sx = original[0].length;
    sy = original.length;
  }
  
  public void init() throws IOException {
    if (image != null)
      throw new IOException("image stream already initialized");
    
    image = original;
  }
  
  public float read() throws IOException {
    if (image == null)
      throw new EOFException("image stream uninitialized");
    
    float result = image[y][x];
    if (increase())
      image = null;
    
    return result;
  }
  
}
