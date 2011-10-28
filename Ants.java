import java.util.*;
import java.util.logging.*;

/**
 * Holds all game data and current game state.
 */
public class Ants {
	/** Maximum map size. */
	public static final int MAX_MAP_SIZE = 256;

	private final int loadTime;

	private final int turnTime;

	private final int rows;

	private final int cols;

	private final int turns;

	private final int viewRadius2;

	private final int attackRadius2;

	private final int spawnRadius2;

	public boolean[][] visionMatrix;
	public ArrayList<int[]> visionOffsets2;
	public ArrayList<int[]> attackOffsets2;

	private long turnStartTime;

	private final Ilk map[][];

	private final Set<Tile> myAnts = new HashSet<Tile>();

	private final Set<Tile> enemyAnts = new HashSet<Tile>();

	private final Set<Tile> myHills = new HashSet<Tile>();

	private final Set<Tile> enemyHills = new HashSet<Tile>();

	private final Set<Tile> foodTiles = new HashSet<Tile>();

	private final Set<Order> orders = new HashSet<Order>();

	/**
	 * Creates new {@link Ants} object.
	 * 
	 * @param loadTime timeout for initializing and setting up the bot on turn 0
	 * @param turnTime timeout for a single game turn, starting with turn 1
	 * @param rows game map height
	 * @param cols game map width
	 * @param turns maximum number of turns the game will be played
	 * @param viewRadius2 squared view radius of each ant
	 * @param attackRadius2 squared attack radius of each ant
	 * @param spawnRadius2 squared spawn radius of each ant
	 */
	public Ants(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
			int attackRadius2, int spawnRadius2) {
		this.loadTime = loadTime;
		this.turnTime = turnTime;
		this.rows = rows;
		this.cols = cols;
		this.turns = turns;
		this.viewRadius2 = viewRadius2;
		this.attackRadius2 = attackRadius2;
		this.spawnRadius2 = spawnRadius2;
		this.visionMatrix = null;
		map = new Ilk[rows][cols];
		for (Ilk[] row : map) {
			Arrays.fill(row, Ilk.LAND);
		}

		int vr = (int)Math.floor(Math.sqrt(this.viewRadius2));
		this.visionOffsets2 = new ArrayList<int[]>();
		for (int dRow = -vr; dRow <= vr; dRow++) {
			for (int dCol = -vr; dCol <= vr; dCol++) {
				int d = dRow*dRow + dCol*dCol;
				if (d <= this.viewRadius2) {
					this.visionOffsets2.add(new int[] {dRow % getRows() - getRows(), dCol % getCols() - getCols()});
				}
			}
		}

        int ar = (int)Math.floor(Math.sqrt(this.viewRadius2));
		this.attackOffsets2 = new ArrayList<int[]>();
		for (int dRow = -ar; dRow <= ar; dRow++) {
			for (int dCol = -ar; dCol <= ar; dCol++) {
				int d = dRow*dRow + dCol*dCol;
				if (d <= this.attackRadius2) {
					this.attackOffsets2.add(new int[] {dRow % getRows() - getRows(), dCol % getCols() - getCols()});
				}
			}
		}
	}

	/**
	 * Returns timeout for initializing and setting up the bot on turn 0.
	 * 
	 * @return timeout for initializing and setting up the bot on turn 0
	 */
	public int getLoadTime() {
		return loadTime;
	}

	/**
	 * Returns timeout for a single game turn, starting with turn 1.
	 * 
	 * @return timeout for a single game turn, starting with turn 1
	 */
	public int getTurnTime() {
		return turnTime;
	}

	/**
	 * Returns game map height.
	 * 
	 * @return game map height
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * Returns game map width.
	 * 
	 * @return game map width
	 */
	public int getCols() {
		return cols;
	}

	/**
	 * Returns maximum number of turns the game will be played.
	 * 
	 * @return maximum number of turns the game will be played
	 */
	public int getTurns() {
		return turns;
	}

	/**
	 * Returns squared view radius of each ant.
	 * 
	 * @return squared view radius of each ant
	 */
	public int getViewRadius2() {
		return viewRadius2;
	}

	/**
	 * Returns squared attack radius of each ant.
	 * 
	 * @return squared attack radius of each ant
	 */
	public int getAttackRadius2() {
		return attackRadius2;
	}

