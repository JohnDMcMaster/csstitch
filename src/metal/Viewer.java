package metal;

import general.execution.Bash;
import general.execution.SSH;
import general.execution.SSHAddress;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import data.DataTools;
import data.Tools;

public class Viewer {
  
  public static void matchRecursively(ArrayList<File> files, File directory, Pattern[] patterns,
      int index) {
    for (String name : new TreeSet<String>(Arrays.asList(directory.list()))) {
      if (patterns[index].matcher(name).matches()) {
        File file = new File(directory.getAbsolutePath() + "/" + name);
        if (index == patterns.length - 1) {
          if (file.isFile())
            files.add(file);
        } else if (file.isDirectory())
          matchRecursively(files, file, patterns, index + 1);
      }
    }
  }
  
  public static File[] matchFiles(String regex) {
    String[] components = regex.split("/");
    Pattern[] patterns = new Pattern[components.length];
    for (int i = 0; i != patterns.length; ++i)
      patterns[i] = Pattern.compile(components[i]);
    
    ArrayList<File> files = new ArrayList<File>();
    if (components[0].length() == 0)
      matchRecursively(files, new File("/"), patterns, 1);
    else
      matchRecursively(files, new File(DataTools.DIR), patterns, 0);
    return files.toArray(new File[] {});
  }
  
  public static File[] matchFiles(String... regexes) {
    ArrayList<File> files = new ArrayList<File>();
    for (String regex : regexes)
      files.addAll(Arrays.asList(matchFiles(regex)));
    return files.toArray(new File[] {});
  }
  
  public static void viewImages(File[] files) throws IOException {
    Image[] images = new Image[files.length];
    String[] titles = new String[files.length];
    for (int i = 0; i != files.length; ++i) {
      images[i] = ImageIO.read(files[i]);
      titles[i] = files[i].getCanonicalPath();
    }
    Tools.displayImages(images, titles);
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length != 0) {
      File[] files = new File[args.length];
      for (int i = 0; i != args.length; ++i)
        files[i] = new File(args[i]);
      viewImages(files);
    } else {
      DataInputStream in = new DataInputStream(new BufferedInputStream(System.in));
      boolean hasTitles = in.readBoolean();
      
      BufferedImage[] images = new BufferedImage[in.readInt()];
      String[] titles = hasTitles ? new String[images.length] : null;
      
      for (int i = 0; i != images.length; ++i) {
        images[i] = Tools.readImage(in);
        if (hasTitles)
          titles[i] = in.readUTF();
      }
      
      Tools.displayImages(images, titles);
    }
  }
  
  public static void viewRemotely(SSHAddress address, BufferedImage[] images, String[] titles)
      throws IOException {
    Process process =
        SSH.command(
            address,
            Bash.command("env DISPLAY=:0.0 /opt/java/jre/bin/java -server -Xmx1G -jar /home/noname/di/view.jar"))
            .startErr();
    DataOutputStream out =
        new DataOutputStream(new BufferedOutputStream(process.getOutputStream()));
    out.writeBoolean(titles != null);
    out.writeInt(images.length);
    for (int i = 0; i != images.length; ++i) {
      Tools.writeImage(out, images[i]);
      if (titles != null)
        out.writeUTF(titles[i]);
    }
    out.close();
    try {
      process.waitFor();
    } catch (InterruptedException e) {
    }
  }
  
  public static void viewRemotely(SSHAddress address, BufferedImage[] images) throws IOException {
    viewRemotely(address, images, null);
  }
  
}
