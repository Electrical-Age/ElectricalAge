package mods.eln.api.spi;

import mods.eln.api.ElnApi;

/**
 * Finds EA's implementation of the API at first use, by class name, in the class loader the API's classes came from.
 * EA's mod jar holds the API's classes and the implementation together; the eln-api jar holds the API alone, for
 * compiling against. So without EA the API's entry points can't work: they throw IllegalStateException saying so
 * (when the API's classes are there at all: a mod that compiled against the eln-api jar and runs without EA gets
 * NoClassDefFoundError instead, unless it checks ModList.get().isLoaded("eln") first).
 */
public final class Implementations {
    private Implementations() {
    }

    /** EA's mod id. */
    public static final String MOD_ID = "eln";

    static final String API_IMPL = "mods.eln.api.impl.ElnApiImpl";
    private static volatile ElnApiProvider api;

    /** EA's ElnApiProvider. Throws IllegalStateException without EA. */
    public static ElnApiProvider api() {
        ElnApiProvider p = api;
        if (p == null) api = p = load(ElnApiProvider.class, API_IMPL);
        return p;
    }

    /**
     * The type held in the public static INSTANCE field of className, looked up with type's class loader. Throws
     * IllegalStateException naming what is missing (and how a mod runs without EA) if there is no such class; a
     * failure isn't remembered, so a later call looks again.
     */
    public static <T> T load(Class<T> type, String className) {
        Class<?> c;
        try {
            c = Class.forName(className, true, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(missing(type), e);
        }
        try {
            return type.cast(c.getField("INSTANCE").get(null));
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException(className + " is not Electrical Age's " + type.getSimpleName()
                + ": the mods.eln.api classes and EA's implementation come from different builds", e);
        }
    }

    static String missing(Class<?> type) {
        return "Electrical Age (mod id \"" + MOD_ID + "\") is not loaded: these are the mods.eln.api " + ElnApi.API_VERSION
            + " classes without EA's implementation (" + type.getSimpleName() + "). A mod that runs without EA checks "
            + "ModList.get().isLoaded(\"" + MOD_ID + "\") before it touches mods.eln.api, and compiles against the "
            + "eln-api jar without shipping it (compileOnly); see EA's docs/api.md, \"Without Electrical Age\".";
    }
}