	/**
	 * Returns squared spawn radius of each ant.
	 * 
	 * @return squared spawn radius of each ant
	 */
	public int getSpawnRadius2() {
		return spawnRadius2;
	}

	/**
	 * Sets turn start time.
	 * 
	 * @param turnStartTime turn start time
	 */
	public void setTurnStartTime(long turnStartTime) {
		this.turnStartTime = turnStartTime;
	}

	/**
	 * Returns how much time the bot has still has to take its turn before timing out.
	 * 
	 * @return how much time the bot has still has to take its turn before timing out
	 */
	public int getTimeRemaining() {
		return turnTime - (int)(System.currentTimeMillis() - turnStartTime);
	}

	/**
	 * Returns ilk at the specified location.
	 * 
	 * @param tile location on the game map
	 * 
	 * @return ilk at the <cod>tile</code>
	 */
	public Ilk getIlk(Tile tile) {
		return map[tile.getRow()][tile.getCol()];
	}

	/**
	 * Sets ilk at the specified location.
	 * 
	 * @param tile location on the game map
	 * @param ilk ilk to be set at <code>tile</code>
	 */
	public void setIlk(Tile tile, Ilk ilk) {
		map[tile.getRow()][tile.getCol()] = ilk;
	}

	/**
	 * Returns ilk at the location in the specified direction from the specified location.
	 * 
	 * @param tile location on the game map
	 * @param direction direction to look up
	 * 
	 * @return ilk at the location in <code>direction</code> from <cod>tile</code>
	 */
	public Ilk getIlk(Tile tile, Aim direction) {
		Tile newTile = getTile(tile, direction);
		return map[newTile.getRow()][newTile.getCol()];
	}

	/**
	 * Returns location in the specified direction from the specified location.
	 * 
	 * @param tile location on the game map
	 * @param direction direction to look up
	 * 
	 * @return location in <code>direction</code> from <cod>tile</code>
	 */
	public Tile getTile(Tile tile, Aim direction) {
		int row = (tile.getRow() + direction.getRowDelta()) % rows;
		if (row < 0) {
			row += rows;
		}
		int col = (tile.getCol() + direction.getColDelta()) % cols;
		if (col < 0) {
			col += cols;
		}
		return new Tile(row, col);
	}

	/**
	 * Returns a set containing all my ants locations.
	 * 
	 * @return a set containing all my ants locations
	 */
	public Set<Tile> getMyAnts() {
		return myAnts;
	}

	/**
	 * Returns a set containing all enemy ants locations.
	 * 
	 * @return a set containing all enemy ants locations
	 */
	public Set<Tile> getEnemyAnts() {
		return enemyAnts;
	}

	/**
	 * Returns a set containing all my hills locations.
	 * 
	 * @return a set containing all my hills locations
	 */
	public Set<Tile> getMyHills() {
		return myHills;
	}

	/**
	 * Returns a set containing all enemy hills locations.
	 * 
	 * @return a set containing all enemy hills locations
	 */
	public Set<Tile> getEnemyHills() {
		return enemyHills;
	}

	/**
	 * Returns a set containing all food locations.
	 * 
	 * @return a set containing all food locations
	 */
	public Set<Tile> getFoodTiles() {
		return foodTiles;
	}

	/**
	 * Returns all orders sent so far.
	 * 
	 * @return all orders sent so far
	 */
	public Set<Order> getOrders() {
		return orders;
	}

	/**
	 * Calculates distance between two locations on the game map.
	 * 
	 * @param t1 one location on the game map
	 * @param t2 another location on the game map
	 * 
	 * @return distance between <code>t1</code> and <code>t2</code>
	 */
	public int getDistance(Tile t1, Tile t2) {
		int rowDelta = Math.abs(t1.getRow() - t2.getRow());
		int colDelta = Math.abs(t1.getCol() - t2.getCol());
		rowDelta = Math.min(rowDelta, rows - rowDelta);
		colDelta = Math.min(colDelta, cols - colDelta);
		return rowDelta * rowDelta + colDelta * colDelta;
	}

