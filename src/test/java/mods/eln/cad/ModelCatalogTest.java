package mods.eln.cad;

import org.erisia.cad.Checker;
import org.erisia.cad.Solid;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The catalog's models pass the checker, and the committed files are what ./gradlew cadModels writes. */
public class ModelCatalogTest {
    @Test
    void everyModelPassesAndIsCommitted() throws Exception {
        Path tmp = Files.createTempDirectory("cad-catalog");
        ModelCatalog.writeAll(tmp);
        Path res = Path.of(System.getProperty("eln.resources", "src/main/resources"));
        List<Path> written = new ArrayList<>();
        try (Stream<Path> s = Files.walk(tmp)) {
            s.filter(Files::isRegularFile).forEach(written::add);
        }
        for (Path p : written) {
            Path rel = tmp.relativize(p);
            assertTrue(Files.exists(res.resolve(rel)), rel + " isn't committed: run ./gradlew cadModels");
            assertEquals(-1L, Files.mismatch(p, res.resolve(rel)), rel + " is stale: run ./gradlew cadModels");
        }
        for (var b : ModelCatalog.BLOCKS) assertEquals(List.of(), Checker.check(b.model().get()), b.name());
        for (var n : ModelCatalog.NODES)
            for (var part : n.parts().get()) {
                var m = new org.erisia.cad.Model();
                for (Solid s : part.solids()) m.add(s);
                assertEquals(List.of(), Checker.check(m), n.name() + "/" + part.name());
            }
    }
}
