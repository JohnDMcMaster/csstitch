package old.block_compression;

import java.io.IOException;

public interface ImageOutput extends ImageSize {
  
  public void init(int sx, int sy) throws IOException;
  public void write(float value) throws IOException;
  
}
