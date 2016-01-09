package mods.eln.i18n;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class LanguageFileGenerator {
    private static final String FILE_HEADER = "#<ELN_LANGFILE_V1_1>\n";

    public static void updateFile(final File file, final Map<String, Set<TranslationItem>> strings,
                                  final Properties existingTranslations) throws IOException {
        final FileWriter writer = new FileWriter(file);

        // Write header.
        writer.append(FILE_HEADER);

        // For each source file with translations create the file comment.
        for (final String sourceFile : strings.keySet()) {
            // Standardise file paths for every platforms
            final String sourcePath = sourceFile.replace("\\", "/");
            writer.append("\n# ").append(sourcePath).append("\n");

            // For each translated string in that source file, add translation text.
            for (final TranslationItem text2Translate : strings.get(sourceFile)) {
                text2Translate.applyExistingTranslationIfPresent(existingTranslations);
                writer.append(text2Translate.toString());
            }
        }

        // Close writer.
        writer.close();
    }
}
