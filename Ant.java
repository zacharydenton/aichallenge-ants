import java.util.*;
import java.util.logging.*;

public class Ant {

    private Ants ants;
    private MyBot bot;
    public Tile position;
    private Tile goal;
    private Ilk goalIlk;
    private LinkedList<Tile> path;

    public static HashSet<Tile> reserved = new HashSet<Tile>();

    public Ant(Ants ants, MyBot bot, Tile position) {
        this.ants = ants;
        this.bot = bot;
        this.position = position;
    }

    public boolean move(Aim direction) {
        Tile destination = ants.getTile(this.position, direction);
        Ilk ilk = ants.getIlk(destination);
        if ((ilk.isTakable() || ilk.isUnoccupied()) 
                && !reserved.contains(destination)) {

            ants.issueOrder(this.position, direction);
            this.position = destination;
            reserved.add(destination);
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
            if (this.position.equals(otherAnt.position))
                continue;
            if (ants.getDistance(this.position, otherAnt.position) <= radius) {
                allies.add(otherAnt);
            }
        }
        return allies;
    }

    public void update() {

        //Logger.getAnonymousLogger().warning("starting update for ant at " + this.position);
        reserved.remove(this.position);
        if (!isIdle()) {
            if ((this.position.equals(this.goal)) || 
                    (this.goalIlk != ants.getIlk(this.goal))) {
                this.setIdle();
            } else if (this.path != null) {
                Tile nextStep = this.path.peek();
                if (nextStep != null 
                        && ants.getDistance(this.position, nextStep) == 1 
                        && this.move(nextStep)) {
                    this.path.removeFirst();
                } else {
                    // couldn't move on path
                    this.setIdle();
                }
            } else {
                if (!this.move(this.goal)) {
                    this.setIdle();
                }
            }
        } 
        //Logger.getAnonymousLogger().warning("finished update. ant is now at " + this.position);
    }

    public boolean isIdle() {
        return this.goal == null;
    }

    public void setIdle() {
        this.goal = null;
        this.goalIlk = null;
        this.path = null;
    }


    public void setGoal(Tile goal) {
        this.path = bot.findPath(this.position, goal);
        if (this.path != null) {
            this.goal = goal;
            this.goalIlk = ants.getIlk(this.goal);
            if (this.path.peek().equals(this.position)) {
                this.path.removeFirst();
            }
            //Logger.getAnonymousLogger().warning("now taking path " + this.path + " to " + this.goal);
        }
    }

    public void setGoal(LinkedList<Tile> path) {
        if (path != null) {
            this.path = path;
            this.goal = path.peekLast();
            this.goalIlk = ants.getIlk(this.goal);
            if (this.path.peek().equals(this.position)) {
                this.path.removeFirst();
            }
            //Logger.getAnonymousLogger().warning("now taking path " + this.path + " to " + this.goal);
        }
    }


    public Tile getGoal() {
        return this.goal;
    }

    public int getDistance(Tile other) {
        return ants.getDistance(this.position, other);
    }

    public void randomMove() {
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        Collections.shuffle(directions);
        LinkedList<Tile> longestPath = new LinkedList<Tile>();
        for (Aim direction : directions) {
            LinkedList<Tile> thisPath = new LinkedList<Tile>();
            Tile tile = ants.getTile(this.position, direction);
            while (ants.getIlk(tile).isPassable() && !ants.getMyHills().contains(tile) && !reserved.contains(tile)) {
                thisPath.add(tile);
            }
            if (thisPath.size() > longestPath.size()) {
                longestPath = thisPath;
            }
        }
        if (longestPath.size() > 0) {
            setGoal(longestPath);
        }
    }

    public ArrayList<Tile> distanceSort(Collection<Tile> c) {
        ArrayList<Tile> sorted = new ArrayList<Tile>(c);
        Collections.sort(sorted, new Comparator<Tile>() {
            public int compare(Tile a, Tile b) {
                return Ant.this.getDistance(a) - Ant.this.getDistance(b);
            }
        });
        return sorted;
    }
}

