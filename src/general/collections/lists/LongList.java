package general.collections.lists;

import java.util.AbstractList;

public class LongList extends AbstractList<Long> {
  
  private long[] array;
  
  public LongList(long[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Long get(int index) {
    return array[index];
  }
  
  public Long set(int index, Long element) {
    long previous = array[index];
    array[index] = element;
    return previous;
  }
  
}
