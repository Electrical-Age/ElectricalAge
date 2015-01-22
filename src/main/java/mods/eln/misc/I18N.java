package mods.eln.misc;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * Internationalization and localization helper class.
 */
public class I18N {
	private final static LanguageRegistry l = LanguageRegistry.instance();

	public static String getCurrentLanguage() {
		return FMLCommonHandler.instance().getCurrentLanguage();
	}

	public static String getString(String key) {
		return l.getStringLocalization(key);
	}

	public static String getString(String key, String lang) {
		return l.getStringLocalization(key, lang);
	}
}
