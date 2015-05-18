package mods.eln.misc;

/**
 * Utility class to print message with colors. 16 colors are available.
 */
public class Color {

	// Only some short color aliases
	public static final String ORANGE = new Color(6).toString();
	public static final String GREEN = new Color(10).toString();
	public static final String RED = new Color(12).toString();
	public static final String WHITE = new Color(15).toString();

	// All color aliases
	public static final String COLOR_BLACK = new Color(0).toString();
	public static final String COLOR_DARK_BLUE = new Color(1).toString();
	public static final String COLOR_DARK_GREEN = new Color(2).toString();
	public static final String COLOR_DARK_CYAN = new Color(3).toString();
	public static final String COLOR_DARK_RED = new Color(4).toString();
	public static final String COLOR_DARK_MAGENTA = new Color(5).toString();
	public static final String COLOR_DARK_YELLOW = new Color(6).toString();
	public static final String COLOR_BRIGHT_GREY = new Color(7).toString();
	public static final String COLOR_DARK_GREY = new Color(8).toString();
	public static final String COLOR_BRIGHT_BLUE = new Color(9).toString();
	public static final String COLOR_BRIGHT_GREEN = new Color(10).toString();
	public static final String COLOR_BRIGHT_CYAN = new Color(11).toString();
	public static final String COLOR_BRIGHT_RED = new Color(12).toString();
	public static final String COLOR_BRIGHT_MAGENTA = new Color(13).toString();
	public static final String COLOR_BRIGHT_YELLOW = new Color(14).toString();
	public static final String COLOR_WHITE = new Color(15).toString();

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
