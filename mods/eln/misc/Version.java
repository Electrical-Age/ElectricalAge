package mods.eln.misc;

/**
 * Mod version.
 */
public final class Version {

	/** Major version code. */
	public final static int MAJOR = 1;

	/** Minor version code. */
	public final static int MINOR = 5;

	/** Unique version code. */
	public final static String REVISION = "10";

	
	public final static String getVersionName() {
		return String.format("BETA-%d.%d r%s", MAJOR, MINOR, REVISION);
	}

	public final static String print() {
		return I18N.getString("mod.name") + " " + getVersionName();
	}

	public final static String printColor() {
		return Color.WHITE + I18N.getString("mod.name") + " version "
				+ Color.ORANGE + getVersionName();
	}
}
