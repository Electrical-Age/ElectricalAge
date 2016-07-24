package mods.eln.misc;

import static mods.eln.i18n.I18N.tr;

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
	public final static int MINOR = 12;

	/** Revision version code. */
	public final static int REVISION = 0;

	/**
	 * Unique version code. Must be a String for annotations. Used to check if a
	 * new version if available. Each update must increment this number.
	 */
	public final static int UNIQUE_VERSION = 1000000 * MAJOR + 1000 * MINOR + REVISION;

	public final static String VERSION_STRING = "" + MAJOR + "." + MINOR + "." + REVISION;

	public static String getVersionName() {
		return VERSION_STRING;
	}

	public static String print() {
		return tr("mod.name") + " " + getVersionName();
	}

	public final static String printColor() {
		return Color.WHITE + tr("mod.name") + " version "
				+ Color.ORANGE + getVersionName();
	}
}
