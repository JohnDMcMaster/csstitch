package old.block_compression;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageDeflater extends ImageSizeIndexImpl implements ImageOutput {
  
  private DataOutputStream out;
  
  private int numExponentBits;
  private int exponentOffset;
  
  private int numValueBits;
  private int valueMask;
  private int valueOffset;
  
  private int numValueBitsPlusTwo;
  private int sixtyTwoMinusNumValueBits;
  
  private long bits;
  private int numBits;
  
  private float[] line;
  
  public ImageDeflater(OutputStream out, int numExponentBits, int exponentOffset)
      throws IOException {
    this.out = new DataOutputStream(out);
    
    this.numExponentBits = numExponentBits;
    this.exponentOffset = exponentOffset;
    
    this.out.writeInt(this.numExponentBits);
    this.out.writeInt(this.exponentOffset);
    
    numValueBits = Constants.NUM_FRACTION_BITS + numExponentBits;
    valueMask = (1 << numValueBits) - 1;
    valueOffset = exponentOffset << Constants.NUM_FRACTION_BITS;
    
    numValueBitsPlusTwo = numValueBits + 2;
    sixtyTwoMinusNumValueBits = 64 - numValueBitsPlusTwo;
  }
  
  public void init(int sx, int sy) throws IOException {
    if (line != null)
      throw new IOException("image stream already initialized");
    
    if (sx <= 0 || sy <= 0)
      throw new RuntimeException("invalid image size: (" + sx + ", " + sy + ")");
    
    this.sx = sx;
    this.sy = sy;
    
    out.writeInt(this.sx);
    out.writeInt(this.sy);
    
    bits = 0;
    numBits = 0;
    
    x = 0;
    y = 0;
    
    line = new float[sx];
  }
  
  public void write(float value) throws IOException {
    if (line == null)
      throw new IOException("image stream uninitialized");
    
    if (!(value > 0 && value <= Float.MAX_VALUE))
      throw new RuntimeException("non-positive pixel value encountered: " + value);
    
    if (numBits >= 32) {
      out.writeInt((int) (bits >> (64 - numBits)));
      numBits -= 32;
    }
    
    if (x != 0 && value == line[(x + sx - 1) % sx]) {
      bits = (bits >>> 1);
      ++numBits;
    } else if (y != 0 && value == line[x]) {
      bits = (bits >>> 2) | (1l << 62);
      numBits += 2;
    } else {
      bits >>>= numValueBitsPlusTwo;
      numBits += numValueBitsPlusTwo;
      
      int valueBits = (Float.floatToRawIntBits(value) & valueMask) - valueOffset;
      bits |= (long) ((valueBits << 2) | 3) << sixtyTwoMinusNumValueBits;
    }
    
    line[x] = value;
    if (increase()) {
      out.writeInt((int) (bits >> (64 - numBits)));
      if (numBits > 32)
        out.writeInt((int) (bits >> (32 - numBits)));
      out.writeInt(0);
      line = null;
    }
  }
  
  public void close() throws IOException {
    out.close();
  }
  
}
