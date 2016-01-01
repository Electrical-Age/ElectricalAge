package mods.eln.misc;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internationalization and localization helper class.
 */
public class I18N {
    private final static LanguageRegistry languageRegistry = LanguageRegistry.instance();

    public static String getCurrentLanguage() {
        return FMLCommonHandler.instance().getCurrentLanguage();
    }

    /**
     * Translates the given string. You can actually pass arguments to the method and reference them in the string using
     * the placeholders %1 up to %9. For simplicity only up to 9 placeholders are allowed. Newlines are not supported
     * at all, so if you have to translate multiline strings, use tr() for each line. The lines will follow one after
     * another in the language anyway.
     * <p/>
     * Example: tr("You have %1 lives left", 4);
     * <p/>
     * IT IS IMPORTANT THAT YOU PASS THE STRING LITERALS AT LEAST ONCE AS THE FIRST PARAMETER TO THIS METHOD!!
     * Otherwise the translation will not be added to the language files automatically. There is no problem to use the
     * tr() method afterwards using an already registered string in the code using a string variable again.
     * <p/>
     * TODO: There is no escaping of the strings %1 to %9 possible.
     *
     * @param text    Text to translate
     * @param objects 0 up to 9 arguments to integrate into the text.
     * @return Translated text or original text if no translation is present.
     */
    public static String tr(final String text, Object... objects) {
        // Assert correct number of objects and no newline in text.
        assert (objects.length <= 9);
        assert (!text.contains("\n"));

        // Try to find the translation for the string using forge API.
        String translation = languageRegistry.getStringLocalization(text);

        // If no translation was found, just use the original text.
        if (translation == null || "".equals(translation)) {
            translation = text;

            // TODO: Maybe we could save the information about the missing translation or make a HTTP post...
        }

        // Replace placeholders in string by actual string values of the passed objects.
        for (int i = 0; i < objects.length; ++i) {
            translation = translation.replace("%" + (i + 1), String.valueOf(objects[i]));
        }

        return translation;
    }

    private static class SourceCodeParser {
        private static final Pattern JAVA_PATTERN = Pattern.compile("tr\\s*\\(\\s*\"(.*?)\"\\s*[,)]");
        private static final String MULTIPLE_LOCATIONS = "Appearing in multiple source files";

        public static Map<String, Set<String>> parseSourceFolder(final File file) throws IOException {
            Map<String, Set<String>> strings = new TreeMap<String,Set<String>>();
            strings.put(MULTIPLE_LOCATIONS, new TreeSet<String>());
            parseSourceFolderRecursive(file, strings);
            return strings;
        }

        private static void parseSourceFolderRecursive(final File folder, final Map<String, Set<String>> strings)
            throws IOException {
            // Check that arguments are valid.
            if (folder != null && folder.exists() && strings != null) {

                // Do for each file.
                for (final File file : folder.listFiles()) {

                    // If the file is a directory, call the method recursively.
                    if (file.isDirectory()) {
                        parseSourceFolderRecursive(file, strings);

                        // If it is a file and has the file extension .java, parse the Java file.
                    } else if (file.isFile() && file.getName().endsWith(".java")) {
                        System.out.println("Parsing Java source file: " + file.getName() + "...");
                        parseJavaFile(file, strings);

                        // If it is a file and has the file extension .kt, parse the Kotlin file.
                    } else if (file.isFile() && file.getName().endsWith(".kt")) {
                        System.out.println("Parsing Kotlin source file: " + file.getName() + "...");
                        parseKotlinFile(file, strings);
                    }
                }
            }
        }

        private static void parseJavaFile(final File file, final Map<String, Set<String>> strings) throws IOException {
            // Load file into memory.
            final String content = new Scanner(file).useDelimiter("\\Z").next();

            // Find all matches for Java style translations.
            final Set<String> translations = new TreeSet<String>();
            final Matcher matcher = JAVA_PATTERN.matcher(content);
            while (matcher.find()) {
                String translation = matcher.group(1);
                System.out.println("  " + translation);

                if (!isStringAlreadyPresent(translation, strings)) {
                    translations.add(translation);
                }
            }

            // If there were translations for that file, add the list of translations to the map.
            if (!translations.isEmpty()) {
                strings.put(file.getPath(), translations);
            }
        }

