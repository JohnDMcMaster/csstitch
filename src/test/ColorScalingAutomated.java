package test;

import java.io.IOException;

public class ColorScalingAutomated {

  public static void main(String[] args) throws IOException, InterruptedException {
    System.out.println(args[0] + ":");
    Runtime runtime = Runtime.getRuntime();
    ColorScaling.main(new String[] {args[0], "/tmp/comp"});
    for (int i = 0; i != 4; ++i)
      for (int j = 0; j != 4; ++j)
        if (i != j) {
          Process process = runtime
              .exec("autopano-sift-c --maxmatches 0 --projection 0,0.05 /tmp/matches-" + i + "-"
                  + j + " /tmp/comp-" + i + ".png /tmp/comp-" + j + ".png");
          int err = process.waitFor();
          if (err != 0)
            System.err.println("autopano exit code: " + err);

          System.out.println(i + "-" + j + ":");
          ColorScalingCorrect.main(new String[] {"/tmp/matches-" + i + "-" + j, args[1]});
        }
  }

}
