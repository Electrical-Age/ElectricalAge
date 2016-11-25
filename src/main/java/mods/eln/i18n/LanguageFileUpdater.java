package mods.eln.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class LanguageFileUpdater {
    private LanguageFileUpdater() {
    }

    private static void updateFile(final File languageFile, final Map<String, Set<TranslationItem>> stringsToTranslate)
        throws IOException {
        // Parse the existing translations from the language file.
        Properties existingTranslations = new Properties();
        existingTranslations.load(new FileInputStream(languageFile));

        // Update the existing language file.
        LanguageFileGenerator.updateFile(languageFile, stringsToTranslate, existingTranslations);
    }

    public static void main(String... args) {
        try {
            if (args.length != 2)
                System.exit(1);

            final File srcFolder = new File(args[0]);
            final File languageFileOrFolder = new File(args[1]);

            // Check if the source folder is present.
            if (!srcFolder.isDirectory())
                System.exit(1);

            // Get the strings to translate from the actual source code.
            Map<String, Set<TranslationItem>> stringsToTranslate = SourceCodeParser.parseSourceFolder(srcFolder);

            // If a single file is passed to the main method, we just update that particular file.
            if (languageFileOrFolder.isFile()) {
                updateFile(languageFileOrFolder, stringsToTranslate);
            } else if (languageFileOrFolder.isDirectory()) {
                for (File file: languageFileOrFolder.listFiles()) {
                    if (file.getName().endsWith(".lang") && !file.getName().startsWith("_")) {
                        updateFile(file, stringsToTranslate);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Runtime.getRuntime().exit(1);
        }
    }
}
