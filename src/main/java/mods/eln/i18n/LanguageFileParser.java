package mods.eln.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LanguageFileParser {
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
