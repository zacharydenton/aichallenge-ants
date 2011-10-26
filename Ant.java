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

    public void beforeTurn() {
        moved = false;
        if (this.goal != null) {
            if ((this.position.equals(this.goal)) || (this.goalIlk != ants.getIlk(this.goal))) {
                this.exploreArea();
            }
        }
    }

    public void afterTurn() {
        if (this.goal != null) {
            if (this.path != null) {
                //Logger.getAnonymousLogger().warning("ant at " + this.position + " is on a path to the " + this.goalIlk + " at " + this.goal + ".");
                //Logger.getAnonymousLogger().warning("current path is: " + this.path);
                Tile nextStep = this.path.peek();
                if (nextStep != null 
                        && ants.getDistance(this.position, nextStep) == 1 
                        && this.move(nextStep)) {
                    //Logger.getAnonymousLogger().warning("successfully moved to " + nextStep);
                    this.path.removeFirst();
                } else {
                    // couldn't move on path
                    //Logger.getAnonymousLogger().warning("could not move to " + nextStep);
                    // pause
                    setIdle();
                }
            } else {
                if (!this.move(this.goal)) {
                    this.setIdle();
                }
            }
        } 
        if (this.isIdle()) {
            this.exploreArea();
        }
    }

    public boolean isIdle() {
        return !moved;
    }

    public boolean isExploring() {
        return exploring;
    }

    public void setIdle() {
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
        this.path = bot.findPath(this.position, goal);
        if (this.path != null) {
            stopExploring();
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
                Tile destinationA = ants.getTile(Ant.this.position, a);
                Tile destinationB = ants.getTile(Ant.this.position, b);
                return Ant.this.ants.getDistance(tile, destinationB) - Ant.this.ants.getDistance(tile, destinationA);
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

    public void exploreArea() {
        // lefty bot
        // new ants should move straight.
        exploring = true;
        if (currentDirection == null) {
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

    public PriorityQueue<Tile> explorationTargets() {
        PriorityQueue<Tile> targets = new PriorityQueue<Tile>(1, new Comparator<Tile>() {
            public int compare(Tile a, Tile b) {
                return Ant.this.getDistance(b) - Ant.this.getDistance(a);
            }
        });
        targets.addAll(getVision());
        return targets;
    }
}
