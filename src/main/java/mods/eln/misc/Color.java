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

	// All color indexes
	public static final int IDX_COLOR_BLACK = 0;
	public static final int IDX_COLOR_DARK_BLUE = 1;
	public static final int IDX_COLOR_DARK_GREEN = 2;
	public static final int IDX_COLOR_DARK_CYAN = 3;
	public static final int IDX_COLOR_DARK_RED = 4;
	public static final int IDX_COLOR_DARK_MAGENTA = 5;
	public static final int IDX_COLOR_DARK_YELLOW = 6;
	public static final int IDX_COLOR_BRIGHT_GREY = 7;
	public static final int IDX_COLOR_DARK_GREY = 8;
	public static final int IDX_COLOR_BRIGHT_BLUE = 9;
	public static final int IDX_COLOR_BRIGHT_GREEN = 10;
	public static final int IDX_COLOR_BRIGHT_CYAN = 11;
	public static final int IDX_COLOR_BRIGHT_RED = 12;
	public static final int IDX_COLOR_BRIGHT_MAGENTA = 13;
	public static final int IDX_COLOR_BRIGHT_YELLOW = 14;
	public static final int IDX_COLOR_WHITE = 15;

	// All color aliases
	public static final String COLOR_BLACK = new Color(IDX_COLOR_BLACK).toString();
	public static final String COLOR_DARK_BLUE = new Color(IDX_COLOR_DARK_BLUE).toString();
	public static final String COLOR_DARK_GREEN = new Color(IDX_COLOR_DARK_GREEN).toString();
	public static final String COLOR_DARK_CYAN = new Color(IDX_COLOR_DARK_CYAN).toString();
	public static final String COLOR_DARK_RED = new Color(IDX_COLOR_DARK_RED).toString();
	public static final String COLOR_DARK_MAGENTA = new Color(IDX_COLOR_DARK_MAGENTA).toString();
	public static final String COLOR_DARK_YELLOW = new Color(IDX_COLOR_DARK_YELLOW).toString();
	public static final String COLOR_BRIGHT_GREY = new Color(IDX_COLOR_BRIGHT_GREY).toString();
	public static final String COLOR_DARK_GREY = new Color(IDX_COLOR_DARK_GREY).toString();
	public static final String COLOR_BRIGHT_BLUE = new Color(IDX_COLOR_BRIGHT_BLUE).toString();
	public static final String COLOR_BRIGHT_GREEN = new Color(IDX_COLOR_BRIGHT_GREEN).toString();
	public static final String COLOR_BRIGHT_CYAN = new Color(IDX_COLOR_BRIGHT_CYAN).toString();
	public static final String COLOR_BRIGHT_RED = new Color(IDX_COLOR_BRIGHT_RED).toString();
	public static final String COLOR_BRIGHT_MAGENTA = new Color(IDX_COLOR_BRIGHT_MAGENTA).toString();
	public static final String COLOR_BRIGHT_YELLOW = new Color(IDX_COLOR_BRIGHT_YELLOW).toString();
	public static final String COLOR_WHITE = new Color(IDX_COLOR_WHITE).toString();

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
