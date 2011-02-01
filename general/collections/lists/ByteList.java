package general.collections.lists;

import java.util.AbstractList;

public class ByteList extends AbstractList<Byte> {
  
  private byte[] array;
  
  public ByteList(byte[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Byte get(int index) {
    return array[index];
  }
  
  public Byte set(int index, Byte element) {
    byte previous = array[index];
    array[index] = element;
    return previous;
  }
  
}
