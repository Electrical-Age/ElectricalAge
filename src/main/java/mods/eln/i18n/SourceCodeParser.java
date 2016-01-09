package mods.eln.i18n;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SourceCodeParser {
    private static final Pattern JAVA_TR_PATTERN = Pattern.compile("(?:tr|TR)\\s*\\(\\s*\"(.*?)\"\\s*[,)]");
    private static final Pattern JAVA_FORGE_PATTERN =
        Pattern.compile("TR_([A-Z]*)\\s*\\(\\s*(?:I18N.)?Type.(.*?)\\s*,\\s\"(.*?)\"\\s*\\)");
    private static final String MULTIPLE_LOCATIONS = "Appearing in multiple source files";

    public static Map<String, Set<TranslationItem>> parseSourceFolder(final File file) throws IOException {
        Map<String, Set<TranslationItem>> strings = new TreeMap<String,Set<TranslationItem>>();
        strings.put(MULTIPLE_LOCATIONS, new TreeSet<TranslationItem>());
        parseSourceFolderRecursive(file, strings);
        return strings;
    }

    private static void parseSourceFolderRecursive(final File folder, final Map<String, Set<TranslationItem>> strings)
        throws IOException {
        // Check that arguments are valid.
        if (folder != null && folder.exists()) {

            // Do for each file.
            for (final File file: folder.listFiles()) {

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

    private static void parseJavaFile(final File file, final Map<String, Set<TranslationItem>> strings)
        throws IOException {
        // Load file into memory.
        final String content = new Scanner(file).useDelimiter("\\Z").next();

        final Set<TranslationItem> textsToTranslate = new TreeSet<TranslationItem>();

        // Find all matches for Java style translations.
        final Matcher trMatcher = JAVA_TR_PATTERN.matcher(content);
        while (trMatcher.find()) {
            final TranslationItem textToTranslate = new TranslationItem(trMatcher.group(1));
            System.out.println("  " + textToTranslate.getKey());

            if (!isStringAlreadyPresent(textToTranslate, strings)) {
                textsToTranslate.add(textToTranslate);
            }
        }

        final Matcher forgeMatcher = JAVA_FORGE_PATTERN.matcher(content);
        while (forgeMatcher.find()) {
            final String property = forgeMatcher.group(1).toLowerCase();
            final I18N.Type type = I18N.Type.valueOf(forgeMatcher.group(2));
            final String text = forgeMatcher.group(3);
            final TranslationItem textToTranslate = new TranslationItem(type.getPrefix() +
                I18N.encodeLangKey(text) + "." + property, text);

            System.out.println("  " + textToTranslate.getKey());

            if (!isStringAlreadyPresent(textToTranslate, strings)) {
                textsToTranslate.add(textToTranslate);
            }
        }

        // If there were translations for that file, add the list of translations to the map.
        if (!textsToTranslate.isEmpty()) {
            strings.put(file.getPath(), textsToTranslate);
        }
    }

    private static void parseKotlinFile(final File file, final Map<String, Set<TranslationItem>> strings) {
        throw new UnsupportedOperationException();
    }

    private static boolean isStringAlreadyPresent(final TranslationItem string,
                                                  final Map<String, Set<TranslationItem>> strings) {
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
