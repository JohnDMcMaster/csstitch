package old.stitcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public strictfp class MemTree {

  public static class Entry {
    private float x, y;
    private float val;
    private int flags;

    public Entry(float x, float y, float val, int flags) {
      this.x = x;
      this.y = y;
      this.val = val;
      this.flags = flags;
    }

    public float getX() {
      return x;
    }

    public float getY() {
      return y;
    }

    public float getVal() {
      return val;
    }

    public int getFlags() {
      return flags;
    }

    public static Entry read(DataInputStream in) throws IOException {
      return new Entry(in.readFloat(), in.readFloat(), in.readFloat(), in.readInt());
    }

    public void write(DataOutputStream out) throws IOException {
      out.writeFloat(x);
      out.writeFloat(y);
      out.writeFloat(val);
      out.writeInt(flags);
    }
  }

  private class Node {
    int x0, y0, x1, y1;
    int level;
    int size;

    Node[][] nodes = new Node[2][2];
    Entry[] entries = new Entry[0];

    public Node(int x, int y, int level) {
      x0 = x;
      y0 = y;

      x1 = x + (minSize << level);
      y1 = y + (minSize << level);

      this.level = level;
    }
  }

  private int minSize;
  private Node globalNode;
  private int numEntries;

  public MemTree(int minSize) {
    this.minSize = minSize;
    globalNode = new Node(0, 0, 0);
    numEntries = 0;
  }
  
  public int GetNumEntries() {
    return numEntries;
  }

  private void increaseGlobalLevel() {
    Node node = new Node(0, 0, globalNode.level + 1);
    node.nodes[0][0] = globalNode;
    globalNode = node;
  }

  private static void addEntryToNodeDirect(Entry entry, Node node) {
    Entry[] entries = new Entry[node.entries.length + 1];
    for (int i = 0; i != node.entries.length; ++i)
      entries[i] = node.entries[i];
    entries[node.entries.length] = entry;
    node.entries = entries;
  }

  private void addEntryToNode(Entry entry, Node node) {
    if (node.level == 0)
      addEntryToNodeDirect(entry, node);
    else {
      int a = (int) (entry.getX() - node.x0) >> (node.level - 1);
      int b = (int) (entry.getY() - node.y0) >> (node.level - 1);

      if (node.nodes[b][a] == null)
        node.nodes[b][a] = new Node(node.x0 + (a << (node.level - 1)), node.y0
            + (b << (node.level - 1)), node.level - 1);

      addEntryToNode(entry, node.nodes[b][a]);
    }
  }

  public void addEntry(Entry entry) {
    while (!(entry.getX() < globalNode.x1 && entry.getY() < globalNode.y1))
      increaseGlobalLevel();

    addEntryToNode(entry, globalNode);
    ++numEntries;
  }

  public static void updateSize(Node node) {
    node.size = 16 + 4 + 16 * node.entries.length + 2 * 2 * 4;

    for (int b = 0; b != 2; ++b)
      for (int a = 0; a != 2; ++a) {
        Node subnode = node.nodes[b][a];
        if (subnode != null) {
          updateSize(subnode);
          node.size += subnode.size;
        }
      }
  }

  private static void writeNode(DataOutputStream out, Node node) throws IOException {
    out.writeInt(node.x0);
    out.writeInt(node.y0);

    out.writeInt(node.x1);
    out.writeInt(node.y1);

    out.writeInt(node.entries.length);
    for (int i = 0; i != node.entries.length; ++i)
      node.entries[i].write(out);

    int pointer = 0;
    for (int b = 0; b != 2; ++b)
      for (int a = 0; a != 2; ++a) {
        if (node.nodes[b][a] == null)
          out.writeInt(-1);
        else {
          out.writeInt(pointer);
          pointer += node.nodes[b][a].size;
        }
      }

    for (int b = 0; b != 2; ++b)
      for (int a = 0; a != 2; ++a)
        if (node.nodes[b][a] != null)
          writeNode(out, node.nodes[b][a]);
  }

  public void write(DataOutputStream out) throws IOException {
    updateSize(globalNode);
    writeNode(out, globalNode);
  }

}
