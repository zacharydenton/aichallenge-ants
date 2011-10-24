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
	private HashSet<Tile> reserved;

	public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
			int attackRadius2, int spawnRadius2) {
		setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
					spawnRadius2));
		unseen = new HashSet<Tile>();
		for (int r = 0; r < getAnts().getRows(); r++) {
			for (int c = 0; c < getAnts().getCols(); c++) {
				unseen.add(new Tile(r, c));
			}
		}

	}

	public boolean moveDirection(Ants ants, Tile location, Aim direction) {
		Tile destination = ants.getTile(location, direction);
		Logger.getAnonymousLogger().warning("here");
		if (ants.getIlk(destination).isPassable() && !reserved.contains(destination)) {
			ants.issueOrder(location, direction);
			reserved.remove(location);
			reserved.add(destination);
			return true;
		} else {

			return false;
		}
	}

	public boolean moveLocation(Ants ants, Tile ant, Tile destination) {
		if (ants.getIlk(destination).isPassable()) {
			List<Aim> directions = ants.getDirections(ant, destination);
			Collections.shuffle(directions);
			for (Aim direction : directions) {
				if (moveDirection(ants, ant, direction)) {
					return true;
				}
			}
		}
		return false;
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

	public int numAllies(Ants ants, Tile ant, int radius) {
		int count = 0;
		for (Tile otherAnt : ants.getMyAnts()) {
			if (ant == otherAnt)
				continue;
			if (ants.getDistance(ant, otherAnt) <= radius) {
				count++;
			}
		}
		return count;
	}

	public LinkedList<Node> closestTarget(Ants ants, Tile startPos, Set<Tile> targets) {
		PriorityQueue<Node> frontierQueue = new PriorityQueue<Node>(1, new Comparator<Node>() {
			public int compare(Node a, Node b) {
				return a.length() - b.length();
			}
		});
		Node start = new Node(ants, startPos, null, null);
		frontierQueue.add(start);
		HashSet<Node> frontierSet = new HashSet<Node>(frontierQueue);
		HashSet<Tile> frontierTiles = new HashSet<Tile>();
		frontierTiles.add(startPos);
		HashSet<Node> explored = new HashSet<Node>();
		HashSet<Tile> exploredTiles = new HashSet<Tile>();

		while (frontierQueue.size() > 0) {
			Node current = frontierQueue.remove();
			frontierSet.remove(current);
			frontierTiles.remove(current.position);
			explored.add(current);
			exploredTiles.add(current.position);

			if (targets.contains(current.position) || frontierQueue.size() > 100) {
				return current.getPath();
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


	public LinkedList<Node> closestUnexplored(Ants ants, Tile ant) {
		return closestTarget(ants, ant, unseen);
	}

	public LinkedList<Node> closestFood(Ants ants, Tile ant) {
		return closestTarget(ants, ant, ants.getFoodTiles());
	}

	public LinkedList<Node> closestAnt(Ants ants, Tile target) {
		return closestTarget(ants, target, ants.getMyAnts());
	}

	public void doTurn() {
		Ants ants = getAnts();
		reserved = new HashSet<Tile>();
		for (Tile ant : ants.getMyAnts()) {
			reserved.add(ant);
		}
		List<Aim> directions = new ArrayList<Aim>(EnumSet.allOf(Aim.class));
		for (Tile hill : ants.getMyHills()) {
			if (ants.getMyAnts().contains(hill)) {
				for (Aim direction : directions) {
					if (moveDirection(ants, hill, direction)) {
						break;
					}
				}
			}
		}

	}
}
