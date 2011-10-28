import java.util.*;
import java.util.logging.*;

public class Ant {

    private Ants ants;
    private MyBot bot;
    public Tile position;
    public Tile goal;
    public Ilk goalIlk;
    public LinkedList<Tile> path;
    private boolean moved;
    private Aim currentDirection;
    private boolean followingWall = false;
    public boolean exploring = false;
    public List<Aim> preferredDirections = new ArrayList<Aim>();
    public int antNumber;
    public Set<Tile> seen = new HashSet<Tile>();


    private static ArrayList<ArrayList<Node>> nodeGrid;
    public static int antCount = 0;
    public static HashSet<Tile> reserved = new HashSet<Tile>();

    public Ant(Ants ants, MyBot bot, Tile position) {
        this.ants = ants;
        this.bot = bot;
        this.position = position;
        this.antNumber = Ant.antCount;
        Ant.antCount++;

        switch (antNumber % 8) {
            case 0:
                preferredDirections.add(Aim.NORTH);
                break;
            case 1:
                preferredDirections.add(Aim.EAST);
                break;
            case 2:
                preferredDirections.add(Aim.SOUTH);
                break;
            case 3:
                preferredDirections.add(Aim.WEST);
                break;
            case 4:
                preferredDirections.add(Aim.NORTH);
                preferredDirections.add(Aim.EAST);
                break;
            case 5:
                preferredDirections.add(Aim.SOUTH);
                preferredDirections.add(Aim.EAST);
                break;
            case 6:
                preferredDirections.add(Aim.SOUTH);
                preferredDirections.add(Aim.WEST);
                break;
            case 7:
                preferredDirections.add(Aim.NORTH);
                preferredDirections.add(Aim.WEST);
                break;
        }


    }

    public static void updateNodeGrid(Ants ants) {
        nodeGrid = new ArrayList<ArrayList<Node>>();
        for (int row = 0; row < ants.getRows(); row++) {
            nodeGrid.add(new ArrayList<Node>());
            for (int col = 0; col < ants.getCols(); col++) {
                Tile position = new Tile(row, col);
                Ilk ilk = ants.getIlk(position);
                boolean walkable = ilk.isPassable() && !ants.getMyHills().contains(position) && !Ant.reserved.contains(position);
                nodeGrid.get(row).add(new Node(position, walkable, null));
            }
        }
    }

