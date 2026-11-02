package mods.eln.api;

/**
 * A machine's field bus port: 16 channels numbered as Minecraft numbers its dyes (0 white .. 15 black), levels
 * 0..1. Double-buffered: what is written in a tick is read in the next, and a write lasts one tick, so write every
 * tick to hold a level. Several writers on one channel: the highest wins.
 */
public interface MachineBus {
    /** Channel c's level (0..1) as written last tick. c outside 0..15 throws IllegalArgumentException. */
    double read(int channel);

    /** Writes channel c for the next tick; the level must be 0..1. Bad channel or level throws IllegalArgumentException. */
    void write(int channel, double level);
}
