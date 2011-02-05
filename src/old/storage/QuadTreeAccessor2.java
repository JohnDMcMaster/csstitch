package old.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class QuadTreeAccessor2 {
  
  public interface SimpleHandler {
    public void handle(ByteBuffer file) throws IOException;
  }
  
  public interface Handler {
    public int intersect(int x0, int y0, int x1, int y1);
    
    public void handle(ByteBuffer file) throws IOException;
  }
  
  private static final int MAPPING_SIZE = 512 * 1024 * 1024;
  private static final int BUFFER_SIZE = 64;
  
  private FileChannel channel;
  private long position;
  
  private MappedByteBuffer mapping;
  private long mappingPosition;
  
  private int topLevel;
  private int topA, topB;
  
  Handler handler;
  
  private void checkMapping() throws IOException {
    if (!(position >= mappingPosition && position < mappingPosition + MAPPING_SIZE)) {
      mapping = null;
      Runtime.getRuntime().gc();
      
      mappingPosition = (position / MAPPING_SIZE) * MAPPING_SIZE;
      mapping =
          channel.map(MapMode.READ_ONLY, mappingPosition,
              Math.min(MAPPING_SIZE + BUFFER_SIZE, channel.size() - mappingPosition));
    }
  }
  
  private int readInt() throws IOException {
    checkMapping();
    
    int result = mapping.getInt((int) (position - mappingPosition));
    position += 4;
    return result;
  }
  
  private long readLong() throws IOException {
    checkMapping();
    
    long result = mapping.getLong((int) (position - mappingPosition));
    position += 8;
    return result;
  }
  
  public QuadTreeAccessor2(String filename) throws IOException {
    channel = new RandomAccessFile(filename, "r").getChannel();
    mappingPosition = -MAPPING_SIZE;
    
    position = 0;
    topLevel = readInt();
    topA = readInt();
    topB = readInt();
  }
  
  public void close() throws IOException {
    channel.close();
  }
  
  private void parseObjectChunk() throws IOException {
    long objectChunkSize = readLong();
    if (objectChunkSize == 0)
      return;
    
    long endPosition = position + objectChunkSize;
    while (position != endPosition) {
      checkMapping();
      mapping.position((int) (position - mappingPosition));
      handler.handle(mapping);
      position = mappingPosition + mapping.position();
    }
  }
  
  private void sift(int level, int a, int b) throws IOException {
    long size = readLong();
    if (size == 0)
      return;
    
    if (level == -1)
      throw new IOException("QuadTree file malformed: levels <0 exist");
    
    int type = handler.intersect(a << level, b << level, (a + 1) << level, (b + 1) << level);
    if (type == 1) {
      position -= 8;
      siftUnconditionally(level, a, b);
      return;
    }
    
    if (type == -1) {
      position += size;
      return;
    }
    
    parseObjectChunk();
    
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        sift(level - 1, 2 * a + i, 2 * b + j);
  }
  
  private void siftUnconditionally(int level, int a, int b) throws IOException {
    long size = readLong();
    if (size == 0)
      return;
    
    if (level == -1)
      throw new IOException("QuadTree file malformed: levels <0 exist");
    
    parseObjectChunk();
    
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        siftUnconditionally(level - 1, 2 * a + i, 2 * b + j);
  }
  
  public void sift(Handler handler) throws IOException {
    this.handler = handler;
    
    position = QuadTree.TOP_NODE_OFFSET;
    sift(topLevel, topA, topB);
  }
  
  public void selectRectangle(final SimpleHandler handler, final int x0, final int y0,
      final int x1, final int y1) throws IOException {
    sift(new Handler() {
      
      public int intersect(int xx0, int yy0, int xx1, int yy1) {
        if (xx1 <= x0 || x1 <= xx0 || yy1 <= y0 || y1 <= yy0)
          return -1;
        
        if (xx0 >= x0 && yy0 >= y0 && xx1 <= x1 && yy1 <= y1)
          return 1;
        
        return 0;
      }
      
      public void handle(ByteBuffer file) throws IOException {
        handler.handle(file);
      }
    });
  }
  
  public void selectAll(SimpleHandler handler) throws IOException {
    selectRectangle(handler, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
        Integer.MAX_VALUE);
  }
  
  public static void main(String[] args) throws IOException {
    new QuadTreeAccessor2(args[0]).selectRectangle(new SimpleHandler() {
      public void handle(ByteBuffer file) {
        float x = file.getFloat();
        float y = file.getFloat();
        float val = file.getFloat();
        file.getInt();
        System.out.println("(" + x + ", " + y + "): " + val);
      }
    }, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]),
        Integer.parseInt(args[4]));
  }
  
}
