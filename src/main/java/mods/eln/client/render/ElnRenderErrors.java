package mods.eln.client.render;

import mods.eln.Eln;

import java.util.HashSet;
import java.util.Set;

/** A render that throws is logged once per kind and message, not every frame (P3). */
public final class ElnRenderErrors {
    private ElnRenderErrors() {
    }

    private static final Set<String> SEEN = new HashSet<>();

    public static void report(String what, RuntimeException e) {
        if (SEEN.add(what + e)) Eln.LOG.error("EA render error in {}", what, e);
    }
}
