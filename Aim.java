import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a direction in which to move an ant.
 */
public enum Aim {
    /** North direction, or up. */
    NORTH(-1, 0, 'n'),
    
    /** East direction or right. */
    EAST(0, 1, 'e'),
    
    /** South direction or down. */
    SOUTH(1, 0, 's'),
    
    /** West direction or left. */
    WEST(0, -1, 'w');
    
    private static final Map<Character, Aim> symbolLookup = new HashMap<Character, Aim>();
	private static final Map<Aim, Aim> rightLookup = new EnumMap<Aim, Aim>(Aim.class);
	private static final Map<Aim, Aim> leftLookup = new EnumMap<Aim, Aim>(Aim.class);
	private static final Map<Aim, Aim> behindLookup = new EnumMap<Aim, Aim>(Aim.class);
    
    static {
        symbolLookup.put('n', NORTH);
        symbolLookup.put('e', EAST);
        symbolLookup.put('s', SOUTH);
        symbolLookup.put('w', WEST);
		rightLookup.put(NORTH, EAST);
		rightLookup.put(EAST, SOUTH);
		rightLookup.put(SOUTH, WEST);
		rightLookup.put(WEST, NORTH);
		leftLookup.put(NORTH, WEST);
		leftLookup.put(WEST, SOUTH);
		leftLookup.put(SOUTH, EAST);
		leftLookup.put(EAST, NORTH);
		behindLookup.put(NORTH, SOUTH);
		behindLookup.put(SOUTH, NORTH);
		behindLookup.put(EAST, WEST);

    }
    
    private final int rowDelta;
    
    private final int colDelta;
    
    private final char symbol;
    
    Aim(int rowDelta, int colDelta, char symbol) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
        this.symbol = symbol;
    }
    
    /**
     * Returns rows delta.
     * 
     * @return rows delta.
     */
    public int getRowDelta() {
        return rowDelta;
    }
    
    /**
     * Returns columns delta.
     * 
     * @return columns delta.
     */
    public int getColDelta() {
        return colDelta;
    }
    
    /**
     * Returns symbol associated with this direction.
     * 
     * @return symbol associated with this direction.
     */
    public char getSymbol() {
        return symbol;
    }
    
    /**
     * Returns direction associated with specified symbol.
     * 
     * @param symbol <code>n</code>, <code>e</code>, <code>s</code> or <code>w</code> character
     * 
     * @return direction associated with specified symbol
     */
    public static Aim fromSymbol(char symbol) {
        return symbolLookup.get(symbol);
    }
public Aim left() {
		return leftLookup.get(this);
	}

	public Aim right() {
		return rightLookup.get(this);
	}

	public Aim behind() {
		return behindLookup.get(this);
	}
	

}
