package general;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class Streams {
  
  public static int DEFAULT_BUFFER_SIZE = 16 * 1024;
  
  public static void readFully(InputStream in, OutputStream out, int bufferSize) throws IOException {
    byte[] array = new byte[bufferSize];
    int length;
    while ((length = in.read(array)) > 0)
      out.write(array, 0, length);
  }
  
  public static void readFully(InputStream in, OutputStream out) throws IOException {
    readFully(in, out, DEFAULT_BUFFER_SIZE);
  }
  
  public static String readIntoString(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    readFully(in, out);
    return out.toString();
  }
  
  public static byte[] readIntoBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    readFully(in, out);
    return out.toByteArray();
  }
  
  public static String readText(String file) throws IOException {
    return readIntoString(new FileInputStream(new File(file)));
  }
  
  public static byte[] readBinary(String file) throws IOException {
    return readIntoBytes(new FileInputStream(new File(file)));
  }
  
  public static void writeText(String file, String content) throws IOException {
    FileWriter out = new FileWriter(new File(file));
    out.write(content);
    out.close();
  }
  
  public static void writeBinary(String file, byte[] content) throws IOException {
    FileOutputStream out = new FileOutputStream(new File(file));
    out.write(content);
    out.close();
  }
  
  private static class DummyInputStream extends FilterInputStream {
    public DummyInputStream(InputStream in) {
      super(in);
    }
    
    public void close() throws IOException {
    }
  }
  
  private static class DummyOutputStream extends FilterOutputStream {
    public DummyOutputStream(OutputStream out) {
      super(out);
    }
    
    public void close() throws IOException {
    }
  }
  
  public static Object readObject(InputStream in) throws IOException {
    ObjectInputStream objectIn = new ObjectInputStream(new DummyInputStream(in));
    Object result;
    try {
      result = objectIn.readUnshared();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    objectIn.close();
    return result;
  }
  
  public static void writeObject(OutputStream out, Object object) throws IOException {
    ObjectOutputStream objectOut = new ObjectOutputStream(new DummyOutputStream(out));
    objectOut.writeUnshared(object);
    objectOut.close();
  }
  
  @SuppressWarnings("unchecked")
  public static <T> T readObject(String file) throws IOException {
    ObjectInputStream in =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
    T object;
    try {
      object = (T) in.readUnshared();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    in.close();
    return object;
  }
  
  public static <T> void writeObject(String file, T object) throws IOException {
    ObjectOutputStream out =
        new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    out.writeUnshared(object);
    out.close();
  }
  
}
