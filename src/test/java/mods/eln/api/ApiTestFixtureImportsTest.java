package mods.eln.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hosted-machine fixture (mods.eln.apitest) is written as another mod would be: no EA type but mods.eln.api's
 * (not api.impl, .spi, .element or the testkit), in imports or anywhere in its text. The build also compiles it against
 * the eln-api jar alone (compileApiConsumer).
 */
public class ApiTestFixtureImportsTest {
    static final Pattern EA = Pattern.compile("mods\\.eln\\.[A-Za-z_][A-Za-z0-9_.]*");

    @Test
    void usesOnlyThePublicApi() throws IOException {
        // (the tests' working directory isn't the project's: find src/main from the resources path the build passes)
        String res = System.getProperty("eln.resources", "src/main/resources");
        Path dir = Path.of(res).resolveSibling("java").resolve("mods/eln/apitest");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertTrue(files.size() >= 3, "fixture sources in " + dir.toAbsolutePath() + ": " + files);
        List<String> bad = new ArrayList<>();
        for (Path f : files) {
            int n = 0;
            for (String line : Files.readAllLines(f)) {
                n++;
                if (line.startsWith("package ")) continue;
                Matcher m = EA.matcher(line);
                while (m.find()) {
                    String ref = m.group();
                    boolean ok = ref.equals("mods.eln.api") || ref.startsWith("mods.eln.apitest.")
                        || ref.startsWith("mods.eln.api.") && !ref.startsWith("mods.eln.api.impl") && !ref.startsWith("mods.eln.api.testkit")
                        && !ref.startsWith("mods.eln.api.spi") && !ref.startsWith("mods.eln.api.element");
                    if (!ok) bad.add(f.getFileName() + ":" + n + " " + ref);
                }
            }
        }
        assertTrue(bad.isEmpty(), "EA types outside the public API in the fixture: " + bad);
    }
}
