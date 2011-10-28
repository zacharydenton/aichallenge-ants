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

    public HashSet<Tile> enemyHills;
    private HashSet<Tile> capturedHills;
    private ArrayList<ArrayList<Node>> targetNodeGrid;
    private ArrayList<ArrayList<Node>> antNodeGrid;
    public ArrayList<Ant> myAnts;
    public HashMap<Tile, Ant> antMap;
    private long startTime;

    public void setup(int loadTime, int turnTime, int rows, 
            int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2) {

        setAnts(new Ants(loadTime, turnTime, rows, 
                    cols, turns, viewRadius2, 
                    attackRadius2, spawnRadius2));

        enemyHills = new HashSet<Tile>();
        capturedHills = new HashSet<Tile>();
        myAnts = new ArrayList<Ant>();
        antMap = new HashMap<Tile, Ant>();
    }

    private void generateTargetNodeGrid() {
        targetNodeGrid = new ArrayList<ArrayList<Node>>();
        for (int row = 0; row < getAnts().getRows(); row++) {
            targetNodeGrid.add(new ArrayList<Node>());
            for (int col = 0; col < getAnts().getCols(); col++) {
                Tile position = new Tile(row, col);
                Ilk ilk = getAnts().getIlk(position);
                boolean walkable = ilk.isPassable() && !getAnts().getMyHills().contains(position) && !Ant.reserved.contains(position);
                targetNodeGrid.get(row).add(new Node(position, walkable, null));
            }
        }
    }

    private void resetTargetNodeGrid() {
        for (ArrayList<Node> nodeArray : targetNodeGrid) {
            for (Node node : nodeArray) {
                node.parent = null;
            }
        }
    }

    private void clearTargetNodeGrid() {
        this.targetNodeGrid = null;
    }

    private void generateAntNodeGrid() {
        antNodeGrid = new ArrayList<ArrayList<Node>>();
        for (int row = 0; row < getAnts().getRows(); row++) {
            antNodeGrid.add(new ArrayList<Node>());
            for (int col = 0; col < getAnts().getCols(); col++) {
                Tile position = new Tile(row, col);
                Ilk ilk = getAnts().getIlk(position);
                boolean walkable = ilk.isPassable() && !getAnts().getMyHills().contains(position) && !Ant.reserved.contains(position) && getAnts().isVisible(position);
                antNodeGrid.get(row).add(new Node(position, walkable, null));
            }
        }
    }

    public int distanceToBase(Tile tile) {
        int minDistance = 999999;
        for (Tile hill : getAnts().getMyHills()) {
            int dist = getAnts().getDistance(tile, hill);
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
    }

    private void resetAntNodeGrid() {
        for (ArrayList<Node> nodeArray : antNodeGrid) {
            for (Node node : nodeArray) {
                node.parent = null;
            }
        }
    }

    private void clearAntNodeGrid() {
        this.antNodeGrid = null;
    }

    public LinkedList<Tile> findPath(final Tile start, final Tile finish) {
        if (System.nanoTime() - startTime > 700000000) { return null; }

        if (this.antNodeGrid == null) {
            generateAntNodeGrid();
        } else {
            resetAntNodeGrid();
        }

        PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
            public int compare(Node a, Node b) {
                return (a.length() + getAnts().getDistance(a.position, finish)) - (b.length() + getAnts().getDistance(b.position, finish));
            }
        });
        frontierQueue.add(antNodeGrid.get(start.getRow()).get(start.getCol()));
        HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
        HashSet<Node> exploredSet = new HashSet<Node>();
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));

        while (frontierQueue.size() > 0) {
            Node path = frontierQueue.poll();
            frontierSet.remove(path);
            exploredSet.add(path);

            if (path.position.equals(finish)) { // stop if we only have 0.3 seconds left
                return path.retracePath();
            }

            for (Aim direction : directions) {
                Tile neighborTile = getAnts().getTile(path.position, direction);
                Node neighbor = antNodeGrid.get(neighborTile.getRow()).get(neighborTile.getCol());
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

    public void attackEnemyHills() {
        //Logger.getAnonymousLogger().warning("enemy hills at: " + enemyHills);
        if ( enemyHills.size() == 0 ) { return; }
        ArrayList<Ant> closeAnts = new ArrayList<Ant>();
        for (Ant ant : myAnts) {
            ArrayList<Tile> closestHills = ant.distanceSort(enemyHills);
            Tile hill = closestHills.get(0);
            if (ant.goal == null || !ant.goal.equals(hill)) {
                if (ant.getDistance(hill) < 300) {
                    closeAnts.add(ant);
                } else {
                    ant.preferredDirections = getAnts().getDirections(ant.position, hill);
                }
            }
        }
        takeTargets(enemyHills, closeAnts);
        //Logger.getAnonymousLogger().warning("elapsed time after attacking enemy hills: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");
    }

    public void attackEnemyAnts() {
        for (final Ant ant : myAnts) {
            if (ant.enemiesNearby().size() > 0) {
                PriorityQueue<Tile> closestEnemies = new PriorityQueue<Tile>(1, new Comparator<Tile>() {
                    public int compare(Tile a, Tile b) {
                        return ant.getDistance(a) - ant.getDistance(b);
                    }
                });
                closestEnemies.addAll(ant.enemiesNearby());
                final Tile closestEnemy = closestEnemies.peek();
                PriorityQueue<Ant> closestAllies = new PriorityQueue<Ant>(1, new Comparator<Ant>() {
                    public int compare(Ant a, Ant b) {
                        return a.getDistance(closestEnemy) - b.getDistance(closestEnemy);
                    }
                });
                closestAllies.addAll(ant.alliesNearby());
                closestAllies.add(ant);
                if (closestAllies.size() > 2) {
                    for (Ant ally : closestAllies) {
                        ally.setGoal(closestEnemy);
                    }
                } 
            }
        }
    }

    public void findFood() {
        HashSet<Tile> claimedFood = new HashSet<Tile>();
        HashSet<Tile> unclaimedFood = new HashSet<Tile>(getAnts().getFoodTiles());
        for (Ant ant : myAnts) {
            if (ant.goal != null && getAnts().getFoodTiles().contains(ant.goal)) {
                claimedFood.add(ant.goal);
            }
        }
        unclaimedFood.removeAll(claimedFood);
        //Logger.getAnonymousLogger().warning("claimed food: " + claimedFood);
        //Logger.getAnonymousLogger().warning("unclaimed food: " + unclaimedFood);
        if ( unclaimedFood.size() == 0 ) { return; }
        ArrayList<Ant> closeAnts = new ArrayList<Ant>();
        for (Ant ant : myAnts) {
            if (ant.goal == null || ant.isExploring()) {
                ArrayList<Tile> closestFood = ant.distanceSort(unclaimedFood);
                for (Tile food : unclaimedFood) {
                    if (ant.getVision().contains(food)) {
                        closeAnts.add(ant);
                    }
                    break;
                }
            }
        }
        takeTargets(unclaimedFood, closeAnts);
        //Logger.getAnonymousLogger().warning("elapsed time after finding food: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");
    }

    public void exploreMap() {
        for (Ant ant : myAnts) {
            if (ant.goal == null && ant.isIdle()) {
                ant.breadthFirstExplore();
            }
        }
        //Logger.getAnonymousLogger().warning("elapsed time after exploring base: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");
    }

    public void takeTargets(Set<Tile> targets, List<Ant> theAnts) {
        // simultaneous A* from all target tiles to closest idle ants

        if (targets.size() == 0) { return; }

        final HashMap<Tile, Ant> idleMap = new HashMap<Tile, Ant>();

        for (Ant ant : theAnts) {
            idleMap.put(ant.position, ant);
        }

        //Logger.getAnonymousLogger().warning("finding paths to " + targets + " from ants at " + idleMap.values());
        if (idleMap.keySet().size() == 0) { return; }


        if (this.targetNodeGrid == null) {
            generateTargetNodeGrid();
        } else {
            resetTargetNodeGrid();
        }

        PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
            public int compare(Node a, Node b) {
                LinkedList<Integer> aDistances = new LinkedList<Integer>();
                LinkedList<Integer> bDistances = new LinkedList<Integer>();
                for (Ant ant : idleMap.values()) {
                    aDistances.add(new Integer(getAnts().getDistance(a.position, ant.position)));
                    bDistances.add(new Integer(getAnts().getDistance(b.position, ant.position)));
                }
                Collections.sort(aDistances);
                Collections.sort(bDistances);
                int aMinDistance;
                try {
                    aMinDistance = aDistances.pop();
                } catch (Exception e) {
                    aMinDistance = 0;
                }
                int bMinDistance;
                try {
                    bMinDistance = bDistances.pop();
                } catch (Exception e) {
                    bMinDistance = 0;
                }
                return (a.length() + aMinDistance) - (b.length() + bMinDistance);
            }
        });
        for (Tile target : targets) {
            frontierQueue.add(targetNodeGrid.get(target.getRow()).get(target.getCol()));
        }
        HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
        HashSet<Node> exploredSet = new HashSet<Node>();
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        //Logger.getAnonymousLogger().warning("finding seekers for the following: ");
        for (Tile target : targets) {
            //Logger.getAnonymousLogger().warning("the " + getAnts().getIlk(target) + " at " + target);
        }

        while (frontierQueue.size() > 0 && idleMap.keySet().size() > 0 && (System.nanoTime() - startTime) < 800000000) {
            Node path = frontierQueue.poll();
            frontierSet.remove(path);
            exploredSet.add(path);

            //Logger.getAnonymousLogger().warning("now at: " + path.position + " (current path: " + path.retracePath() + ")");

            if (idleMap.containsKey(path.position)) {
                Ant ant = idleMap.get(path.position);
                LinkedList<Tile> thePath = path.retracePath();
                Collections.reverse(thePath);
                ant.setGoal(thePath);
                ant.exploring = false;
                idleMap.remove(path.position);
                if (idleMap.keySet().size() == 0) {
                    return;
                }
            }

            for (Aim direction : directions) {
                Tile neighborTile = getAnts().getTile(path.position, direction);
                Node neighbor = targetNodeGrid.get(neighborTile.getRow()).get(neighborTile.getCol());
                if (neighbor.walkable || idleMap.containsKey(neighborTile)) {
                    if (!frontierSet.contains(neighbor) && !exploredSet.contains(neighbor)) {
                        neighbor.parent = path;
                        frontierQueue.add(neighbor);
                        frontierSet.add(neighbor);
                    }
                }
            }
        }
    }

    public void takeTarget(Tile target) {
        // A* from a target tile to closest idle ant
        //Logger.getAnonymousLogger().warning("finding seeker for " + target);
        final HashMap<Tile, Ant> idleMap = new HashMap<Tile, Ant>();

        for (Ant ant : myAnts) {
            if (ant.goal == null && (ant.isIdle() || ant.isExploring())) {
                idleMap.put(ant.position, ant);
            }
        }
        //Logger.getAnonymousLogger().warning("idle ants: " + idleMap.keySet());
        if (idleMap.keySet().size() == 0) { return; }


        if (this.targetNodeGrid == null) {
            generateTargetNodeGrid();
        } else {
            resetTargetNodeGrid();
        }

        PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
            public int compare(Node a, Node b) {
                LinkedList<Integer> aDistances = new LinkedList<Integer>();
                LinkedList<Integer> bDistances = new LinkedList<Integer>();
                for (Ant ant : idleMap.values()) {
                    aDistances.add(new Integer(getAnts().getDistance(a.position, ant.position)));
                    bDistances.add(new Integer(getAnts().getDistance(b.position, ant.position)));
                }
                Collections.sort(aDistances);
                Collections.sort(bDistances);
                int aMinDistance;
                try {
                    aMinDistance = aDistances.pop();
                } catch (Exception e) {
                    aMinDistance = 0;
                }
                int bMinDistance;
                try {
                    bMinDistance = bDistances.pop();
                } catch (Exception e) {
                    bMinDistance = 0;
                }
                return (a.length() + aMinDistance) - (b.length() + bMinDistance);
            }
        });
        frontierQueue.add(targetNodeGrid.get(target.getRow()).get(target.getCol()));
        HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
        HashSet<Node> exploredSet = new HashSet<Node>();
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        //Logger.getAnonymousLogger().warning("finding paths from " + frontierSet + " to " + idleMap.keySet());

        while (frontierQueue.size() > 0 && idleMap.keySet().size() > 0) {
            Node path = frontierQueue.poll();
            frontierSet.remove(path);
            exploredSet.add(path);

            //Logger.getAnonymousLogger().warning("now at: " + path.position + " (current path: " + path.retracePath() + ")");

            if (idleMap.containsKey(path.position)) {
                Ant ant = idleMap.get(path.position);
                LinkedList<Tile> thePath = path.retracePath();
                Collections.reverse(thePath);
                ant.setGoal(thePath);
                ant.exploring = false;
                return;
            }

            for (Aim direction : directions) {
                Tile neighborTile = getAnts().getTile(path.position, direction);
                Node neighbor = targetNodeGrid.get(neighborTile.getRow()).get(neighborTile.getCol());
                if (neighbor.walkable || idleMap.containsKey(neighborTile)) {
                    if (!frontierSet.contains(neighbor) && !exploredSet.contains(neighbor)) {
                        neighbor.parent = path;
                        frontierQueue.add(neighbor);
                        frontierSet.add(neighbor);
                    }
                }
            }
        }
    }


    public void doTurn() {
        startTime = System.nanoTime();
        Ants ants = getAnts();
        clearTargetNodeGrid();
        clearAntNodeGrid();
        Ant.updateNodeGrid(ants);

        // remove dead ants
        ArrayList<Ant> oldAnts = new ArrayList<Ant>(myAnts);
        HashSet<Tile> antPositions = new HashSet<Tile>();
        for (Ant ant : oldAnts) {
            if (!ants.getMyAnts().contains(ant.position)) { // ant has died
                myAnts.remove(ant);
                Ant.reserved.remove(ant.position);
            } else {
                antPositions.add(ant.position);
            }
        }

        //Logger.getAnonymousLogger().warning("elapsed time after removing dead ants: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        // add new ants
        HashSet<Tile> newAnts = new HashSet<Tile>(ants.getMyAnts());
        newAnts.removeAll(antPositions); // only consider ants we haven't seen before
        for (Tile position : newAnts) {
            myAnts.add(new Ant(ants, this, position));
        }

        for (Ant ant : myAnts) {
            antMap.put(ant.position, ant);
        }

        //Logger.getAnonymousLogger().warning("elapsed time after adding new ants: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        //Logger.getAnonymousLogger().warning("elapsed time after updating unseen squares: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        // remember enemy hills
        for (Tile hillLoc : ants.getEnemyHills()) {
            if (!capturedHills.contains(hillLoc)) {
                enemyHills.add(hillLoc);
            }
        }

        // remove captured hills
        for (Tile hillLoc : enemyHills) {
            if (ants.getMyAnts().contains(hillLoc)) {
                capturedHills.add(hillLoc);
            }
        }
        for (Tile hillLoc : capturedHills) {
            if (enemyHills.contains(hillLoc)) {
                enemyHills.remove(hillLoc);
            }
        }

        //Logger.getAnonymousLogger().warning("elapsed time after remembering enemy hills: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        // prepare ants
        for (Ant ant : myAnts) {
            ant.beforeTurn();
        }

        //Logger.getAnonymousLogger().warning("elapsed time after preparing ants: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        // move ants away from the base
        for (Ant ant : myAnts) {
            if (ants.getMyHills().contains(ant.position)) {
                if (!ant.initialMovePreferred()) {
                    ant.moveAwayFromBase();
                }
            }
        }


        //Logger.getAnonymousLogger().warning("elapsed time after moving ants off the base: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");

        // assign goals
        attackEnemyHills();
        findFood();
        attackEnemyAnts();
        exploreMap();

        // move ants
        for (Ant ant : myAnts) {
            ant.afterTurn();
        }
        //Logger.getAnonymousLogger().warning("total time elapsed: " + (System.nanoTime() - startTime) / 1000000000.0f + " seconds");
    }
}
