package confocal;

import java.io.IOException;
import java.io.RandomAccessFile;

public class TiffReader {
  
  public static int readShort(RandomAccessFile in) throws IOException {
    int a = in.read();
    int b = in.read();
    return a | (b << 8);
  }
  
  public static int readInt(RandomAccessFile in) throws IOException {
    int a = readShort(in);
    int b = readShort(in);
    return a | (b << 16);
  }
  
  public static short[][] readLeicaTiff(String filename) throws IOException {
    RandomAccessFile in = new RandomAccessFile(filename, "r");
    
    in.skipBytes(4);
    
    int imageDirOffset = readInt(in);
    in.seek(imageDirOffset);
    
    int numImageDirs = readShort(in);
    
    int sx = -1;
    int sy = -1;
    
    int rowsPerStrip = -1;
    int stripOffsetsOffset = -1;
    int stripByteCountsOffset = -1;
    
    for (int i = 0; i != numImageDirs; ++i) {
      int id = readShort(in);
      int type = readShort(in);
      int count = readInt(in);
      int offset = readInt(in);
      
      if (id == 256)
        sx = offset;
      else if (id == 257)
        sy = offset;
      else if (id == 273)
        stripOffsetsOffset = offset;
      else if (id == 278)
        rowsPerStrip = offset;
      else if (id == 279)
        stripByteCountsOffset = offset;
    }
    
    System.err.println(sx + " x " + sy);
    
    int numRows = (sy - 1) / rowsPerStrip + 1;
    
    int[] stripOffsets = new int[numRows];
    in.seek(stripOffsetsOffset);
    for (int i = 0; i != numRows; ++i)
      stripOffsets[i] = readInt(in);
    
    int[] stripByteCounts = new int[numRows];
    in.seek(stripByteCountsOffset);
    for (int i = 0; i != numRows; ++i)
      stripByteCounts[i] = readInt(in);
    
    short[][] data = new short[sy][sx];
    
    int x = 0;
    int y = 0;
    
    for (int i = 0; i != numRows; ++i) {
      in.seek(stripOffsets[i]);
      for (int j = 0; j != stripByteCounts[i] / 2; ++j) {
        data[y][x] = (short) readShort(in);
        if (++x == sx) {
          x = 0;
          if (++y == sy)
            y = 0;
        }
      }
    }
    
    return data;
  }
}
