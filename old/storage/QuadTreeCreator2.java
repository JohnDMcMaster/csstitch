package old.storage;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;

import old.storage.QuadTreeCreator.Processor;


public class QuadTreeCreator2<T> {
  
  public static final int MIN_LEVEL_FILE = 11;
  public static final int MIN_LEVEL_VERBOSE = 11;
  
  private String dir;
  private DataOutputStream output;
  private int minLevel;
  
  private Processor<T> processor;
  private float[] boundary = new float[4];
  
  private Handle copy;
  private DataOutputStream copyOutput;
  private byte[] buffer = new byte[1024];
  
  private float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
  private float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
  
  public QuadTreeCreator2(String dir, Processor<T> processor) throws IOException {
    this(dir, dir + "/stitch-input.dat", processor);
  }
  
  public QuadTreeCreator2(String dir, String input, Processor<T> processor) throws IOException {
    this.dir = dir;
    this.processor = processor;
    
    copy = new FileHandle(input);
    copyOutput = copy.openWriting();
  }
  
  public QuadTreeCreator2(String dir, Processor<T> processor, String filename, int minLevel,
      int topLevel, int topA, int topB) throws IOException {
    this(dir, dir + "/stitch-input.dat", processor, filename, minLevel, topLevel, topA, topB);
  }
  
  // resuming operation
  public QuadTreeCreator2(String dir, String input, Processor<T> processor, String filename,
      int minLevel, int topLevel, int topA, int topB) throws IOException {
    this.dir = dir;
    this.processor = processor;
    
    copy = new FileHandle(input);
    
    this.minLevel = minLevel;
    
    startProcessing(filename, topLevel, topA, topB);
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
    
    this.minLevel = minLevel;
    
    startProcessing(filename, topLevel, topA, topB);
  }
  
  private void startProcessing(String filename, int topLevel, int topA, int topB)
      throws IOException {
    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    
    output.writeInt(topLevel);
    output.writeInt(topA);
    output.writeInt(topB);
    
    process(copy, topLevel, topA, topB);
    
    output.close();
  }
  
  private void process(Handle input, int level, int a, int b) throws IOException {
    if (level >= MIN_LEVEL_VERBOSE)
      System.out.println("processing level " + level + "...");
    
    if (input.getSize() == 0) {
      input.delete();
      output.writeLong(0);
      return;
    }
    
    Handle[][] subInput = new Handle[2][2];
    
    DataOutputStream[][] subOut = new DataOutputStream[2][2];
    
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        if (level - 1 >= MIN_LEVEL_FILE)
          subInput[j][i] =
              new FileHandle(String.format(dir + "/stitch-input-%02d--%02d-%02d.dat", level - 1, 2
                  * a + i, 2 * b + j));
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
          if (boundary[0] >= (2 * a + 1) << (level - 1))
            i = 1;
          else if (boundary[2] <= (2 * a + 1) << (level - 1))
            i = 0;
          
          if (boundary[1] >= (2 * b + 1) << (level - 1))
            j = 1;
          else if (boundary[3] <= (2 * b + 1) << (level - 1))
            j = 0;
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
    
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i) {
        subOut[j][i].close();
        subInput[j][i].process();
      }
    
    objectsOut.close();
    objects.process();
    
    output.writeLong(-1);
    
    output.writeLong(objects.getSize());
    
    if (objects.getSize() != 0) {
      DataInputStream objectsIn = objects.openReading();
      
      int length;
      while ((length = objectsIn.read(buffer)) != -1)
        output.write(buffer, 0, length);
      
      objectsIn.close();
    }
    
    objects.delete();
    
    for (int j = 0; j != 2; ++j)
      for (int i = 0; i != 2; ++i)
        process(subInput[j][i], level - 1, 2 * a + i, 2 * b + j);
  }
  
}
