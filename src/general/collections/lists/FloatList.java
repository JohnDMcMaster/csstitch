package general.collections.lists;

import java.util.AbstractList;

public class FloatList extends AbstractList<Float> {
  
  private float[] array;
  
  public FloatList(float[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Float get(int index) {
    return array[index];
  }
  
  public Float set(int index, Byte element) {
    float previous = array[index];
    array[index] = element;
    return previous;
  }
  
}
