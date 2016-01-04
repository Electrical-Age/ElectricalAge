package mods.eln.i18n;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

class Main {
    public static void main(String... args) {
        try {
            final File languageFile = new File(args[1]);
            final File srcFolder = new File(args[0]);

            // Check if the source folder is present.
            if (!srcFolder.isDirectory())
                System.exit(1);

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
