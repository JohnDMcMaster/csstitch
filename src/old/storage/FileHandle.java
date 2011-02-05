package old.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileHandle implements Handle {
  
  private File file;

  public FileHandle(String filename) {
    file = new File(filename);
  }

  public DataInputStream openReading() throws IOException {
    return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
  }

  public void process() throws IOException {
  }

  public DataOutputStream openWriting() throws IOException {
    return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
  }

  public long getSize() throws IOException {
    return file.length();
  }

  public void delete() throws IOException {
    if (!file.delete())
      throw new IOException("file could not be deleted: " + file.getCanonicalPath());
  }

  public String getName() {
    return file.getName();
  }
  
}

