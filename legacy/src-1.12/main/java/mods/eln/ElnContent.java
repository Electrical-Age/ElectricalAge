package mods.eln;

/**
 * Device content hooks (1.12 port, M1 split). Everything in Eln's lifecycle that touches device packages
 * (sixnode, transparentnode, simplenode, gridnode, mechanical, item, entity, ore, wiki, ...) lives in
 * mods.eln.registry.ElnContentImpl, which is built together with those packages. When they are excluded from the
 * build (M1: core only), Eln runs with the no-op defaults below.
 */
public interface ElnContent {
    /** preInit, after the core blocks/items exist: device blocks, items, descriptors. */
    default void preInit() {
    }

    /** FMLInitializationEvent: replicator, recipes. */
    default void init() {
    }

    /** First FMLPostInitializationEvent handler (other mods' ores). */
    default void modsLoaded() {
    }

    /** FMLServerAboutToStartEvent, after the simulator was initialised. */
    default void serverAboutToStart() {
    }

    /** FMLServerStartingEvent, after the world data and commands. */
    default void serverStarting() {
    }

    /** FMLServerStoppedEvent: clear device static state. */
    default void serverStopped() {
    }

    /** Server tick END (ServerEventListener). */
    default void serverTick() {
    }

    /** Client: device renderers and overlays (ClientProxy.registerRenderers). */
    default void clientInit() {
    }

    /** Client: joined a server (ConnectionListener). */
    default void clientConnected() {
    }

    /** Client: the wiki key was pressed (ClientKeyHandler). */
    default void openWiki() {
    }

    static ElnContent load() {
        try {
            return (ElnContent) Class.forName("mods.eln.registry.ElnContentImpl").newInstance();
        } catch (ClassNotFoundException e) {
            System.out.println("Electrical Age: device content not in this build (core only)");
            return new ElnContent() {
            };
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Electrical Age: cannot create device content", e);
        }
    }
}
