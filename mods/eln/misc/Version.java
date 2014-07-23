package mods.eln.misc;



/**
 * Current mod version. Used to check if a new mod version is available. Must be
 * set correctly for each mod release.
 * 
 * @author metc
 */
public final class Version {

	/** Major version code. */
	public final static int MAJOR = 1;

	/** Minor version code. */
	public final static int MINOR = 7;

	/**
	 * Unique version code. Must be a String for annotations. Used to check if a
	 * new version if available. Each update must increment this number.
	 */
	public final static String REVISION = "24";

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
