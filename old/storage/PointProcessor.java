package old.storage;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PointProcessor implements QuadTreeCreator.Processor<Point> {

  private float margin;
  private Point point = new Point();

  public PointProcessor(float margin) {
    this.margin = margin;
  }

  public Point read(DataInput file) throws IOException {
    point.x = file.readFloat();
    point.y = file.readFloat();
    point.val = file.readFloat();
    point.flags = file.readInt();

    return point;
  }

  public void write(DataOutput file, Point object) throws IOException {
    file.writeFloat(object.x);
    file.writeFloat(object.y);
    file.writeFloat(object.val);
    file.writeInt(object.flags);
  }

  public void getBounds(Point object, float[] boundary) {
    float x = object.x;
    float y = object.y;

    boundary[0] = x - margin;
    boundary[1] = y - margin;
    boundary[2] = x + margin;
    boundary[3] = y + margin;
  }

}
