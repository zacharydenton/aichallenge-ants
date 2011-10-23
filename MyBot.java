import java.io.IOException;
import java.util.*;

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

	public boolean moveDirection(Ants ants, Set<Tile> destinations, Tile location, Aim direction) {
		Tile destination = ants.getTile(location, direction);
		if (ants.getIlk(destination).isUnoccupied() && !destinations.contains(destination)) {
			ants.issueOrder(location, direction);
			destinations.add(destination);
			return true;
		} else {
			return false;
		}
	}

	public void assignTargets(Ants ants, Set<Tile> destinations, Set<Tile> goals, Set<Tile> active, Set<Tile> targets) {
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
				List<Aim> directions = ants.getDirections(closestAnt, target);
				Collections.shuffle(directions);
				for (Aim direction : directions) {
					if (moveDirection(ants, destinations, closestAnt, direction)) {
						goals.add(target);
						active.add(closestAnt);
					}
				}
			}
		}
		for (Tile ant : ants.getMyAnts()) {
			if (!active.contains(ant)) {
				destinations.add(ant);
			}
		}
	}

	public Set<Tile> getExplorationTargets(Ants ants, int numTargets) {
		HashSet<Tile> targets = new HashSet<Tile>();
		for (int i = 0; i < numTargets; i++) {
			int row = (int)(Math.random() * (ants.getRows() + 1));
			int col = (int)(Math.random() * (ants.getCols() + 1));
			targets.add(new Tile(row, col));
		}
		return targets;
	}

	public void gatherFood(Ants ants, Set<Tile> destinations, Set<Tile> goals, Set<Tile> active) {
		assignTargets(ants, destinations, goals, active, ants.getFoodTiles());
	}

	public void exploreMap(Ants ants, Set<Tile> destinations, Set<Tile> goals, Set<Tile> active) {
		assignTargets(ants, destinations, goals, active, 
				getExplorationTargets(ants, ants.getMyAnts().size() - active.size()));
	}

	public void doTurn() {
		Ants ants = getAnts();
		Set<Tile> destinations = new HashSet<Tile>();
		Set<Tile> goals = new HashSet<Tile>();
		Set<Tile> active = new HashSet<Tile>();

		for (Tile ant : ants.getMyAnts()) {
			if (goals.contains(ant)) {
				goals.remove(ant);
			}
		}

		gatherFood(ants, destinations, goals, active);
		exploreMap(ants, destinations, goals, active);
	}
}
