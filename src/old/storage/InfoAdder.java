package old.storage;

import general.Streams;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class InfoAdder {

  public static void main(String[] args) throws IOException {
    FileInputStream in = new FileInputStream(args[1]);
    DataOutputStream out = new DataOutputStream(new FileOutputStream(args[0]));
    
    out.writeInt(Integer.parseInt(args[2])); // minLevel
    out.writeInt(Integer.parseInt(args[3])); // topLevel
    out.writeInt(Integer.parseInt(args[4])); // topA
    out.writeInt(Integer.parseInt(args[5])); // topB
    
    Streams.readFully(in, out, 8192);
  }

}
