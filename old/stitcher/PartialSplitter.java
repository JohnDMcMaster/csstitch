package old.stitcher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import old.storage.FileHandle;
import old.storage.Handle;
import old.storage.MemoryHandle;


public strictfp class PartialSplitter {

  public static String inputFormat;
  public static String outputFormat;

  public static final int MIN_LEVEL = 1;
  public static final int MIN_LEVEL_FILE = 10;
  public static final int MIN_LEVEL_VERBOSE = 10;

  public static Handle process(int level, int a, int b, Handle input) throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.out.println("processing level " + level + ", (" + a + ", " + b + ") ...");

    if (level == MIN_LEVEL) {
      Handle output = new MemoryHandle();

      DataOutputStream out = output.openWriting();
      out.writeLong(input.getSize() + 20);
      out.writeInt((int) input.getSize());

      DataInputStream in = input.openReading();

      try {
        for (;;) {
          float x = in.readFloat();
          float y = in.readFloat();
          float val = in.readFloat();
          int flags = in.readInt();

          out.writeFloat(x);
          out.writeFloat(y);
          out.writeFloat(val);
          out.writeInt(flags);
        }
      } catch (EOFException eof) {
        in.close();
      }
      
      input.delete();
      
      for (int i = 0; i != 4; ++i)
        out.writeLong(0);
      
      output.process();
      return output;
    }

    Handle[][] subInput = new Handle[2][2];
    Handle[][] subOutput = new Handle[2][2];

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        if (level - 1 >= MIN_LEVEL_FILE)
          subInput[j][i] = new FileHandle(String.format(inputFormat, level - 1, 2 * a + i, 2 * b
              + j));
        else
          subInput[j][i] = new MemoryHandle();
      }

    {
      DataInputStream in = input.openReading();

      DataOutputStream[][] out = new DataOutputStream[2][2];
      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i)
          out[j][i] = subInput[j][i].openWriting();

      try {
        for (;;) {
          float x = in.readFloat();
          float y = in.readFloat();
          float val = in.readFloat();
          int flags = in.readInt();

          int i = ((int) x >> (level - 1)) - 2 * a;
          int j = ((int) y >> (level - 1)) - 2 * b;

          out[j][i].writeFloat(x);
          out[j][i].writeFloat(y);
          out[j][i].writeFloat(val);
          out[j][i].writeInt(flags);
        }
      } catch (EOFException eof) {
        in.close();
      }

      try {
        input.delete();
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }

      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i)
          out[j][i].close();
    }

    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        subInput[j][i].process();
        subOutput[j][i] = process(level - 1, 2 * a + i, 2 * b + j, subInput[j][i]);
      }

    Handle output;
    if (level >= MIN_LEVEL_FILE)
      output = new FileHandle(String.format(outputFormat, level, a, b));
    else
      output = new MemoryHandle();

    {
      DataOutputStream out = output.openWriting();

      long size = 4;
      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i)
          size += subOutput[j][i].getSize();

      out.writeLong(size);
      out.writeInt(0);

      for (int j = 0; j != 2; ++j)
        for (int i = 0; i != 2; ++i) {
          DataInputStream in = subOutput[j][i].openReading();

          try {
            for (;;) {
              float x = in.readFloat();
              float y = in.readFloat();
              float val = in.readFloat();
              int flags = in.readInt();

              out.writeFloat(x);
              out.writeFloat(y);
              out.writeFloat(val);
              out.writeInt(flags);
            }
          } catch (EOFException eof) {
            in.close();
          }

          try {
            subOutput[j][i].delete();
          } catch (IOException e) {
            System.err.println(e.getMessage());
          }
        }

      out.close();
    }

    output.process();
    return output;
  }

  public static int[] getLevelInfo(String filename) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));

    float minX = Float.POSITIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;

    float maxX = Float.NEGATIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;

    try {
      for (;;) {
        float x = in.readFloat();
        float y = in.readFloat();
        in.readFloat();
        in.readInt();

        minX = Math.min(minX, x);
        minY = Math.min(minY, y);

        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
      }
    } catch (EOFException eof) {
      in.close();
    }

    int level = 0, a, b;
    for (;; ++level) {
      a = (int) minX >> level;
      b = (int) minY >> level;
      if ((int) maxX >> level == a && (int) maxY >> level == b)
        break;
    }

    return new int[] {level, a, b};
  }

  public static void main(String[] args) throws IOException {
    inputFormat = args[1];
    outputFormat = args[2];

    int[] info = getLevelInfo(args[0]);
    FileHandle fileHandle = (FileHandle) process(info[0], info[1], info[2], new FileHandle(args[0]));
    System.out.println(fileHandle.getName());
  }
}
