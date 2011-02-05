package general.collections;

import general.collections.lists.BooleanList;
import general.collections.lists.ByteList;
import general.collections.lists.CharacterList;
import general.collections.lists.DoubleList;
import general.collections.lists.FloatList;
import general.collections.lists.IntegerList;
import general.collections.lists.LongList;
import general.collections.lists.ShortList;

import java.util.List;

public class Lists {
  
  private Lists() {
  }
  
  public static List<Boolean> asList(boolean[] array) {
    return new BooleanList(array);
  }
  
  public static List<Byte> asList(byte[] array) {
    return new ByteList(array);
  }
  
  public static List<Character> asList(char[] array) {
    return new CharacterList(array);
  }
  
  public static List<Double> asList(double[] array) {
    return new DoubleList(array);
  }
  
  public static List<Float> asList(float[] array) {
    return new FloatList(array);
  }
  
  public static List<Integer> asList(int[] array) {
    return new IntegerList(array);
  }
  
  public static List<Long> asList(long[] array) {
    return new LongList(array);
  }
  
  public static List<Short> asList(short[] array) {
    return new ShortList(array);
  }
  
}
