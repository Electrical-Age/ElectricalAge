package mods.eln.api.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docs/api.md can't rot: every java code block in it names a region of a compiled source in this package (a fence
 * "```java src/test/java/mods/eln/api/docs/Mill.java#spec" and, in the file, the lines between "// doc:begin spec" and
 * "// doc:end spec"), and must equal that region (its common indentation removed). Every region here is shown in the
 * page, and the samples use only what the eln-api jars hold: mods.eln.api and .lowlevel, the testkit in the tests
 * (*Test.java); not mods.eln.api.impl, .spi or .element (but HeaterElement, the EA-element sample, which names EA's
 * types). The build also compiles them against the jars alone (compileApiConsumer). Code blocks of other languages are
 * refused: every sample on the page is compiled.
 */
public class ApiDocSamplesTest {
    static final Pattern FENCE = Pattern.compile("^```java(?:\\s+(\\S+)#(\\S+))?\\s*$");
    static final Pattern BEGIN = Pattern.compile("^\\s*// doc:begin (\\S+)\\s*$"), END = Pattern.compile("^\\s*// doc:end (\\S+)\\s*$");
    static final Pattern EA = Pattern.compile("mods\\.eln\\.[A-Za-z_][A-Za-z0-9_.]*");
    static final String DIR = "src/test/java/mods/eln/api/docs";

    /** The project's root (the tests' working directory isn't it: from the resources path the build passes). */
    static Path root() {
        Path res = Path.of(System.getProperty("eln.resources", "src/main/resources")).toAbsolutePath();
        return res.getParent().getParent().getParent();
    }

    /** The doc regions of file: name -> lines, common indentation removed. */
    static Map<String, List<String>> regions(Path file) throws IOException {
        Map<String, List<String>> out = new LinkedHashMap<>();
        List<String> open = null;
        String name = null;
        for (String line : Files.readAllLines(file)) {
            Matcher b = BEGIN.matcher(line), e = END.matcher(line);
            if (b.matches()) {
                assertTrue(open == null, file.getFileName() + ": region " + b.group(1) + " opens inside " + name);
                name = b.group(1);
                open = new ArrayList<>();
            } else if (e.matches()) {
                assertTrue(open != null && e.group(1).equals(name), file.getFileName() + ": doc:end " + e.group(1) + " without its begin");
                assertTrue(out.put(name, dedent(open)) == null, file.getFileName() + ": region " + name + " twice");
                open = null;
            } else if (open != null) open.add(line.stripTrailing());
        }
        assertTrue(open == null, file.getFileName() + ": region " + name + " never ends");
        return out;
    }

    static List<String> dedent(List<String> lines) {
        int indent = Integer.MAX_VALUE;
        for (String l : lines) if (!l.isBlank()) indent = Math.min(indent, l.length() - l.stripLeading().length());
        List<String> out = new ArrayList<>();
        for (String l : lines) out.add(l.isBlank() ? "" : l.substring(indent));
        return out;
    }

    @Test
    void everyJavaBlockIsACompiledSampleAndMatchesIt() throws IOException {
        Path root = root();
        List<String> doc = Files.readAllLines(root.resolve("docs/api.md"));
        List<String> problems = new ArrayList<>();
        Map<String, Integer> used = new LinkedHashMap<>();
        int blocks = 0;
        for (int i = 0; i < doc.size(); i++) {
            Matcher f = FENCE.matcher(doc.get(i));
            if (!f.matches()) {
                if (doc.get(i).startsWith("```")) problems.add("api.md:" + (i + 1) + ": a code block that isn't a java sample: " + doc.get(i));
                continue;
            }
            int start = i + 1;
            List<String> body = new ArrayList<>();
            for (i = start; i < doc.size() && !doc.get(i).startsWith("```"); i++) body.add(doc.get(i).stripTrailing());
            blocks++;
            if (f.group(1) == null) {
                problems.add("api.md:" + start + ": a java block with no sample reference (```java <file>#<region>)");
                continue;
            }
            Path file = root.resolve(f.group(1));
            if (!f.group(1).startsWith(DIR + "/") || !Files.exists(file)) {
                problems.add("api.md:" + start + ": " + f.group(1) + " is not a sample file in " + DIR);
                continue;
            }
            List<String> region = regions(file).get(f.group(2));
            used.merge(file.getFileName() + "#" + f.group(2), 1, Integer::sum);
            if (region == null) problems.add("api.md:" + start + ": " + f.group(1) + " has no region " + f.group(2));
            else if (!region.equals(body))
                problems.add("api.md:" + start + ": differs from " + f.group(1) + "#" + f.group(2) + "\n--- page:\n"
                    + String.join("\n", body) + "\n--- source:\n" + String.join("\n", region));
        }
        assertTrue(blocks >= 10, "java blocks in api.md: " + blocks);
        try (Stream<Path> s = Files.list(root.resolve(DIR))) {
            for (Path p : s.filter(p -> p.toString().endsWith(".java")).sorted().toList())
                for (String r : regions(p).keySet())
                    if (!used.containsKey(p.getFileName() + "#" + r)) problems.add(p.getFileName() + "#" + r + " is in no block of api.md");
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    @Test
    void samplesUseOnlyThePublicApi() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> s = Files.list(root().resolve(DIR))) {
            for (Path f : s.filter(p -> p.toString().endsWith(".java")).toList()) {
                String n0 = f.getFileName().toString();
                if (n0.equals("HeaterElement.java") || n0.equals("ApiDocSamplesTest.java")) continue; // (EA's own element; this test)
                boolean test = n0.endsWith("Test.java");
                int n = 0;
                for (String line : Files.readAllLines(f)) {
                    n++;
                    if (line.startsWith("package ")) continue;
                    Matcher m = EA.matcher(line);
                    while (m.find()) {
                        String ref = m.group();
                        boolean ok = ref.equals("mods.eln.api") || ref.startsWith("mods.eln.api.")
                            && !ref.startsWith("mods.eln.api.impl") && !ref.startsWith("mods.eln.api.spi")
                            && !ref.startsWith("mods.eln.api.element") && (test || !ref.startsWith("mods.eln.api.testkit"));
                        if (!ok) bad.add(f.getFileName() + ":" + n + " " + ref);
                    }
                }
            }
        }
        assertEquals(List.of(), bad, "EA types outside the public API in the doc samples");
    }
}
