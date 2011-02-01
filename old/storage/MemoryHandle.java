package old.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MemoryHandle implements Handle {

  private ByteArrayOutputStream out;
  private byte[] buffer;

  public MemoryHandle() {
  }

  public DataInputStream openReading() throws IOException {
    return new DataInputStream(new ByteArrayInputStream(buffer));
  }

  public void process() throws IOException {
    buffer = out.toByteArray();
    out = null;
  }

  public DataOutputStream openWriting() throws IOException {
    out = new ByteArrayOutputStream(256);
    return new DataOutputStream(out);
  }

  public long getSize() throws IOException {
    return buffer.length;
  }

  public void delete() throws IOException {
    buffer = null;
  }
  
}