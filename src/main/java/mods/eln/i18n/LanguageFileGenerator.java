package mods.eln.i18n;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

class LanguageFileGenerator {
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
            for (final String text2Translate : strings.get(sourceFile)) {
                writer.append(text2Translate).append("=\n");
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
            for (final String text2Translate : strings.get(sourceFile)) {
                writer.append(text2Translate).append("=");

                final String existingTranslation = existingTranslations.remove(text2Translate);
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
