package old.block_compression;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageInflater extends ImageSizeIndexImpl implements ImageInput {
  
  private DataInputStream in;
  
  private int numExponentBits;
  private int exponentOffset;
  
  private int numValueBits;
  private int valueMask;
  private int valueOffset;
  
  private int numValueBitsPlusTwo;
  
  private long bits;
  private int numBits;
  
  private float[] line;
  
  public ImageInflater(InputStream in) throws IOException {
    this.in = new DataInputStream(in);
    
    numExponentBits = this.in.readInt();
    exponentOffset = this.in.readInt();
    
    numValueBits = Constants.NUM_FRACTION_BITS + numExponentBits;
    valueMask = (1 << numValueBits) - 1;
    valueOffset = exponentOffset << Constants.NUM_FRACTION_BITS;
    
    numValueBitsPlusTwo = numValueBits + 2;
  }
  
  public void init() throws IOException {
    if (line != null)
      throw new IOException("image stream already initialized");
    
    sx = this.in.readInt();
    sy = this.in.readInt();
    
    if (sx <= 0 || sy <= 0)
      throw new IOException("invalid image size: (" + sx + ", " + sy + ")");
    
    bits = 0;
    numBits = 0;
    
    x = 0;
    y = 0;
    
    line = new float[sx];
  }
  
  public float read() throws IOException {
    if (line == null)
      throw new IOException("image stream uninitialized");
    
    if (numBits <= 32) {
      bits |= (in.readInt() & 0xffffffffl) << numBits;
      numBits += 32;
    }
    
    float result;
    if (((int) bits & 0x00000001) == 0) {
      result = line[(x + sx - 1) % sx];
      bits >>>= 1;
      --numBits;
    } else if (((int) bits & 0x00000002) == 0) {
      result = line[x];
      bits >>>= 2;
      numBits -= 2;
    } else {
      int valueBits = (((int) bits >>> 2) & valueMask) + valueOffset;
      result = Float.intBitsToFloat(valueBits);
      
      bits >>>= numValueBitsPlusTwo;
      numBits -= numValueBitsPlusTwo;
    }
    
    line[x] = result;
    if (increase()) {
      if (numBits < 32)
        in.readInt();
      line = null;
    }
    
    return result;
  }
  
  public void close() throws IOException {
    in.close();
  }
  
}
