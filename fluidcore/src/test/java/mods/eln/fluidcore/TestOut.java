package mods.eln.fluidcore;

import java.nio.file.Files;
import java.nio.file.Path;

/** Where the core's tests write their logs: -Dfluidcore.out=DIR, else build/fluidcore-out (made if missing). */
final class TestOut {
    private TestOut() {
    }

    static Path path(String name) {
        Path dir = Path.of(System.getProperty("fluidcore.out", "build/fluidcore-out"));
        try {
            Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return dir.resolve(name);
    }
}
