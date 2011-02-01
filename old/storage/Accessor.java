package old.storage;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Accessor {

  public interface SimpleHandler {
    public void handle(float x, float y, float val, int flags);
  }

  public interface Handler {
    public int intersect(int x0, int y0, int x1, int y1);

    public void handle(float x, float y, float val, int flags);
  }

  private int minLevel, topLevel;
  private int topA, topB;

  private FileChannel channel;
  private ByteBuffer addressBuffer = ByteBuffer.allocate(32);
  private long[][] table;

  private int mapLevel;
  private long[][] mapPositions;
  private int[][] mapSizes;

  private int currentMappingA = -1, currentMappingB = -1;
  private int position; // measured in '4 bytes'

  private MappedByteBuffer buffer;
  private IntBuffer intBuffer;
  private FloatBuffer floatBuffer;
  private LongBuffer longBuffer;

  Handler handler;

  public Accessor(String filename) throws IOException {
    this(filename, -1);
  }

  public Accessor(String filename, int mapLevel) throws IOException {
    channel = new FileInputStream(filename).getChannel();

    addressBuffer.limit(16);
    channel.read(addressBuffer);

    this.minLevel = addressBuffer.getInt(0);
    this.topLevel = addressBuffer.getInt(4);
    this.topA = addressBuffer.getInt(8);
    this.topB = addressBuffer.getInt(12);

    table = new long[topLevel - minLevel + 1][5];

    if (mapLevel == -1)
      mapLevel = findMapLevel(1024 * 1024 * 1024, topLevel, 16, channel.size() - 16);

    this.mapLevel = mapLevel;
    mapPositions = new long[1 << (topLevel - mapLevel)][1 << (topLevel - mapLevel)];
    mapSizes = new int[1 << (topLevel - mapLevel)][1 << (topLevel - mapLevel)];
    initMap(16, channel.size() - 16, topLevel, 0, 0);
  }

  private long[] readTable(long position) throws IOException {
    addressBuffer.clear();
    channel.read(addressBuffer, position);

    long[] result = new long[4];
    for (int i = 0; i != 4; ++i)
      result[i] = addressBuffer.getLong(8 * i);

    return result;
  }

  private int findMapLevel(int maxSize, int level, long position, long size) throws IOException {
    if (size <= maxSize)
      return level;

    if (level == minLevel)
      throw new IOException("bags are too large");

    long[] sizes = readTable(position);
    position += 32;

    int mapLevel = Integer.MAX_VALUE;
    for (int i = 0; i != 4; ++i)
      if (sizes[i] != 0) {
        mapLevel = Math.min(mapLevel, findMapLevel(maxSize, level - 1, position, sizes[i]));
        position += sizes[i];
      }

    return mapLevel;
  }

  private void initMap(long position, long size, int level, int a, int b) throws IOException {
    if (level == mapLevel) {
      if (size > Integer.MAX_VALUE)
        throw new RuntimeException("'mapLevel' too large");

      int iA = a - (topA << (topLevel - mapLevel));
      int iB = b - (topB << (topLevel - mapLevel));

      mapPositions[iB][iA] = position;
      mapSizes[iB][iA] = (int) size;
    } else {
      long[] sizes = readTable(position);
      position += 32;

      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i) {
          long s = sizes[2 * j + i];
          if (s != 0) {
            initMap(position, s, level - 1, 2 * a + i, 2 * b + j);
            position += s;
          }
        }
    }
  }

  public void selectRectangle(final SimpleHandler handler, final int x0, final int y0,
      final int x1, final int y1) throws IOException {
    sift(new Handler() {

      public int intersect(int xx0, int yy0, int xx1, int yy1) {
        if (xx1 <= x0 || x1 <= xx0 || yy1 <= y0 || y1 <= yy0)
          return -1;

        if (x0 >= xx0 && y0 >= yy0 && x1 <= xx1 && y1 <= yy1)
          return 1;

        return 0;
      }

      public void handle(float x, float y, float val, int flags) {
        handler.handle(x, y, val, flags);
      }
    });
  }

  public void sift(Handler handler) throws IOException {
    this.handler = handler;
    sift(topLevel, topA, topB);
  }

  private void map(int a, int b) throws IOException {
    position = 0;

    if (!(currentMappingA == a && currentMappingB == b)) {
      currentMappingA = a;
      currentMappingB = b;

      int iA = a - (topA << (topLevel - mapLevel));
      int iB = b - (topB << (topLevel - mapLevel));

      buffer = channel.map(FileChannel.MapMode.READ_ONLY, mapPositions[iB][iA], mapSizes[iB][iA]);
      intBuffer = buffer.asIntBuffer();
      floatBuffer = buffer.asFloatBuffer();
      longBuffer = buffer.asLongBuffer();
    }
  }

  private void siftUnconditionally(int level, int a, int b) throws IOException {
    if (level > mapLevel)
      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i) {
          int iA = (a << (level - 1 - mapLevel)) - (topA << (topLevel - mapLevel));
          int iB = (b << (level - 1 - mapLevel)) - (topB << (topLevel - mapLevel));

          if (mapSizes[iB][iA] != 0)
            siftUnconditionally(level - 1, 2 * a + i, 2 * b + j);
        }
    else {
      if (level == mapLevel)
        map(a, b);

      for (int i = 0; i != 4; ++i)
        table[level - minLevel][i] = longBuffer.get((position >> 1) + i);
      position += 8;

      if (level - 1 == minLevel)
        for (int i = 0; i != 4; ++i)
          for (int k = (int) (table[1][i] / 16); k != 0; --k) {
            float x = floatBuffer.get(position);
            float y = floatBuffer.get(position + 1);
            float val = floatBuffer.get(position + 2);
            int flags = intBuffer.get(position + 3);
            position += 4;

            handler.handle(x, y, val, flags);
          }
      else
        for (int j = 0; j != 2; ++j)
          for (int i = 0; i != 2; ++i)
            if (table[level - minLevel][2 * j + i] != 0)
              siftUnconditionally(level - 1, 2 * a + i, 2 * b + j);
    }
  }

  private void sift(int level, int a, int b) throws IOException {
    if (level == mapLevel)
      map(a, b);

    if (level <= mapLevel) {
      for (int i = 0; i != 4; ++i)
        table[level - minLevel][i] = longBuffer.get((position >> 1) + i);
      position += 8;
    }

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        int aa = 2 * a + i;
        int bb = 2 * b + j;
        int ll = level - 1;

        if (ll >= mapLevel) {
          int iA = (aa << (ll - mapLevel)) - (topA << (topLevel - mapLevel));
          int iB = (bb << (ll - mapLevel)) - (topB << (topLevel - mapLevel));
          if (mapSizes[iB][iA] == 0)
            continue;
        } else
          if (table[level - minLevel][2 * j + i] == 0)
            continue;
        
        int type = handler.intersect(aa << ll, bb << ll, (aa + 1) << ll, (bb + 1) << ll);
        if (type == -1) {
          if (level <= mapLevel)
            position += (int) (table[level - minLevel][2 * j + i] / 4);
        } else if (ll == minLevel) {
          for (int k = (int) (table[1][2 * j + i] / 16); k != 0; --k) {
            float x = floatBuffer.get(position);
            float y = floatBuffer.get(position + 1);
            float val = floatBuffer.get(position + 2);
            int flags = intBuffer.get(position + 3);
            position += 4;

            handler.handle(x, y, val, flags);
          }
        } else if (level == 1)
          siftUnconditionally(ll, aa, bb);
        else
          sift(ll, aa, bb);
      }
  }

}
