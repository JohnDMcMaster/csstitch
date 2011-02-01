package old.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Handle {
  
  public DataInputStream openReading() throws IOException;

  public void process() throws IOException;

  public DataOutputStream openWriting() throws IOException;

  public long getSize() throws IOException;

  public void delete() throws IOException;
  
};