    public boolean move(Aim direction) {
        if (moved) { return false; }
        Tile destination = ants.getTile(this.position, direction);
        Ilk ilk = ants.getIlk(destination);
        if ((ilk.isTakable() || ilk.isUnoccupied()) 
                && !reserved.contains(destination)
                && !ants.getMyHills().contains(destination)) {

            ants.issueOrder(this.position, direction);
            reserved.remove(this.position);
            this.position = destination;
            reserved.add(this.position);
            moved = true;
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

    public Set<Ant> alliesNearby() {
        Set<Ant> closeAllies = new HashSet<Ant>();
        /*
           Set<Tile> closeAllyTiles = new HashSet<Tile>(ants.getMyAnts());
           closeAllyTiles.retainAll(getAttackable());
           for (Tile ally : closeAllyTiles) {
           closeAllies.add(bot.antMap.get(ally));
           }
           */
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        for (Aim direction : directions) {
            Tile pos = ants.getTile(this.position, direction);
            if (bot.antMap.containsKey(pos)) {
                closeAllies.add(bot.antMap.get(pos));
            }
        }
        return closeAllies;
    }

    public void beforeTurn() {
        moved = false;
        seen.addAll(getVision());
        if (this.goal != null) {
            if ((this.position.equals(this.goal)) || (this.goalIlk != ants.getIlk(this.goal))) {
                this.clearGoal();
            } else {
                //Logger.getAnonymousLogger().warning("ant at " + this.position + " plans to go to the " + this.goalIlk + " at " + this.goal + " by following the path: " + this.path);
            }
        }
    }

    public void afterTurn() {
        if (this.isIdle() && this.goal != null) {
            if (this.path != null) {
                //Logger.getAnonymousLogger().warning("ant at " + this.position + " is on a path to the " + this.goalIlk + " at " + this.goal + ".");
                //Logger.getAnonymousLogger().warning("current path is: " + this.path);
                Tile nextStep = this.path.peek();
                if (nextStep != null 
                        && ants.getDistance(this.position, nextStep) == 1) {
                    if (this.move(nextStep)) {
                        this.path.removeFirst();
                    } else {
                        clearGoal();
                    }
                } else if (this.move(this.goal)) {
                    clearGoal();
                } else {
                    // couldn't move on path
                    clearGoal();
                }
            } else {
                if (!this.move(this.goal)) {
                    clearGoal();
                }
            }
        } 
        if (this.isIdle()) {
            exploreArea();
        }
    }

    public Set<Tile> enemiesNearby() {
        Set<Tile> closeEnemies = new HashSet<Tile>(ants.getEnemyAnts());
        closeEnemies.retainAll(getVision());
        return closeEnemies;
    }

    public boolean isIdle() {
        return !moved;
    }

    public boolean isExploring() {
        return exploring;
    }

    public void clearGoal() {
        this.goal = null;
        this.goalIlk = null;
        this.path = null;
    }

    public void stopExploring() {
        this.currentDirection = null;
        this.followingWall = false;
        this.exploring = false;
    }

    public boolean setGoal(Tile goal) {
        //Logger.getAnonymousLogger().warning("finding a path to the " + ants.getIlk(goal) + " at " + goal);
        this.path = bot.findPath(this.position, goal);
        if (this.path != null) {
            //Logger.getAnonymousLogger().warning("path found successfully: " + this.path);
            this.goal = goal;
            this.goalIlk = ants.getIlk(this.goal);
            if (this.path.peek().equals(this.position)) {
                this.path.removeFirst();
            }
            return true;
            //Logger.getAnonymousLogger().warning("now taking path " + this.path + " to " + this.goal);
        }
        return false;
    }

    public void setGoal(LinkedList<Tile> path) {
        //Logger.getAnonymousLogger().warning("now taking path " + path + " to " + path.peekLast());
        if (path != null) {
            stopExploring();
            this.path = path;
            this.goal = path.peekLast();
            this.goalIlk = ants.getIlk(this.goal);
            if (this.path.peek().equals(this.position)) {
                this.path.removeFirst();
            }
        }
    }


    public Tile getGoal() {
        return this.goal;
    }

    public int getDistance(Tile other) {
        return ants.getDistance(this.position, other);
    }

    public boolean foodMove() {
        ArrayList<Tile> closestFood = this.distanceSort(ants.getFoodTiles());
        if (closestFood.size() > 0) {
            Tile food = closestFood.get(0);
            for (Aim direction : ants.getDirections(this.position, food)) {
                if (this.move(direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean randomMove() {
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        Collections.shuffle(directions);
        for (Aim direction : directions) {
            Tile destination = ants.getTile(this.position, direction);
            if (this.move(direction)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveAwayFrom(final Tile tile) {
        PriorityQueue<Aim> directions = new PriorityQueue<Aim>(1, new Comparator<Aim>() {
            public int compare(Aim a, Aim b) {
                Tile destinationA = Ant.this.ants.getTile(Ant.this.position, a);
                Tile destinationB = Ant.this.ants.getTile(Ant.this.position, b);
                int distanceA = Ant.this.ants.getDistance(tile, destinationA);
                int distanceB = Ant.this.ants.getDistance(tile, destinationB);
                List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
                int neighborsA = 0;
                int neighborsB = 0;
                for (Aim direction : directions) {
                    Ilk ilkA = Ant.this.ants.getIlk(destinationA, direction);
                    if ((ilkA.isTakable() || ilkA.isUnoccupied()) 
                        && !reserved.contains(Ant.this.ants.getTile(destinationA, direction))
                        && !Ant.this.ants.getMyHills().contains(Ant.this.ants.getTile(destinationA, direction))) {
                        neighborsA++;
                        }
                    Ilk ilkB = Ant.this.ants.getIlk(destinationB, direction);
                    if ((ilkB.isTakable() || ilkB.isUnoccupied()) 
                        && !reserved.contains(Ant.this.ants.getTile(destinationB, direction))
                        && !Ant.this.ants.getMyHills().contains(Ant.this.ants.getTile(destinationB, direction))) {
                        neighborsB++;
                        }

                }

                return (distanceB + neighborsB) - (distanceA + neighborsA);
            }
        });
        directions.addAll(EnumSet.allOf(Aim.class));
        for (Aim direction : directions) {

            if (this.move(direction)) {
                return true;
            }
        }
        return false;
    }

    public boolean moveTowards(final Tile tile) {
        PriorityQueue<Aim> directions = new PriorityQueue<Aim>(1, new Comparator<Aim>() {
            public int compare(Aim a, Aim b) {
                Tile destinationA = Ant.this.ants.getTile(Ant.this.position, a);
                Tile destinationB = Ant.this.ants.getTile(Ant.this.position, b);
                int distanceA = Ant.this.ants.getDistance(tile, destinationA);
                int distanceB = Ant.this.ants.getDistance(tile, destinationB);
                List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
                int neighborsA = 0;
                int neighborsB = 0;
                for (Aim direction : directions) {
                    Ilk ilkA = Ant.this.ants.getIlk(destinationA, direction);
                    if ((ilkA.isTakable() || ilkA.isUnoccupied()) 
                        && !reserved.contains(Ant.this.ants.getTile(destinationA, direction))
                        && !Ant.this.ants.getMyHills().contains(Ant.this.ants.getTile(destinationA, direction))) {
                        neighborsA++;
                        }
                    Ilk ilkB = Ant.this.ants.getIlk(destinationB, direction);
                    if ((ilkB.isTakable() || ilkB.isUnoccupied()) 
                        && !reserved.contains(Ant.this.ants.getTile(destinationB, direction))
                        && !Ant.this.ants.getMyHills().contains(Ant.this.ants.getTile(destinationB, direction))) {
                        neighborsB++;
                        }

                }

                return (distanceA + neighborsA) - (distanceB + neighborsB);
            }
        });
        directions.addAll(EnumSet.allOf(Aim.class));
        for (Aim direction : directions) {

            if (this.move(direction)) {
                return true;
            }
        }
        return false;
    }

    public void moveAwayFromBase() {
        if (ants.getMyHills().size() > 0) {
            int minDistance = 999999;
            Tile closestHill = null;
            for (Tile hill : ants.getMyHills()) {
                int dist = this.getDistance(hill);
                if (dist < minDistance) {
                    minDistance = dist;
                    closestHill = hill;
                }
            }
            if (closestHill != null) {
                this.moveAwayFrom(closestHill);
            }
        }
    }

    public int distanceToBase() {
        int minDistance = 999999;
        for (Tile hill : ants.getMyHills()) {
            int dist = this.getDistance(hill);
            if (dist < minDistance) {
                minDistance = dist;
            }
        }
        return minDistance;
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

    public boolean movePreferred() {
        for (Aim direction : preferredDirections) {
            if (this.move(direction)) {
                Collections.reverse(preferredDirections);
                return true;
            }
        }
        return false;
    }

    public boolean initialMovePreferred() {
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
        for (Aim direction : preferredDirections) {
            Tile destination = ants.getTile(this.position, direction);
            int neighbors = 0;
            for (Aim neighborDirection : directions) {
                Ilk ilk = Ant.this.ants.getIlk(destination, neighborDirection);
                if ((ilk.isTakable() || ilk.isUnoccupied()) 
                        && !reserved.contains(Ant.this.ants.getTile(destination, neighborDirection))
                        && !Ant.this.ants.getMyHills().contains(Ant.this.ants.getTile(destination, neighborDirection))) {
                    neighbors++;
                        }
            }
            if (neighbors > 1 && this.move(direction)) {
                return true;
            }
        }
        return false;
    }

    public void exploreArea() {
        // lefty bot
        // new ants should move straight.
        exploring = true;
        if (currentDirection == null) {
            if (movePreferred()) {
                return;
            } else {
                if (this.position.getRow() % 2 == 0) {
                    if (this.position.getCol() % 2 == 0) {
                        currentDirection = Aim.NORTH;
                    } else {
                        currentDirection = Aim.SOUTH;
                    }
                } else {
                    if (this.position.getCol() % 2 == 0) {
                        currentDirection = Aim.EAST;
                    } else {
                        currentDirection = Aim.WEST;
                    }
                }
            }
        }

        if (!followingWall) {
            Tile destination = ants.getTile(this.position, currentDirection);
            if (ants.getIlk(destination).isPassable() && !ants.getMyHills().contains(destination)) {
                if (!this.move(currentDirection)) {
                    // pause ant, turn and try again next turn
                    moved = true;
                    currentDirection = currentDirection.left();
                } 
            } else {
                // hit a wall, start following it
                currentDirection = currentDirection.right();
                followingWall = true;
            }
        }

        if (followingWall) {
            List<Aim> directions = new ArrayList<Aim>();
            directions.add(currentDirection.left());
            directions.add(currentDirection);
            directions.add(currentDirection.right());
            directions.add(currentDirection.behind());
            // try 4 directions in order, attempting to turn left at corners
            for (Aim direction : directions) {
                Tile destination = ants.getTile(this.position, direction);
                if (ants.getIlk(destination).isPassable() && !ants.getMyHills().contains(destination)) {
                    if (this.move(direction)) {
                        currentDirection = direction;
                        //followingWall = false;
                        break;
                    } else {
                        // pause ant, turn and send straight
                        currentDirection = currentDirection.right();
                        followingWall = false;
                        moved = true;
                        break;
                    }
                }
            }
        }
    }

    public HashSet<Tile> getVision() {
        HashSet<Tile> vision = new HashSet<Tile>();
        for (int i = 0; i < ants.getVisionOffsets().size(); i++) {
            int vRow = (ants.getVisionOffsets().get(i)[0] + this.position.getRow()) % ants.getRows();
            if (vRow < 0) {
                vRow += ants.getRows();
            }
            int vCol = (ants.getVisionOffsets().get(i)[1] + this.position.getCol()) % ants.getCols();
            if (vCol < 0) {
                vCol += ants.getCols();
            }
            vision.add(new Tile(vRow, vCol));
        }
        return vision;
    }

    public HashSet<Tile> getAttackable() {
        HashSet<Tile> attackable = new HashSet<Tile>();
        for (int i = 0; i < ants.getAttackOffsets().size(); i++) {
            int vRow = (ants.getAttackOffsets().get(i)[0] + this.position.getRow()) % ants.getRows();
            if (vRow < 0) {
                vRow += ants.getRows();
            }
            int vCol = (ants.getAttackOffsets().get(i)[1] + this.position.getCol()) % ants.getCols();
            if (vCol < 0) {
                vCol += ants.getCols();
            }
            attackable.add(new Tile(vRow, vCol));
        }
        return attackable;
    }

    public PriorityQueue<Tile> explorationTargets() {

        PriorityQueue<Tile> targets = new PriorityQueue<Tile>(1, new Comparator<Tile>() {
            public int compare(Tile a, Tile b) {
                return Ant.this.getDistance(b) - Ant.this.getDistance(a);
            }
        });
        Tile pos = ants.getTile(this.position, preferredDirections.get(0));
        for (int i = 0; i < 5; i++) {
            if (ants.getIlk(pos).isPassable()
                    && !ants.getMyHills().contains(pos)
                    && !Ant.reserved.contains(pos)
                    && this.getVision().contains(pos)) {
                targets.add(pos);
                    }
            pos = ants.getTile(pos, preferredDirections.get(0));
            Collections.reverse(preferredDirections);
        }
        return targets;
    }

    public void breadthFirstExplore() {
        // does a breadth first search until a tile this ant hasn't seen
        // is encountered.
        for (ArrayList<Node> nodeArray : nodeGrid) {
            for (Node node : nodeArray) {
                node.parent = null;
            }
        }
        PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
            public int compare(Node a, Node b) {
                return a.length() - b.length();
            }
        });
        frontierQueue.add(nodeGrid.get(this.position.getRow()).get(this.position.getCol()));
        HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
        HashSet<Node> exploredSet = new HashSet<Node>();
        List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));

        while (frontierQueue.size() > 0) {
            Node path = frontierQueue.poll();
            frontierSet.remove(path);
            exploredSet.add(path);

            if (!seen.contains(path.position)) { 
                setGoal(path.retracePath());
                exploring = true;
                return;
            }

            for (Aim direction : directions) {
                Tile neighborTile = ants.getTile(path.position, direction);
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
    }

}
