package mods.eln.i18n;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * Internationalization and localization helper class.
 */
public class I18N {
    private final static LanguageRegistry languageRegistry = LanguageRegistry.instance();

    public static String getCurrentLanguage() {
        return FMLCommonHandler.instance().getCurrentLanguage();
    }

    /**
     * Translates the given string. You can pass arguments to the method and reference them in the string using
     * the placeholders %N$ whereas N is the index of the actual parameter <b>starting at 1</b>. Newlines are not
     * supported at all, so if you have to translate multiline strings, you have to use tr() for each line separately.
     * <p/>
     * Example: tr("You have %1$ lives left", 4);
     * <p/>
     * IT IS IMPORTANT THAT YOU PASS THE <b>STRING LITERALS</b> AT LEAST ONCE AS THE FIRST PARAMETER TO THIS METHOD or
     * you call the method TR() with the actual string literal in order to register the translation text automatically!
     * Otherwise the translation will not be added to the language files. There is no problem to use the tr() method
     * afterwards using an already registered string in the code using a string variable as the first parameter.
     * <p/>
     *
     * @param text      Text to translate
     * @param objects   Arguments to integrate into the text.
     * @return          Translated text or original text (Argument placeholders are replaced by the actual arguments
     *                  anyway) if no translation is present.
     */
    public static String tr(final String text, Object... objects) {
        // We can not allow newlines in translated strings.
        assert (!text.contains("\n"));

        // Try to find the translation for the string using forge API.
        String translation = languageRegistry.getStringLocalization(text);

        // If no translation was found, just use the original text.
        if (translation == null || "".equals(translation)) {
            translation = text;
        }

        // Replace placeholders in string by actual string values of the passed objects.
        for (int i = 0; i < objects.length; ++i) {
            translation = translation.replace("%" + (i + 1) + "$", String.valueOf(objects[i]));
        }

        return translation;
    }

    /**
     * This method can be used to mark an unlocalized string in order to add it to the generated language files.
     * The method does not actually translate the string - it marks the string literal only to be translated afterwards.
     * A common use case is to add text to the language file which is translated using a string variable with the
     * method tr().
     *
     * @param string    String LITERAL to add to the language files.
     * @return          Exactly the same string as given to the method.
     */
    public static String TR(final String string) {
        return string;
    }

    /**
     * Defines the different translatable types.
     */
    public enum Type {
        /**
         * The text to translate is not related to a particular translatable type, so basically only the ".name" suffix
         * is added to the translation key.
         */
        NONE(""),

        /**
         * The text to translate is related to an item. The "item." prefix will be added to the translation key.
         */
        ITEM("item."),

        /**
         * The text to translate is related to a tile. The "tile." prefix will be added to the translation key.
         */
        TILE("tile."),

        /**
         * The text to translate is related to an achievement. The "achievement." prefix will be added to the
         * translation key.
         */
        ACHIEVEMENT("achievement."),

        /**
         * The text to translate is related to an entity. The "entity." prefix will be added to the translation key.
         */
        ENTITY("entity."),

        /**
         * The text to translate is related to a death attack. The "death.attack" prefix will be added to the
         * translation key.
         */
        DEATH_ATTACK("death.attack."),

        /**
         * The text to translate is related to an item group. The "itemGroup." prefix will be added to the translation
         * key.
         */
        ITEM_GROUP("itemGroup."),

        /**
         * The text to translate is related to a container. The "container." prefix will be added to the translation
         * key.
         */
        CONTAINER("container."),

        /**
         * The text to translate is related to an block. The "block." prefix will be added to the translation key.
         */
        BLOCK("block.");

        private final String prefix;

        Type(final String prefix) {
            this.prefix = prefix;
        }

        /**
         * Returns the prefix to use in the language file.
         *
         * @return  Prefix for the type of translatable text.
         */
        public String getPrefix() {
            return prefix;
        }
    };

    /**
     * Used to register a name to translate. The forge mechanisms are used in order to translate the name.
     *
     * @param type      Type the translatable name is related to.
     * @param string    String LITERAL to register for translation.
     * @return          Returns the same string literal, forge will translate the name magically.
     */
    public static String TR_NAME(final Type type, final String string) {
        return string;
    }

    /**
     * Used to register a description to translate. The forge mechanisms are used in order to translate the description.
     *
     * @param type      Type the translatable description is related to.
     * @param string    String LITERAL to register for translation.
     * @return          Returns the same string literal, forge will translate the description magically.
     */
    public static String TR_DESC(final Type type, final String string) {
        return string;
    }
}
