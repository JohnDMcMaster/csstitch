package ids;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class TaggedObjects<T> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private IdGenerator generator;
  private TreeMap<Long, T> objects;
  
  public TaggedObjects() {
    generator = new IdGenerator();
    objects = new TreeMap<Long, T>();
  }
  
  public T get(long id) {
    return objects.get(id);
  }
  
  public long add(T object) {
    long id = generator.generate();
    objects.put(id, object);
    return id;
  }
  
  public void remove(long id) {
    objects.remove(id);
    generator.release(id);
  }
  
  public Map<Long, T> getMap() {
    return Collections.unmodifiableMap(objects);
  }
  
}
