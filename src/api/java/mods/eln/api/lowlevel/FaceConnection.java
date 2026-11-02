package mods.eln.api.lowlevel;

import mods.eln.api.RelativeFace;

import java.util.Objects;

/**
 * What cables join at one face of a hosted circuit's block: load, at every cable position of face (relative to the
 * block's facing, as in a MachineSpec). A SignalLoad (or SignalSource) takes signal cables, any other load power
 * cables. One load on two faces joins them: power passes through the block, and the line is cut when it unloads.
 */
public record FaceConnection(RelativeFace face, Load load) {
    public FaceConnection {
        Objects.requireNonNull(face, "face");
        Objects.requireNonNull(load, "load");
    }

    /** What the face carries. */
    public PortKind kind() {
        return load instanceof SignalLoad ? PortKind.SIGNAL : PortKind.POWER;
    }
}
