package old.block_compression;

import java.io.IOException;

public interface ImageInput extends ImageSize {
  
  public void init() throws IOException;
  public float read() throws IOException;
  
}
