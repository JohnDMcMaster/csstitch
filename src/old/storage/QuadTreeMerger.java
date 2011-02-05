package old.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import old.storage.QuadTreeCreator.Processor;


public class QuadTreeMerger<T> {

  private DataInputStream[] in;
  private int[] topLevels;
  private int[] topAs, topBs;

  private DataOutputStream out;
  private int topLevel;
  private int topA, topB;

  Processor<T> processor;
  private byte[] buffer = new byte[1024];
  private boolean[] selector;

  public void merge(String nameOut, String[] namesIn, Processor<T> processor) throws IOException {
    this.processor = processor;

    in = new DataInputStream[namesIn.length];

    topLevels = new int[in.length];
    topAs = new int[in.length];
    topBs = new int[in.length];

    selector = new boolean[in.length];

    topLevel = Integer.MIN_VALUE;

    for (int i = 0; i != in.length; ++i) {
      in[i] = new DataInputStream(new BufferedInputStream(new FileInputStream(namesIn[i])));
      topLevels[i] = in[i].readInt();
      topAs[i] = in[i].readInt();
      topBs[i] = in[i].readInt();

      topLevel = Math.max(topLevel, topLevels[i]);
    }

    outer: for (;; ++topLevel) {
      topA = topAs[0] >> (topLevel - topLevels[0]);
      topB = topBs[0] >> (topLevel - topLevels[0]);

      for (int i = 1; i != in.length; ++i)
        if ((topAs[i] >> (topLevel - topLevels[i])) != topA)
          continue outer;

      break;
    }

    out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nameOut)));

    out.writeInt(topLevel);
    out.writeInt(topA);
    out.writeInt(topB);

    process(topLevel, topA, topB);

    out.close();
  }

  private void process(int level, int a, int b) throws IOException {
    boolean skip = true;
    for (int i = 0; i != in.length; ++i) {
      selector[i] = level <= topLevels[i] && (a >> (topLevels[i] - level)) == topAs[i]
          && (b >> (topLevels[i] - level)) == topBs[i];
      if (selector[i])
        skip &= in[i].readLong() == 0;
    }

    if (skip) {
      out.writeLong(0);
      return;
    }

    out.writeLong(-1);

    MemoryHandle objects = new MemoryHandle();
    DataOutputStream objectsOut = objects.openWriting();

    for (int i = 0; i != in.length; ++i)
      if (selector[i]) {
        long objectChunkSize = in[i].readLong();
        if (objectChunkSize != 0) {
          while (buffer.length < objectChunkSize)
            buffer = new byte[2 * buffer.length];

          in[i].read(buffer, 0, (int) objectChunkSize);
          DataInputStream objectIn = new DataInputStream(new ByteArrayInputStream(buffer, 0,
              (int) objectChunkSize));
          try {
            for (;;) {
              T object = processor.read(objectIn);
              objectsOut.writeInt(i);
              processor.write(objectsOut, object);
            }
          } catch (EOFException eof) {
          }
        }
      }

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        process(level - 1, 2 * a + i, 2 * b + j);
  }
}
