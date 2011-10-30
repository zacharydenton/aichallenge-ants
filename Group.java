import java.util.*;
public abstract class Group {
    public ArrayList<Ant> ants;
    public Ant leader;

    public Group() {
        ants = new ArrayList<Ant>();
    }

    public void add(Ant ant) {
        ants.add(ant);
    }

    public void addAll(Collection<Ant> c) {
        ants.addAll(c);
    }

    public abstract void update();
}

