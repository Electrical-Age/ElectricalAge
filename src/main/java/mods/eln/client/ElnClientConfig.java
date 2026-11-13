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
        STATIC_PARTS_IN_CHUNKS = b.comment("Draw the parts of devices that hold still as part of the chunk, like blocks, and only the moving parts",
                "every frame. Much cheaper where many devices stand. Off = every device is drawn whole every frame, as before.")
            .define("staticPartsInChunks", true);
        b.pop();
        SPEC = b.build();
    }

    static void refresh() {
        mods.eln.client.render.bake.ChunkBake.INSTANCE.configure(STATIC_PARTS_IN_CHUNKS.get());
    }
}
