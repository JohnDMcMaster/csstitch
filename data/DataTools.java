package data;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TreeMap;

public class DataTools {

  public static final String DIR = getDir();
  public static final boolean INTERACTIVE = new File(".").getAbsolutePath().contains(".eclipse");

  public static String getDir() {
    String filename = new File(".").getAbsolutePath();
    String temp = filename.substring(0, filename.lastIndexOf('/') + 1);
    if (!temp.contains("/."))
      return temp;

    if (new File("/home/noname/decapsulation/image-processing").exists())
      return "/home/noname/decapsulation/image-processing/";

    if (new File("/data/micrographs").exists())
      return "/data/micrographs/";

    return "/home/noname";
  }

  public static DataInputStream openReading(String filename) throws IOException {
    return new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
  }

  public static DataOutputStream openWriting(String filename) throws IOException {
    return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
  }

  public static DataInputStream openReadingDir(String filename) throws IOException {
    return openReading(DIR + filename);
  }

  public static <T> T readObject(String filename) throws IOException {
    try {
      return (T) new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)))
          .readUnshared();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T> void writeObject(String filename, T object) throws IOException {
    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
        filename)));
    out.writeUnshared(object);
    out.close();
  }

  public static int[][] readMatrixInt(DataInputStream in) throws IOException {
    int sx = in.readInt();
    int sy = in.readInt();

    int[][] matrix = new int[sy][sx];

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        matrix[y][x] = in.readInt();

    return matrix;
  }

  public static void writeMatrixInt(DataOutputStream out, int[][] matrix) throws IOException {
    int sx = matrix.length == 0 ? 0 : matrix[0].length;
    int sy = matrix.length;

    out.writeInt(sx);
    out.writeInt(sy);

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        out.writeInt(matrix[y][x]);
  }

  public static double[][] readMatrixDouble(DataInputStream in) throws IOException {
    int sx = in.readInt();
    int sy = in.readInt();

    double[][] matrix = new double[sy][sx];

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        matrix[y][x] = in.readDouble();

    return matrix;
  }

  public static void writeMatrixDouble(DataOutputStream out, double[][] matrix) throws IOException {
    int sx = matrix.length == 0 ? 0 : matrix[0].length;
    int sy = matrix.length;

    out.writeInt(sx);
    out.writeInt(sy);

    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x)
        out.writeDouble(matrix[y][x]);
  }

  public static Rectangle readRectangle(DataInputStream in) throws IOException {
    int x = in.readInt();
    int y = in.readInt();
    int xx = in.readInt();
    int yy = in.readInt();
    int x0 = Math.min(x, xx);
    int y0 = Math.min(y, yy);
    int x1 = Math.max(x, xx);
    int y1 = Math.max(y, yy);
    return new Rectangle(x0, y0, x1 - x0, y1 - y0);
  }

  public static void writeRectangle(DataOutputStream out, Rectangle rectangle) throws IOException {
    out.writeInt(rectangle.x);
    out.writeInt(rectangle.y);
    out.writeInt(rectangle.x + rectangle.width);
    out.writeInt(rectangle.y + rectangle.height);
  }

  public static Rectangle[] readRectangles(DataInputStream in) throws IOException {
    int n = in.readInt();
    Rectangle[] rectangles = new Rectangle[n];
    for (int i = 0; i != n; ++i)
      rectangles[i] = readRectangle(in);
    return rectangles;
  }

  public static void writeRectangles(DataOutputStream out, Rectangle[] rectangles)
      throws IOException {
    out.writeInt(rectangles.length);
    for (Rectangle rectangle : rectangles)
      writeRectangle(out, rectangle);
  }

  public static Rectangle[] readRectangles(String filename) throws IOException {
    DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
    Rectangle[] rectangles = readRectangles(in);
    in.close();
    return rectangles;
  }

  static void writeRectangles(String filename, Rectangle[] rectangles) throws IOException {
    DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
        filename)));
    writeRectangles(out, rectangles);
    out.close();
  }

  public static TreeMap<Integer, Double>[] readIntegerDoubleMapArray(DataInputStream in)
      throws IOException {
    TreeMap<Integer, Double>[] matrix = new TreeMap[in.readInt()];
    for (int i = 0; i != matrix.length; ++i) {
      matrix[i] = new TreeMap<Integer, Double>();
      for (int j = in.readInt(); j != 0; --j) {
        int key = in.readInt();
        double value = in.readDouble();
        matrix[i].put(key, value);
      }
    }
    return matrix;
  }

  public static void writeIntegerDoubleMapArray(DataOutputStream out,
      TreeMap<Integer, Double>[] matrix) throws IOException {
    out.writeInt(matrix.length);
    for (TreeMap<Integer, Double> map : matrix) {
      out.writeInt(map.size());
      for (Integer i : map.keySet()) {
        out.writeInt(i);
        out.writeDouble(map.get(i));
      }
    }
  }

  public static double[][][][] readDoubleArrayArrayMatrix(DataInputStream in) throws IOException {
    int sx = in.readInt();
    int sy = in.readInt();
    double[][][][] matrix = new double[sy][sx][][];
    for (int y = 0; y != sy; ++y)
      for (int x = 0; x != sx; ++x) {
        matrix[y][x] = new double[in.readInt()][];
        for (int i = 0; i != matrix[y][x].length; ++i) {
          matrix[y][x][i] = new double[in.readInt()];
          for (int j = 0; j != matrix[y][x][i].length; ++j)
            matrix[y][x][i][j] = in.readDouble();
        }
      }
    return matrix;
  }

  public static void writeDoubleArrayArrayMatrix(DataOutputStream out, double[][][][] corrArray)
      throws IOException {
    out.writeInt(corrArray[0].length);
    out.writeInt(corrArray.length);
    for (double[][][] a : corrArray)
      for (double[][] b : a) {
        out.writeInt(b.length);
        for (double[] c : b) {
          out.writeInt(c.length);
          for (double d : c)
            out.writeDouble(d);
        }
      }
  }

}