	/**
	 * Returns one or two orthogonal directions from one location to the another.
	 * 
	 * @param t1 one location on the game map
	 * @param t2 another location on the game map
	 * 
	 * @return orthogonal directions from <code>t1</code> to <code>t2</code>
	 */
	public List<Aim> getDirections(Tile t1, Tile t2) {
		List<Aim> directions = new ArrayList<Aim>();
		if (t1.getRow() < t2.getRow()) {
			if (t2.getRow() - t1.getRow() >= rows / 2) {
				directions.add(Aim.NORTH);
			} else {
				directions.add(Aim.SOUTH);
			}
		} else if (t1.getRow() > t2.getRow()) {
			if (t1.getRow() - t2.getRow() >= rows / 2) {
				directions.add(Aim.SOUTH);
			} else {
				directions.add(Aim.NORTH);
			}
		}
		if (t1.getCol() < t2.getCol()) {
			if (t2.getCol() - t1.getCol() >= cols / 2) {
				directions.add(Aim.WEST);
			} else {
				directions.add(Aim.EAST);
			}
		} else if (t1.getCol() > t2.getCol()) {
			if (t1.getCol() - t2.getCol() >= cols / 2) {
				directions.add(Aim.EAST);
			} else {
				directions.add(Aim.WEST);
			}
		}
		return directions;
	}

	/**
	 * Clears game state information about my ants locations.
	 */
	public void clearMyAnts() {
		myAnts.clear();
	}

	/**
	 * Clears game state information about enemy ants locations.
	 */
	public void clearEnemyAnts() {
		enemyAnts.clear();
	}

	/**
	 * Clears game state information about my hills locations.
	 */
	public void clearMyHills() {
		myHills.clear();
	}

	/**
	 * Clears game state information about enemy hills locations.
	 */
	public void clearEnemyHills() {
		enemyHills.clear();
	}


	/**
	 * Clears game map state information
	 */
	public void clearMap() {
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (map[row][col] != Ilk.WATER) {
					map[row][col] = Ilk.LAND;
				}
			}
		}
		this.visionMatrix = null;
	}
	public boolean isVisible(Tile location) {
		if (this.visionMatrix == null) {

			this.visionMatrix = new boolean[getRows()][getCols()];
			for (int r = 0; r < getRows(); r++) {
				for (int c = 0; c < getCols(); c++) {
					this.visionMatrix[r][c] = false;
				}
			}

			for (Tile ant : getMyAnts()) {
				for (int i = 0; i < this.visionOffsets2.size(); i++) {
					//Logger.getAnonymousLogger().warning("vision offsets at " +ant+ ": " + visionOffsets2.get(i)[0] + ", " + visionOffsets2.get(i)[1]);
					int vRow = (this.visionOffsets2.get(i)[0] + ant.getRow()) % rows;
					if (vRow < 0) {
						vRow += rows;
					}
					int vCol = (this.visionOffsets2.get(i)[1] + ant.getCol()) % cols;
					if (vCol < 0) {
						vCol += cols;
					}

					this.visionMatrix[vRow][vCol] = true;
				}
			}
		}
		return this.visionMatrix[location.getRow()][location.getCol()];
	}

	/**
	 * Updates game state information about new ants and food locations.
	 * 
	 * @param ilk ilk to be updated
	 * @param tile location on the game map to be updated
	 */
	public void update(Ilk ilk, Tile tile) {
		map[tile.getRow()][tile.getCol()] = ilk;
		switch (ilk) {
			case FOOD:
				foodTiles.add(tile);
				break;
			case MY_ANT:
				myAnts.add(tile);
				break;
			case ENEMY_ANT:
				enemyAnts.add(tile);
				break;
		}
	}

	/**
	 * Updates game state information about hills locations.
	 *
	 * @param owner owner of hill
	 * @param tile location on the game map to be updated
	 */
	public void updateHills(int owner, Tile tile) {
		if (owner > 0)
			enemyHills.add(tile);
		else
			myHills.add(tile);
	}

	/**
	 * Issues an order by sending it to the system output.
	 * 
	 * @param myAnt map tile with my ant
	 * @param direction direction in which to move my ant
	 */
	public void issueOrder(Tile myAnt, Aim direction) {
		Order order = new Order(myAnt, direction);
		orders.add(order);
		System.out.println(order);
	}

    public ArrayList<int[]> getVisionOffsets() {
        return visionOffsets2;
    }

    public ArrayList<int[]> getAttackOffsets() {
        return attackOffsets2;
    }
}
