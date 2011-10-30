public class HorizontalGroup extends Group {
    public void update() {
        this.leader = ants.get(0);
        if (ants.size() == 0) { return; }
        int i = 0;
        for (Ant ant : ants) {
            ant.setGoal(getGoal(i));
            i++;
        }
    }

    public Tile getGoal(int index) {
        return new Tile(0,0);
    }
}

