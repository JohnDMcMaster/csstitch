package general.collections.lists;

import java.util.AbstractList;

public class DoubleList extends AbstractList<Double> {
  
  private double[] array;
  
  public DoubleList(double[] array) {
    this.array = array;
  }
  
  public int size() {
    return array.length;
  }
  
  public Double get(int index) {
    return array[index];
  }
  
  public Double set(int index, Double element) {
    double previous = array[index];
    array[index] = element;
    return previous;
  }
  
}
