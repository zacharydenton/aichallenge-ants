import java.util.*;

public class Ant {

    private Ants ants;
    private MyBot bot;
    public Tile position;

    public Ant(Ants ants, MyBot bot, Tile position) {
        this.ants = ants;
        this.bot = bot;
        this.position = position;
    }

    public boolean move(Aim direction) {
        Tile destination = ants.getTile(this.position, direction);
        Ilk ilk = ants.getIlk(destination);
        if (ilk.isTakable() || ilk.isUnoccupied()) {
            ants.issueOrder(this.position, direction);
            return true;
        } else {
            return false;
        }
    }

    public boolean move(Tile destination) {
        if (ants.getIlk(destination).isPassable()) {
            List<Aim> directions = ants.getDirections(this.position, destination);
            Collections.shuffle(directions);
            for (Aim direction : directions) {
                if (this.move(direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Ant> getAllies(Ants ants, Tile ant, int radius) {
        ArrayList<Ant> allies = new ArrayList<Ant>();
        for (Ant otherAnt : bot.myAnts) {
            if (this.position == otherAnt.position)
                continue;
            if (ants.getDistance(this.position, otherAnt.position) <= radius) {
                allies.add(otherAnt);
            }
        }
        return allies;
    }
}
