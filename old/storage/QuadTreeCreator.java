package old.storage;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class QuadTreeCreator<T> {

  public static interface Processor<T> {
    public T read(DataInput file) throws IOException;

    public void write(DataOutput file, T object) throws IOException;

    public void getBounds(T object, float[] boundary);
  }

  public static final int MIN_LEVEL_FILE = 10;
  public static final int MIN_LEVEL_VERBOSE = 10;

  private String filename;
  private int minLevel;

  private Processor<T> processor;
  private float[] boundary = new float[4];

  private Handle copy;
  private DataOutputStream copyOutput;
  private byte[] buffer = new byte[1024];

  private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
  private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;

  public QuadTreeCreator(Processor<T> processor) throws IOException {
    this.processor = processor;

    copy = new FileHandle("stitch-input.dat");
    copyOutput = copy.openWriting();
  }

  public void add(T object) throws IOException {
    processor.getBounds(object, boundary);

    minX = Math.min(boundary[0], minX);
    minY = Math.min(boundary[1], minY);
    maxX = Math.max(boundary[2], maxX);
    maxY = Math.max(boundary[3], maxY);

    processor.write(copyOutput, object);
  }

  public void write(String filename, int minLevel) throws IOException {
    copyOutput.close();
    copy.process();

    int topLevel = 0;
    int topA = (int) Math.floor(minX);
    int topB = (int) Math.floor(minY);

    while (!(maxX <= ((topA + 1) << topLevel) && maxY <= (topB + 1) << topLevel)) {
      ++topLevel;
      topA >>= 1;
      topB >>= 1;
    }

    this.filename = filename;
    this.minLevel = minLevel;

    process(copy, topLevel, topA, topB, true);
  }

  private Handle process(Handle input, int level, int a, int b, boolean isTopLevel)
      throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.out.println("processing level " + level + "...");

    Handle[][] subInput = new Handle[2][2];
    Handle[][] subOutput = new Handle[2][2];

    DataOutputStream[][] subOut = new DataOutputStream[2][2];

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        if (level - 1 >= MIN_LEVEL_FILE)
          subInput[j][i] = new FileHandle(String.format("stitch-input-%02d--%02d-%02d.dat",
              level - 1, 2 * a + i, 2 * b + j));
        else
          subInput[j][i] = new MemoryHandle();

        subOut[j][i] = subInput[j][i].openWriting();
      }

    Handle objects = new MemoryHandle();
    DataOutputStream objectsOut = objects.openWriting();

    DataInputStream in = input.openReading();

    try {
      for (;;) {
        T object = processor.read(in);
        processor.getBounds(object, boundary);

        int i = -1, j = -1;
        if (level != minLevel) {
          if (boundary[2] <= (2 * a + 1) << (level - 1))
            i = 0;
          else if (boundary[0] >= (2 * a + 1) << (level - 1))
            i = 1;

          if (boundary[3] <= (2 * b + 1) << (level - 1))
            j = 0;
          else if (boundary[1] >= (2 * b + 1) << (level - 1))
            j = 1;
        }

        if (i == -1 || j == -1)
          processor.write(objectsOut, object);
        else
          processor.write(subOut[j][i], object);
      }
    } catch (EOFException eof) {
      in.close();
    }

    input.delete();

    objectsOut.close();
    objects.process();

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        subOut[j][i].close();
        subInput[j][i].process();
        if (subInput[j][i].getSize() != 0)
          subOutput[j][i] = process(subInput[j][i], level - 1, 2 * a + i, 2 * b + j, false);
        else
          subInput[j][i].delete();
      }

    Handle output;
    if (isTopLevel)
      output = new FileHandle(filename);
    else if (level >= MIN_LEVEL_FILE)
      output = new FileHandle(String.format("stitch-output-%02d--%02d-%02d.dat", level, a, b));
    else
      output = new MemoryHandle();

    DataOutputStream out = output.openWriting();

    if (isTopLevel) {
      out.writeInt(level);
      out.writeInt(a);
      out.writeInt(b);
    }

    long size = 8 + objects.getSize();
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        size += subOutput[j][i] == null ? 8 : subOutput[j][i].getSize();

    out.writeLong(size);
    out.writeLong(objects.getSize());

    if (objects.getSize() != 0) {
      DataInputStream objectsIn = objects.openReading();

      int length;
      while ((length = objectsIn.read(buffer)) > 0)
        out.write(buffer, 0, length);

      objectsIn.close();
    }

    objects.delete();

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        if (subOutput[j][i] != null) {
          DataInputStream subIn = subOutput[j][i].openReading();

          int length;
          while ((length = subIn.read(buffer)) > 0)
            out.write(buffer, 0, length);

          subIn.close();
          subOutput[j][i].delete();
        } else
          out.writeLong(0);

    out.close();
    output.process();
    return output;
  }

}
