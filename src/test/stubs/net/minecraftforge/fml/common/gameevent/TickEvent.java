package net.minecraftforge.fml.common.gameevent;

/** TEST STUB (tools/simtest.sh): only what Simulator.tick reads (ServerTickEvent.phase). Not on the mod's classpath. */
public class TickEvent {
    public enum Phase {
        START, END
    }

    public final Phase phase;

    public TickEvent(Phase phase) {
        this.phase = phase;
    }

    public static class ServerTickEvent extends TickEvent {
        public ServerTickEvent(Phase phase) {
            super(phase);
        }
    }
}
