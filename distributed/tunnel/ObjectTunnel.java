package distributed.tunnel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectTunnel {
  
  private ObjectInputStream in;
  private ObjectOutputStream out;
  
  public ObjectTunnel(ObjectInputStream in, ObjectOutputStream out) {
    this.in = in;
    this.out = out;
  }
  
  public ObjectTunnel() throws IOException {
    out = new ObjectOutputStream(new BufferedOutputStream(System.out));
    out.flush();

    in = new ObjectInputStream(new BufferedInputStream(System.in));
  }
  
  public ObjectTunnel(Process process) throws IOException {
    out = new ObjectOutputStream(new BufferedOutputStream(process.getOutputStream()));
    out.flush();
    
    in = new ObjectInputStream(new BufferedInputStream(process.getInputStream()));
  }
  
  public ObjectInputStream getIn() {
    return in;
  }
  
  public ObjectOutputStream getOut() {
    return out;
  }
  
  public void flush() throws IOException {
    out.flush();
  }
  
  public <T> void write(T object) throws IOException {
    out.writeBoolean(object != null);
    if (object != null)
      out.writeUnshared(object);
    out.flush();
  }
  
  @SuppressWarnings("unchecked")
  public<T> T read() throws IOException {
    try {
      return in.readBoolean() ? (T) in.readUnshared() : null;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
}
