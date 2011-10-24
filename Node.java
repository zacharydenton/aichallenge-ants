import java.util.*;

class Node {
	public Ants ants;
	public Tile position;
	public Node parent;
	public Tile goal;

	public Node(Ants ants, Tile position, Node parent, Tile goal) {
		this.ants = ants;
		this.position = position;
		this.parent = parent;
		this.goal = goal;
	}

	public int length() {
		int i = 0;
		Node p = this.parent;
		while (p != null) {
			p = p.parent;
			i++;
		}
		return i;
	}
	public int value() {
		int g = this.length();    
		int h = ants.getDistance(position, goal);
		return g + h;
	}

	public LinkedList<Node> getPath() {
		LinkedList<Node> path = new LinkedList<Node>();
		Node p = this.parent;
		while (p != null) {
			path.addFirst(p);
			p = p.parent;
		}
		return path;
	}

	public Set<Tile> getNeighbors() {
		HashSet<Tile> neighbors = new HashSet<Tile>();
		for (Aim aim : new Aim[]{Aim.NORTH, Aim.SOUTH, Aim.EAST, Aim.WEST}) {
			Ilk ilk = ants.getIlk(this.position, aim);
			if (ilk.isPassable()) {
				neighbors.add(ants.getTile(this.position, aim));
			}
		}
		return neighbors;
	}

	public int hashCode() {
		return this.position.hashCode();
	}

}


