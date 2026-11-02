package mods.eln.api;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The eln-api jars other mods compile against (build.gradle: apiJar, apiTestkitJar): they hold the API's packages and
 * nothing else of EA's, their classes name no EA class outside them (constant pools: every class, field and method
 * reference and descriptor), the API jar names nothing of the testkit's, and their manifests carry API_VERSION.
 */
public class ApiJarTest {
    static final Set<String> API = Set.of("mods/eln/api", "mods/eln/api/lowlevel", "mods/eln/api/spi");
    static final Set<String> TESTKIT = Set.of("mods/eln/api/testkit", "mods/eln/api/testkit/spi");
    static final Pattern EA = Pattern.compile("mods/eln/[A-Za-z0-9_/$]+");

    @Test
    void theApiJarHoldsTheApiAloneAndNamesNothingElseOfEas() throws IOException {
        check(System.getProperty("eln.apiJar"), API, API);
    }

    @Test
    void theTestkitJarHoldsTheTestkitAloneAndNamesOnlyItAndTheApi() throws IOException {
        Set<String> allowed = new TreeSet<>(API);
        allowed.addAll(TESTKIT);
        check(System.getProperty("eln.apiTestkitJar"), TESTKIT, allowed);
    }

    static void check(String path, Set<String> packages, Set<String> allowed) throws IOException {
        assertTrue(path != null, "the build passes the jar's path (eln.apiJar, eln.apiTestkitJar)");
        List<String> problems = new ArrayList<>();
        Set<String> seen = new TreeSet<>();
        int classes = 0;
        try (JarFile jar = new JarFile(path)) {
            assertEquals(ElnApi.API_VERSION, jar.getManifest().getMainAttributes().getValue("Eln-Api-Version"), path + ": manifest");
            for (JarEntry e : jar.stream().toList()) {
                String name = e.getName();
                if (e.isDirectory() || name.startsWith("META-INF/")) continue;
                if (!name.endsWith(".class")) {
                    problems.add(name + ": not a class");
                    continue;
                }
                classes++;
                String pkg = name.substring(0, name.lastIndexOf('/'));
                seen.add(pkg);
                if (!packages.contains(pkg)) problems.add(name + ": outside " + packages);
                try (InputStream in = jar.getInputStream(e)) {
                    for (String utf : utf8Constants(in)) {
                        Matcher m = EA.matcher(utf);
                        while (m.find()) {
                            String ref = m.group();
                            String refPkg = ref.substring(0, ref.lastIndexOf('/'));
                            if (!allowed.contains(refPkg)) problems.add(name + " names " + ref);
                        }
                    }
                }
            }
        }
        assertEquals(packages, seen, path + ": packages");
        assertTrue(classes >= packages.size(), path + ": " + classes + " classes");
        assertTrue(problems.isEmpty(), path + ":\n" + String.join("\n", problems));
    }

    /** The CONSTANT_Utf8 entries of a class file's constant pool (class names, descriptors, signatures, strings). */
    static List<String> utf8Constants(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        if (in.readInt() != 0xCAFEBABE) throw new IOException("not a class file");
        in.readUnsignedShort();
        in.readUnsignedShort();
        int count = in.readUnsignedShort();
        List<String> out = new ArrayList<>();
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> out.add(in.readUTF());
                case 7, 8, 16, 19, 20 -> in.skipNBytes(2);
                case 15 -> in.skipNBytes(3);
                case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                case 5, 6 -> {
                    in.skipNBytes(8);
                    i++;
                }
                default -> throw new IOException("constant pool tag " + tag);
            }
        }
        return out;
    }
}
