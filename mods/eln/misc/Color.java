package mods.eln.misc;

/**
 * Utility class to print message with colors. 16 colors are available.
 */
public class Color {

	// Only some colors
	public static String BLACK = new Color(0).toString();
	public static String BLUE = new Color(1).toString();
	public static String ORANGE = new Color(6).toString();
	public static String RED = new Color(12).toString();
	public static String WHITE = new Color(15).toString();
	
	private String color;

	/**
	 * Create a custom color.
	 * 
	 * @param n
	 *            0 to 15 for all available colors
	 */
	public Color(int n) {
		if (n < 0)
			n = 0;
		else if (n > 15)
			n = 15;
		color = Integer.toHexString(n);
	}

	@Override
	public String toString() {
		return "\u00a7" + color;
	}
}
