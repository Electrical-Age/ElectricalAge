package net.minecraftforge.fml.common;

import net.minecraftforge.fml.common.eventhandler.EventBus;

/** TEST STUB (tools/simtest.sh): Simulator's constructor calls instance().bus().register(this). Not on the mod's classpath. */
public class FMLCommonHandler {
    private static final FMLCommonHandler INSTANCE = new FMLCommonHandler();
    private final EventBus bus = new EventBus();

    public static FMLCommonHandler instance() { return INSTANCE; }

    public EventBus bus() { return bus; }
}
