package old.storage;

import java.io.IOException;
import java.io.RandomAccessFile;

public class QuadTreeAccessor {

  public interface SimpleHandler {
    public void handle(RandomAccessFile file);
  }

  public interface Handler {
    public int intersect(int x0, int y0, int x1, int y1);

    public void handle(RandomAccessFile file);
  }

  private RandomAccessFile file;

  private int topLevel;
  private int topA, topB;

  Handler handler;

  public QuadTreeAccessor(String filename) throws IOException {
    file = new RandomAccessFile(filename, "r");

    topLevel = file.readInt();
    topA = file.readInt();
    topB = file.readInt();
  }

  private void parseObjectChunk() throws IOException {
    long objectChunkSize = file.readLong();
    long endPosition = file.getFilePointer() + objectChunkSize;
    while (file.getFilePointer() != endPosition)
      handler.handle(file);
  }

  private void sift(int level, int a, int b) throws IOException {
    long size = file.readLong();
    if (size == 0)
      return;

    if (level == -1)
      throw new IOException("QuadTree file malformed: levels <0 exist");

    int type = handler.intersect(a << level, b << level, (a + 1) << level, (b + 1) << level);
    if (type == 1) {
      file.seek(file.getFilePointer() - 8);
      siftUnconditionally(level, a, b);
      return;
    }

    if (type == -1) {
      file.seek(file.getFilePointer() + size);
      return;
    }

    parseObjectChunk();

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        sift(level - 1, 2 * a + i, 2 * b + j);
  }

  private void siftUnconditionally(int level, int a, int b) throws IOException {
    long size = file.readLong();
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

    file.seek(QuadTree.TOP_NODE_OFFSET);
    sift(topLevel, topA, topB);
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

      public void handle(RandomAccessFile file) {
        handler.handle(file);
      }
    });
  }

}
