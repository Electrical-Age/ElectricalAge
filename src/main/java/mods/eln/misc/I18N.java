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

	/*
	TODO: Only argument placeholders from %1 to %9 (9 arguments) are supported.
	TODO: There is no escaping of the strings %1 and so on possible.
	 */
	public static String tr(String text, Object... objects) {
		String translation = l.getStringLocalization(text, getCurrentLanguage());
		if (translation == null || "".equals(translation)) {
			translation = text;
		}

		for (int i = 0; i < objects.length; ++i) {
			String stringRepresentation = objects[i] != null ? objects[i].toString() : "null";
			translation = translation.replace("%" + (i + 1), stringRepresentation);
		}

		return translation;
	}

	public static String translateToLanguage(String text, String language) {
		String translation = l.getStringLocalization(text, language);
		if (translation != null && !"".equals(translation)) {
			return translation;
		} else {
			return text;
		}
	}
}
