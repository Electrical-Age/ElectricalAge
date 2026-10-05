package mods.eln;

public class CommonProxy {
    public static final String CABLE_PNG = "/mods/eln/sprites/CABLE.PNG";
    public static final String CABLENODE_PNG = "/mods/eln/sprites/CABLENODE.PNG";
    public static final String THERMALCABLE_PNG = "/mods/eln/sprites/TEX_THERMALCABLEBASE.PNG";

    // Client stuff
    public void registerRenderers() {
        // Nothing here as the server doesn't render graphics!
    }

    /** TE description data (EA publish packet bytes) arrived on the client; see NodeBlockEntity.getUpdateTag. */
    public void handleDescriptionPacket(byte[] data) {
    }
}
