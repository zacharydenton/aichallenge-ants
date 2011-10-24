import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.util.logging.*;

/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     * 
     * @param args command line arguments
     * 
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot().readSystemInput();
    }

    private HashSet<Tile> unseen;
    public ArrayList<Ant> myAnts;

    public void setup(int loadTime, int turnTime, int rows, 
            int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2) {

        setAnts(new Ants(loadTime, turnTime, rows, 
                    cols, turns, viewRadius2, 
                    attackRadius2, spawnRadius2));

        unseen = new HashSet<Tile>();
        for (int r = 0; r < getAnts().getRows(); r++) {
            for (int c = 0; c < getAnts().getCols(); c++) {
                unseen.add(new Tile(r, c));
            }
        }

        myAnts = new ArrayList<Ant>();
    }

    public Set<Tile> getRandomExplorationTargets(Ants ants, int numTargets) {
        HashSet<Tile> targets = new HashSet<Tile>();
        for (int i = 0; i < numTargets; i++) {
            int row = (int)(Math.random() * (ants.getRows() + 1));
            int col = (int)(Math.random() * (ants.getCols() + 1));
            Tile target = new Tile(row, col);
            while (!unseen.contains(target)) {
                row = (int)(Math.random() * (ants.getRows() + 1));
                col = (int)(Math.random() * (ants.getCols() + 1));
                target = new Tile(row, col);
            }
            targets.add(target);
        }
        return targets;
    }

    public void doTurn() {
        Ants ants = getAnts();

        // remove dead ants
        ArrayList<Ant> oldAnts = new ArrayList<Ant>(myAnts);
        HashSet<Tile> antPositions = new HashSet<Tile>();
        for (Ant ant : oldAnts) {
            if (!ants.getMyAnts().contains(ant.position)) { // ant has died
                myAnts.remove(ant);
            } else {
                antPositions.add(ant.position);
            }
        }

        // add new ants
        HashSet<Tile> newAnts = new HashSet<Tile>(ants.getMyAnts());
        newAnts.removeAll(antPositions); // only consider ants we haven't seen before
        for (Tile position : newAnts) {
            myAnts.add(new Ant(ants, this, position));
        }

        // update unseen squares
        HashSet<Tile> oldUnseen = new HashSet<Tile>(unseen);
        for (Tile loc : oldUnseen) {
            if (ants.isVisible(loc)) {
                unseen.remove(loc);
            }
        }

        // avoid blocking the hill
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        for (Ant ant : myAnts) {
            if (ants.getMyHills().contains(ant.position)) {
                for (Aim direction : directions) {
                    if (ant.move(direction)) {
                        break;
                    }
                }
            }
        }
    }
}
