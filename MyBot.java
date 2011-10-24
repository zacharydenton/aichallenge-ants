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

	private ArrayList<Tile> unseen;
	private ConcurrentHashMap<Tile, Node> goals;

	public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
			int attackRadius2, int spawnRadius2) {
		setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
					spawnRadius2));
		unseen = new ArrayList<Tile>();
		for (int r = 0; r < getAnts().getRows(); r++) {
			for (int c = 0; c < getAnts().getCols(); c++) {
				unseen.add(new Tile(r, c));
			}
		}

		goals = new ConcurrentHashMap<Tile, Node>();
	}

	public boolean moveDirection(Ants ants, Tile location, Aim direction) {
		Tile destination = ants.getTile(location, direction);
		if (ants.getIlk(destination).isPassable()) {
			ants.issueOrder(location, direction);
			return true;
		} else {
			return false;
		}
	}

	public boolean moveLocation(Ants ants, Set<Tile> destinations, Set<Tile> active, Tile ant, Tile destination) {
		if (ants.getIlk(destination).isPassable() && !destinations.contains(destination)) {
			List<Aim> directions = ants.pathDirections(ant, destination);
			Collections.shuffle(directions);
			for (Aim direction : directions) {
				if (moveDirection(ants, ant, direction)) {
					//Logger.getAnonymousLogger().warning("ant at " + ant + " moves " + direction + " to " + ants.getTile(ant, direction));
					active.add(ant);
					return true;
				}
			}
		}
		return false;
	}


	public void assignTargets(Ants ants, Set<Tile> destinations, Set<Tile> active, Set<Tile> targets) {
		for (Tile target : targets) {
			Tile closestAnt = null;
			int closestDistance = 999999;
			for (Tile ant : ants.getMyAnts()) {
				if (!active.contains(ant)) {
					int distance = ants.getDistance(ant, target);
					if (distance < closestDistance) {
						closestDistance = distance;
						closestAnt = ant;
					}
				}
			}
			if (closestAnt != null) {
				moveLocation(ants, destinations, active, closestAnt, target);				
			}
		}
		for (Tile ant : ants.getMyAnts()) {
			if (!active.contains(ant)) {
				destinations.add(ant);
			}
		}
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

	public Set<Tile> getExplorationTargets(Ants ants, int numTargets) {
		HashSet<Tile> targets = new HashSet<Tile>();
		for (int i = 0; i < numTargets; i++) {
			targets.add(unseen.get(i));
		}
		return targets;
	}

	public int numAllies(Ants ants, Tile ant, int radius) {
		int count = 0;
		for (Tile otherAnt : ants.getMyAnts()) {
			if (ant == otherAnt)
				continue;
			if (ants.getDistance(ant, otherAnt) <= radius) {
				//Logger.getAnonymousLogger().warning("ant at " + ant + " is allied with ant at " + otherAnt);
				count++;
			}
		}
		return count;
	}

	public void findFood(Ants ants, Set<Tile> active) {
		findSeekers(ants, active, ants.getFoodTiles());
	}

	public void exploreMap(final Ants ants, Set<Tile> destinations, Set<Tile> active) {
		// assignTargets(ants, destinations, goals, active, 
		// getExplorationTargets(ants, ants.getMyAnts().size() - active.size()));
		//

		for (final Tile ant : ants.getMyAnts()) {
			if (!active.contains(ant)) {
				ArrayList<Tile> closestUnseen = new ArrayList<Tile>(unseen);
				Collections.sort(closestUnseen, new Comparator<Tile>() {
					public int compare(Tile a, Tile b) {
						return ants.getDistance(ant, a) - ants.getDistance(ant, b);
					}
				});
				//Logger.getAnonymousLogger().warning("closest unseen: " + closestUnseen);
				for (Tile location : closestUnseen) {
					//Logger.getAnonymousLogger().warning("ant at " + ant + " wants to explore " + location + " (" + ants.getDistance(ant, location) + " units away)");
					if (moveLocation(ants, destinations, active, ant, location)) {
						return;
					}
				}
			}
		}
	}


	public void attackEnemyAnts(Ants ants, Set<Tile> destinations, Set<Tile> active) {
		assignTargets(ants, destinations, active, ants.getEnemyAnts());
	}

	public void attackEnemyHills(Ants ants, Set<Tile> destinations, Set<Tile> active) {
		//assignTargets(ants, destinations, goals, active, ants.getEnemyHills());
		//findSeekers(ants, destinations, active, ants.getEnemyHills());
	}

	public void findAllies(Ants ants, Set<Tile> destinations, Set<Tile> active) {
		Set<Tile> unallied = new HashSet<Tile>();
		for (Tile ant : ants.getMyAnts()) {
			if (numAllies(ants, ant, 1) == 0) {
				unallied.add(ant);
			}
		}
		//Logger.getAnonymousLogger().warning(unallied.toString());
		assignTargets(ants, destinations, active, unallied);
	}

	// things to remember between turns
	private Map<Tile, Aim> antStraight = new HashMap<Tile, Aim>();
	private Map<Tile, Aim> antLefty = new HashMap<Tile, Aim>();
	public void leftyMove(Ants ants, Set<Tile> destinations, Set<Tile> active) {
		Map<Tile, Aim> newStraight = new HashMap<Tile, Aim>();
		Map<Tile, Aim> newLefty = new HashMap<Tile, Aim>();
		for (Tile location : ants.getMyAnts()) {
			// send new ants in a straight line
			if (!antStraight.containsKey(location) && !antLefty.containsKey(location)) {
				Aim direction;
				if (location.getRow() % 2 == 0) {
					if (location.getCol() % 2 == 0) {
						direction = Aim.NORTH;
					} else {
						direction = Aim.SOUTH;
					}
				} else {
					if (location.getCol() % 2 == 0) {
						direction = Aim.EAST;
					} else {
						direction = Aim.WEST;
					}
				}
				antStraight.put(location, direction);
			}
			// send ants going in a straight line in the same direction
			if (antStraight.containsKey(location)) {
				Aim direction = antStraight.get(location);
				Tile destination = ants.getTile(location, direction);
				if (ants.getIlk(destination).isPassable()) {
					if (ants.getIlk(destination).isUnoccupied() && !destinations.contains(destination)) {
						//moveDirection(ants, destinations, location, direction);
						active.add(location);
						newStraight.put(destination, direction);
					} else {
						// pause ant, turn and try again next turn
						newStraight.put(location, direction.left());
						destinations.add(location);
						active.add(location);
					}
				} else {
					// hit a wall, start following it
					antLefty.put(location, direction.right());
				}
			}
			// send ants following a wall, keeping it on their left
			if (antLefty.containsKey(location)) {
				Aim direction = antLefty.get(location);
				List<Aim> directions = new ArrayList<Aim>();
				directions.add(direction.left());
				directions.add(direction);
				directions.add(direction.right());
				directions.add(direction.behind());
				// try 4 directions in order, attempting to turn left at corners
				for (Aim new_direction : directions) {
					try {
						Tile destination = ants.getTile(location, new_direction);
						if (ants.getIlk(destination).isPassable()) {
							if (ants.getIlk(destination).isUnoccupied() && !destinations.contains(destination)) {
								//moveDirection(ants, destinations, location, new_direction);
								active.add(location);
								newLefty.put(destination, new_direction);
								break;
							} else {
								// pause ant, turn and send straight
								newStraight.put(location, direction.right());
								destinations.add(location);
								active.add(location);
								break;
							}
						}

					} catch (NullPointerException e) {
						e.printStackTrace();
						Logger.getAnonymousLogger().warning("null pointer: " + new_direction + " : " + location);
					}
				}
			}
		}
		antStraight = newStraight;
		antLefty = newLefty;
	}

	public Node closestAnt(Ants ants, Tile target) {
		PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
			public int compare(Node a, Node b) {
				return a.length() - b.length();
			}
		});
		Node start = new Node(ants, target, null, null);
		frontierQueue.add(start);
		HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
		HashSet<Tile> frontierTiles = new HashSet<Tile>();
		frontierTiles.add(target);
		HashSet<Node> explored = new HashSet<Node>();
		HashSet<Tile> exploredTiles = new HashSet<Tile>();

		while (frontierQueue.size() > 0) {
			Node current = frontierQueue.remove();
			frontierSet.remove(current);
			frontierTiles.remove(current.position);
			explored.add(current);
			exploredTiles.add(current.position);

			if (ants.getMyAnts().contains(current.position)) {
				return current;
			}

			for (Tile neighborTile : current.getNeighbors()) {
				if (!frontierTiles.contains(neighborTile) && !exploredTiles.contains(neighborTile)) {
					Node neighbor = new Node(ants, neighborTile, current, null);
					frontierQueue.add(neighbor);
					frontierSet.add(neighbor);
					frontierTiles.add(neighborTile);
				}
			}

		}
		return null;
	}

	public void findSeekers(Ants ants, Set<Tile> active, Set<Tile> targets) {
		for (Tile target : targets) {
			if (!goals.containsKey(target)) { 
				Node path = closestAnt(ants, target);
				if (!active.contains(path.position)) {
					goals.put(target, path);
					active.add(path.position);
				}
			}
		}
	}

	public void moveToGoals(Ants ants, Set<Tile> active) {
		for (Map.Entry entry : goals.entrySet()) {
			Tile target = (Tile) entry.getKey();
			Node path = (Node) entry.getValue();
			if (path.parent == null) {
				goals.remove(target);
			} else {
				for (Aim direction : ants.getDirections(path.position, path.parent.position)) {
					moveDirection(ants, path.position, direction);
				}
				goals.put(target, path.parent);
			}
		}
	}

	public void doTurn() {
		Ants ants = getAnts();
		Set<Tile> destinations = new HashSet<Tile>();
		Set<Tile> active = new HashSet<Tile>();

		ArrayList<Tile> oldUnseen = new ArrayList<Tile>(unseen);
		for (Tile location : oldUnseen) {
			if (ants.isVisible(location)) {
				unseen.remove(location);
			}
		}


		/*
		attackEnemyHills(ants, destinations, goals, active);
		if (ants.getMyAnts().size() > 50 || ants.getFoodTiles().size() == 0) {
			//findAllies(ants, destinations, goals, active);
			attackEnemyAnts(ants, destinations, goals, active);
		} 
		gatherFood(ants, destinations, goals, active);
		leftyMove(ants, destinations, goals, active);
		*/
		findFood(ants, active);
		exploreMap(ants, active);

		moveToGoals(ants, active);
	}
}
