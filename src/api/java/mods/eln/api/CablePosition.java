package mods.eln.api;

/**
 * Where on a face a signal's cable meets the block: anywhere on it (ANY, the default), or only a cable lying on the
 * floor or the ceiling of the block beside that face. Two signals may share a face at different positions, e.g. an
 * input on the floor and an output on the ceiling of a flank whose other faces are taken. FLOOR and CEILING are for
 * the side faces (FRONT, BACK, LEFT, RIGHT); on a block facing up or down they are the down and up of EA's frame for
 * that face.
 * <p>
 * An edge holds up to three pins (Baughn 2026-11-05: several pins per edge, as EA's displays): FLOOR (or CEILING) in
 * the middle, and FLOOR_LEFT / FLOOR_RIGHT (CEILING_LEFT / CEILING_RIGHT) a quarter block to the left and right, as
 * seen facing that face from outside. Only a voxel cable meets the side pins: one lying on that floor (or ceiling) of
 * a voxel block beside the face and ending at the pin. A plain six-node cable meets only the middle pin.
 */
public enum CablePosition {
    ANY, FLOOR, CEILING, FLOOR_LEFT, FLOOR_RIGHT, CEILING_LEFT, CEILING_RIGHT;

    /** On the floor edge: FLOOR, FLOOR_LEFT or FLOOR_RIGHT. */
    public boolean floor() {
        return this == FLOOR || this == FLOOR_LEFT || this == FLOOR_RIGHT;
    }

    /** On the ceiling edge: CEILING, CEILING_LEFT or CEILING_RIGHT. */
    public boolean ceiling() {
        return this == CEILING || this == CEILING_LEFT || this == CEILING_RIGHT;
    }

    /** A side pin, off the middle of its edge: only voxel cables meet it. */
    public boolean side() {
        return this == FLOOR_LEFT || this == FLOOR_RIGHT || this == CEILING_LEFT || this == CEILING_RIGHT;
    }
}
