package old.block_compression;

import java.io.IOException;

public class ImageReader extends ImageSizeIndexImpl implements ImageOutput {
  
  float[][] image, result;
  
  public ImageReader() {
  }
  
  public ImageReader(ImageInput input) throws IOException {
    ImageStreams.link(input, this);
  }
  
  public void init(int sx, int sy) throws IOException {
    if (image != null)
      throw new IOException("image stream already initialized");
    
    if (sx <= 0 || sy <= 0)
      throw new RuntimeException("invalid image size: (" + sx + ", " + sy + ")");
    
    this.sx = sx;
    this.sy = sy;
    
    image = new float[this.sy][this.sx];
    
    x = 0;
    y = 0;
  }
  
  public void write(float value) throws IOException {
    if (image == null)
      throw new IOException("image stream uninitialized");
    
    image[y][x] = value;
    if (increase()) {
      result = image;
      image = null;
    }
  }
  
  public float[][] getImage() {
    return result;
  }
  
  public float[][] getPartialImage() {
    return image;
  }
  
}
