package old.storage;

import java.io.IOException;
import java.io.RandomAccessFile;

public class QuadTreeSizeFixer {

  public static final int MIN_LEVEL_VERBOSE = 10;

  private RandomAccessFile file;

  QuadTreeSizeFixer(String filename, int level) throws IOException {
    file = new RandomAccessFile(filename, "rw");
    int topLevel = level;
    //int topLevel = file.readInt();
    //file.seek(QuadTree.TOP_NODE_OFFSET);
    
    fixSize(topLevel);
    file.close();
  }

  private void fixSize(int level) throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.out.println("fixing level " + level + "...");
    
    long startPosition = file.getFilePointer();

    long size = file.readLong();
    if (size != 0) {
      long objectChunkSize = file.readLong();
      file.seek(file.getFilePointer() + objectChunkSize);

      for (int i = 0; i != 4; ++i)
        fixSize(level - 1);

      long endPosition = file.getFilePointer();
      if (size != endPosition - (startPosition + 8))
        throw new RuntimeException(size + " vs. " + (endPosition - startPosition));
      
      //file.seek(startPosition);
      //file.writeLong(endPosition - (startPosition + 8));
      //file.seek(endPosition);
    }
  }
  
  public static void main(String[] args) throws IOException {
    new QuadTreeSizeFixer(args[0], Integer.parseInt(args[1]));
  }

}
