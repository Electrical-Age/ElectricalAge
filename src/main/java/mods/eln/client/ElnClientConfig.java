package mods.eln.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The player's own settings (config/eln-client.toml); a change to the file applies at once. */
public final class ElnClientConfig {
    private ElnClientConfig() {
    }

    public static final ModConfigSpec SPEC;
    static final ModConfigSpec.BooleanValue STATIC_PARTS_IN_CHUNKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("rendering");
        STATIC_PARTS_IN_CHUNKS = b.comment("Bake stationary device parts into chunk geometry and render only moving parts each frame.",
                "Improves frame rate in dense bases; disable to render entire devices dynamically every frame.")
            .define("staticPartsInChunks", true);
        b.pop();
        SPEC = b.build();
    }

    static void refresh() {
        mods.eln.client.render.bake.ChunkBake.INSTANCE.configure(STATIC_PARTS_IN_CHUNKS.get());
    }
}
