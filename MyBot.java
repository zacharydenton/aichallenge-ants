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
    private ArrayList<ArrayList<Node>> nodeGrid;
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

    private void generateNodeGrid() {
        nodeGrid = new ArrayList<ArrayList<Node>>();
        for (int row = 0; row < getAnts().getRows(); row++) {
            nodeGrid.add(new ArrayList<Node>());
            for (int col = 0; col < getAnts().getCols(); col++) {
                Tile position = new Tile(row, col);
                Ilk ilk = getAnts().getIlk(position);
                boolean walkable = ilk.isPassable() && !getAnts().getMyHills().contains(position) && !Ant.reserved.contains(position);
                nodeGrid.get(row).add(new Node(position, walkable, null));
            }
        }
    }

    private void resetNodeGrid() {
        for (ArrayList<Node> nodeArray : nodeGrid) {
            for (Node node : nodeArray) {
                node.parent = null;
            }
        }
    }

    private void clearNodeGrid() {
        this.nodeGrid = null;
    }

    public LinkedList<Tile> findPath(Tile start, Tile finish) {
        if (this.nodeGrid == null) {
            generateNodeGrid();
        } else {
            resetNodeGrid();
        }

        Logger.getAnonymousLogger().warning("finding path from " + start + " to " + finish);

        PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
            public int compare(Node a, Node b) {
                return a.length() - b.length();
            }
        });
        frontierQueue.add(nodeGrid.get(start.getRow()).get(start.getCol()));
        HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
        HashSet<Node> exploredSet = new HashSet<Node>();
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));


        while (frontierQueue.size() > 0) {
            Node path = frontierQueue.poll();
            frontierSet.remove(path);
            exploredSet.add(path);

            if (path.position.equals(finish)) {
                Logger.getAnonymousLogger().warning("found a path: " + path.retracePath());
                return path.retracePath();
            }

            for (Aim direction : directions) {
                Tile neighborTile = getAnts().getTile(path.position, direction);
                Node neighbor = nodeGrid.get(neighborTile.getRow()).get(neighborTile.getCol());
                if (neighbor.walkable) {
                    if (!frontierSet.contains(neighbor) && !exploredSet.contains(neighbor)) {
                        neighbor.parent = path;
                        frontierQueue.add(neighbor);
                        frontierSet.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    public void attackEnemyHills(Ants ants) {
        for (Ant ant : myAnts) {
            if (ant.isIdle()) {
                ArrayList<Tile> closestHills = ant.distanceSort(ants.getEnemyHills());
                if (closestHills.size() > 0) {
                    ant.setGoal(closestHills.get(0));
                }
            }
        }
    }

    public void attackEnemyAnts(Ants ants) {
        for (Ant ant : myAnts) {
            if (ant.isIdle()) {
                ArrayList<Tile> closestEnemies = ant.distanceSort(ants.getEnemyAnts());
                if (closestEnemies.size() > 0) {
                    ant.setGoal(closestEnemies.get(0));
                }
            }
        }
    }

    public void findFood(Ants ants) {
        for (Ant ant : myAnts) {
            if (ant.isIdle()) {
                ArrayList<Tile> closestFood = ant.distanceSort(ants.getFoodTiles());
                if (closestFood.size() > 0) {
                    ant.setGoal(closestFood.get(0));
                }
            }
        }
    }

    public void exploreMap(Ants ants) {
        for (Ant ant : myAnts) {
            if (ant.isIdle()) {
                ArrayList<Tile> closestUnseen = ant.distanceSort(unseen);
                if (closestUnseen.size() > 0) {
                    ant.setGoal(closestUnseen.get(0));
                }
            }
        }
    }

    public void doTurn() {
        Ants ants = getAnts();
        clearNodeGrid();

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

        // assign goals
        attackEnemyHills(ants);
        //attackEnemyAnts(ants);
        findFood(ants);
        exploreMap(ants);

        /*
        for (Ant ant : myAnts) {
            if (ants.getMyHills().contains(ant.position)) {
                ant.randomMove();
            }
        }
        */

        // update ants
        for (Ant ant : myAnts) {
            ant.update();
        }
    }
}
