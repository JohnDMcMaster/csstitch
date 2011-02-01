package old.storage;

import java.io.IOException;
import java.io.RandomAccessFile;

public class QuadTreeChecker {

  private static int MIN_LEVEL_VERBOSE = 10;
  
  private RandomAccessFile file;

  public QuadTreeChecker(String filename) throws IOException {
    file = new RandomAccessFile(filename, "rw");
    int topLevel = file.readInt();
    file.seek(QuadTree.TOP_NODE_OFFSET);
    check(topLevel);
  }
  
  private void check(int level) throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.out.println("checking level " + level + "...");
    
    long size = file.readLong();
    long startPosition = file.getFilePointer();
    if (size != 0) {
      long objectChunkSize = file.readLong();
      file.seek(file.getFilePointer() + objectChunkSize);

      for (int i = 0; i != 4; ++i)
        check(level - 1);

      long endPosition = file.getFilePointer();
      if (size != endPosition - startPosition)
        throw new RuntimeException(size + " vs. " + (endPosition - startPosition));
    }
  }
  
  public static void main(String[] args) throws IOException {
    new QuadTreeChecker(args[0]);
  }

}
