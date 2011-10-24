import java.util.*;
import java.util.logging.*;

class Node {
    public Tile position;
    public boolean walkable;
    public Node parent;

    public Node(Tile position, boolean walkable, Node parent) {
        this.position = position;
        this.walkable = walkable;
        this.parent = parent;
    }

    public int length() {
        int i = 0;
        Node p = this.parent;
        while (p != null) {
            p = p.parent;
            i++;
        }
        return i;
    }

    public LinkedList<Tile> retracePath() {
        LinkedList<Tile> tilePath = new LinkedList<Tile>();
        tilePath.addFirst(this.position);
        Node parent = this.parent;
        while (parent != null) {
            tilePath.addFirst(parent.position);
            parent = parent.parent;
        }
        return tilePath;
    }
}


