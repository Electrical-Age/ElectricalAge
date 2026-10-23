package mods.eln.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.gui.layout.Layout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** The screen layout engine (P4b), and every screen's layout file: no overlaps, nothing outside its panel. */
class LayoutTest {
    static Layout.Result lay(String json) {
        return Layout.layout(JsonParser.parseString(json).getAsJsonObject(), Layout.FIXED);
    }

    @Test
    void rowPlacesSideBySideWithGap() {
        var r = lay("{\"row\": [{\"slot\": \"a\"}, {\"slot\": \"b\"}], \"gap\": 4}");
        assertEquals(new Layout.Rect(7, 7, 18, 18), r.get("slot", "a"));
        assertEquals(new Layout.Rect(29, 7, 18, 18), r.get("slot", "b"));
        assertEquals(176, r.width());
        assertEquals(18 + 14, r.height());
        assertTrue(r.problems().isEmpty());
    }

    @Test
    void columnStacksAndFillStretches() {
        var r = lay("{\"column\": [{\"button\": \"x\", \"sample\": \"Go\"}, {\"gauge\": \"g\", \"fill\": true}], \"gap\": 2}");
        assertEquals(new Layout.Rect(7, 7, 22, 20), r.get("button", "x"));
        assertEquals(new Layout.Rect(7, 29, 162, 12), r.get("gauge", "g"));
    }

    @Test
    void fillSharesTheSpareWidthOfARow() {
        var r = lay("{\"row\": [{\"button\": \"a\", \"sample\": \"A\", \"fill\": true}, {\"button\": \"b\", \"sample\": \"B\", \"fill\": true}], \"gap\": 2}");
        Layout.Rect a = r.get("button", "a"), b = r.get("button", "b");
        assertEquals(162, a.w() + b.w() + 2);
        assertEquals(a.right() + 2, b.x());
    }

    @Test
    void textIsAsWideAsItsWidestSample() {
        var r = lay("{\"column\": [{\"text\": \"t\", \"sample\": [\"ab\", \"abcdef\"]}]}");
        assertEquals(36, r.get("text", "t").w());
    }

    @Test
    void wideContentWidensThePanel() {
        var r = lay("{\"row\": [{\"field\": \"f\", \"chars\": 40}]}");
        assertEquals(40 * 6 + 10 + 14, r.width());
    }

    @Test
    void rowCentresItsChildrenVertically() {
        var r = lay("{\"row\": [{\"slot\": \"s\", \"big\": true}, {\"slot\": \"t\"}]}");
        assertEquals(7 + 4, r.get("slot", "t").y());
    }

    @Test
    void anOverlapIsReported() {
        var r = new Layout.Result(176, 40, List.of(new Layout.Placed("slot", "a", new Layout.Rect(7, 7, 18, 18)),
            new Layout.Placed("slot", "b", new Layout.Rect(20, 10, 18, 18)), new Layout.Placed("text", "c", new Layout.Rect(170, 7, 20, 9))));
        assertEquals(2, r.problems().size());
    }

    /** Every layout shipped in assets/eln/gui must be sound with the fixed-width measure. */
    @Test
    void everyShippedLayoutIsSound() throws IOException {
        Path dir = Path.of(System.getProperty("eln.guiDir", "src/main/resources/assets/eln/gui"));
        assertTrue(Files.isDirectory(dir), "no " + dir.toAbsolutePath());
        List<String> problems = new ArrayList<>();
        int n = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json")).sorted()::iterator) {
                n++;
                JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                for (String s : Layout.layout(o, Layout.FIXED).problems()) problems.add(p.getFileName() + ": " + s);
            }
        }
        assertTrue(n > 0, "no layouts found");
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
