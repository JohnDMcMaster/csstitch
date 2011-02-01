package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import data.DataTools;

public class HuginPrepaper {

  // followed by
  //   ls *.pto | xargs -I arg mv arg.2 arg
  public static void main(String[] args) throws IOException {
    convert(DataTools.DIR + "comp-cpu-3-00");
    //convert(DataTools.DIR + "/comp01");
    //convert(DataTools.DIR + "/comp10");
    //convert(DataTools.DIR + "/comp11");
  }

  public static void convert(String dir) throws IOException {
    for (String name : new File(dir).list())
      if (name.endsWith(".pto")) {
        int x0 = name.charAt(3) - '0';
        int y0 = name.charAt(4) - '0';
        
        int x1 = name.charAt(6) - '0';
        int y1 = name.charAt(7) - '0';
        
        BufferedReader in = new BufferedReader(new FileReader(dir + "/" + name));
        PrintStream out = new PrintStream(dir + "/" + name + ".2");

        for (int i = 0; i != 7; ++i)
          in.readLine();

        out.println("# hugin project file");
        out.println("#hugin_ptoversion 2");
        out.println("p f2 w3000 h1500 v360  E0 R0 n\"TIFF_m c:NONE\"");
        out.println("m g1 i0 m2 p0.00784314");
        out.println("");
        out.println("# image lines");
        out.println("#-hugin  cropFactor=1");
        out.println("i w1625 h1225 f0 Eb1 Eev0 Er1 Ra0 Rb0 Rc0 Rd0 Re0 Va1 Vb0 Vc0 Vd0 Vx0 Vy0 a0 b-0.01 c0 d0 e0 g0 p0 r0 t0 v0.395905434409552 y0  Vm5 u10 n\"" + x0 + "-" + y0 + ".PNG\"");
        out.println("#-hugin  cropFactor=1");
        out.println("i w1625 h1225 f0 Eb1 Eev0 Er1 Ra0 Rb0 Rc0 Rd0 Re0 Va1 Vb0 Vc0 Vd0 Vx0 Vy0 a=0 b=0 c=0 d0 e0 g0 p0 r0 t0 v=0 y0  Vm5 u10 n\"" + x1 + "-" + y1 + ".PNG\"");
        out.println("");
        
        while (true) {
          String line = in.readLine();
          if (line == null)
            break;
          
          out.println(line);
        }
        
        in.close();
        out.close();
      }
  }

}
