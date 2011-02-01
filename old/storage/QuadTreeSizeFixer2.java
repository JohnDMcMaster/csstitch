package old.storage;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import stitcher.StitchInfo;

import data.DataTools;
import distributed.Bootstrap;

public class QuadTreeSizeFixer2 {

  public static final int MIN_LEVEL_VERBOSE = 10;

  private static final long MAPPING_BUFFER = 64;
  private static final long MAPPING_SIZE = 512 * 1024 * 1024;

  FileChannel channel;
  long position;

  MappedByteBuffer mapping;
  long mappingPosition;

  public void checkMapping() throws IOException {
    if (!(position >= mappingPosition && position < mappingPosition + MAPPING_SIZE)) {
      mappingPosition = (position / MAPPING_SIZE) * MAPPING_SIZE;
      mapping = channel.map(FileChannel.MapMode.READ_WRITE, mappingPosition,
          Math.min(MAPPING_SIZE + MAPPING_BUFFER, channel.size() - mappingPosition));
    }
  }

  public int readInt() throws IOException {
    checkMapping();

    int result = mapping.getInt((int) (position - mappingPosition));
    position += 4;
    return result;
  }

  public long readLong() throws IOException {
    checkMapping();

    long result = mapping.getLong((int) (position - mappingPosition));
    position += 8;
    return result;
  }

  public void writeLong(long x) throws IOException {
    checkMapping();

    mapping.putLong((int) (position - mappingPosition), x);
    position += 8;
  }

  public QuadTreeSizeFixer2(String filename) throws IOException {
    channel = new RandomAccessFile(filename, "rw").getChannel();
    mappingPosition = -MAPPING_SIZE;

    position = 0;

    int topLevel = readInt();
    int topA = readInt();
    int topB = readInt();

    fixSize(topLevel);

    channel.close();
  }

  private void fixSize(int level) throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.err.println("fixing level " + level + "...");

    long startPosition = position;

    long size = readLong();
    if (size != 0) {
      long objectChunkSize = readLong();
      position += objectChunkSize;

      for (int i = 0; i != 4; ++i)
        fixSize(level - 1);

      long endPosition = position;
      //if (size != endPosition - (startPosition + 8))
      //  throw new RuntimeException(size + " vs. " + (endPosition - startPosition));

      position = startPosition;
      writeLong(endPosition - (startPosition + 8));
      position = endPosition;
    }
  }

  public static void main(String[] args) throws IOException {
    //Bootstrap.bootstrap(new String[] {"stitch-combined.dat"});

    new QuadTreeSizeFixer2(args[0]);
  }

}
