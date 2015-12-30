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

	public static String tr(String key) {
		return tr(key, getCurrentLanguage());
	}

	public static String tr(String key, String lang) {
		String translation = l.getStringLocalization(key, lang);
		if (translation != null && !"".equals(translation)) {
			return translation;
		} else {
			return key;
		}
	}
}