        private static void parseKotlinFile(final File file, final Map<String, Set<String>> strings) {
            throw new UnsupportedOperationException();
        }

        private static boolean isStringAlreadyPresent(final String string,
                                                      final Map<String, Set<String>> strings) {
            for (final String fileName: strings.keySet()) {
                if (MULTIPLE_LOCATIONS.equals(fileName)) {
                    if (strings.get(fileName).contains(string))
                        return true;
                } else {
                    if (strings.get(fileName).remove(string)) {
                        strings.get(MULTIPLE_LOCATIONS).add(string);
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static class LanguageFileGenerator {
        private static final String FILE_HEADER = "#<ELN_LANGFILE_V1_1>\n";

        public static void generateNewFile(final File file, final Map<String, Set<String>> strings)
            throws IOException {
            final FileWriter writer = new FileWriter(file);

            // Write header.
            writer.append(FILE_HEADER);

            // For each source file with translations create the file comment.
            for (final String sourceFile : strings.keySet()) {
                writer.append("\n# ").append(sourceFile).append("\n");

                // For each translated string in that source file, add translation text.
                for (final String translation : strings.get(sourceFile)) {
                    writer.append(translation).append("=\n");
                }
            }

            // Close writer.
            writer.close();
        }

        public static void updateFile(final File file, final Map<String, Set<String>> strings,
                                      final Map<String, String> existingTranslations) throws IOException {
            final FileWriter writer = new FileWriter(file);

            // Write header.
            writer.append(FILE_HEADER);

            // For each source file with translations create the file comment.
            for (final String sourceFile : strings.keySet()) {
                writer.append("\n# ").append(sourceFile).append("\n");

                // For each translated string in that source file, add translation text.
                for (final String translation : strings.get(sourceFile)) {
                    writer.append(translation).append("=");

                    final String existingTranslation = existingTranslations.remove(translation);
                    if (existingTranslation != null) {
                        writer.append(existingTranslation);
                    }

                    writer.append("\n");
                }
            }

            // Add translations from file that were not found in code.
            if (!existingTranslations.isEmpty()) {
                writer.append("\n# Existing translations in language file not found in code\n");

                for (Map.Entry<String,String> entry: existingTranslations.entrySet()) {
                    writer.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                }
            }

            // Close writer.
            writer.close();
        }
    }

    private static class LanguageFileParser {
        private static final Pattern TRANSLATION_PATTERN = Pattern.compile("^([^=]+)=([^=]+)$");

        public static Map<String, String> parseFile(final File file) throws IOException {
            final Map<String, String> translations = new TreeMap<String, String>();

            // TODO: Check header.

            final BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) != '#') {
                    final Matcher matcher = TRANSLATION_PATTERN.matcher(line);
                    if (matcher.find()) {
                        translations.put(matcher.group(1), matcher.group(2));
                    }
                }
            }

            return translations;
        }
    }

    private static void usage() {
        System.out.println("Usage: java -jar <EA jar file> mods.eln.misc.i18N <EA src folder> <lang file>");
        System.exit(1);
    }

    public static void main(String... args) {
        try {
            final File languageFile = new File(args[1]);
            final File srcFolder = new File(args[0]);

            // Check if the source folder is present.
            if (!srcFolder.isDirectory())
                usage();

            // If the language file already exists, parse the existing translations from the language file.
            Map<String, String> existingTranslations = null;
            if (languageFile.isFile()) {
                existingTranslations = LanguageFileParser.parseFile(languageFile);
            }

            // Get the string to translate from the source code.
            Map<String, Set<String>> stringsToTranslate = SourceCodeParser.parseSourceFolder(srcFolder);

            // If there was no file present, just generate the language file, otherwise update the existing file.
            if (existingTranslations == null) {
                LanguageFileGenerator.generateNewFile(languageFile, stringsToTranslate);
            } else {
                LanguageFileGenerator.updateFile(languageFile, stringsToTranslate, existingTranslations);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
