package distributed.manager;

import java.io.DataOutputStream;
import java.io.IOException;

import general.Link;
import general.execution.Command;

public class Test {
  
  public static void main(String[] args) throws IOException, InterruptedException {
    Process process = new Command("/home/noname/di/manager/a.out").startErr();
    Link.link(process.getInputStream(), System.out);
    
    DataOutputStream out = new DataOutputStream(process.getOutputStream());
    Thread.sleep(1000);
    out.writeInt(0x12345678);
    Thread.sleep(1000);
    out.close();
  }
  
}
