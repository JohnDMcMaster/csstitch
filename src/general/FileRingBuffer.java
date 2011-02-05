package general;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileRingBuffer {
  
  private File file;
  private DataInputStream in;
  private DataOutputStream out;
  
  public FileRingBuffer(File file) throws IOException {
    this.file = file;
    out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
  }
  
  public DataInputStream getIn() {
    return in;
  }
  
  public DataOutputStream getOut() {
    return out;
  }
  
  public void close() throws IOException {
    in.close();
    out.close();
    file.delete();
  }
  
  public static void main(String[] args) throws IOException {
    FileRingBuffer buffer = new FileRingBuffer(new File("/home/noname/di/tmp"));
    DataOutputStream out = buffer.getOut();
    DataInputStream in = buffer.getIn();
    
    int sum = 0;
    
    for (int i = 0; i != 1000; ++i) {
      System.err.println(i);
      
      int n = (int) (1024 * Math.random());
      
      for (int j = 0; j != n; ++j)
        out.writeInt((int) (2 * Math.random()));
      out.flush();
      
      for (int j = 0; j != n; ++j)
        sum += in.readInt();
    }
    
    System.err.println(sum);
  }
  
}
