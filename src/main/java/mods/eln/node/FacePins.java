package mods.eln.node;

import mods.eln.bus.BusPort;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalLoad;

import java.util.List;

/**
 * A node with several pins on an edge of a face (Baughn 2026-11-05: as EA's displays): a voxel cable lying on that
 * edge's floor (or ceiling) in the block beside it and ending at a pin's point links to that pin (VoxelNode.connectPeerPins).
 * The edges listed here take no plain connection (a cable anywhere on them would meet the wrong pin).
 */
public interface FacePins {
    /** A pin: on edge lrdu of the face (the node's own frame), leftward voxels from the middle (seen facing the face
     *  from outside), with its connection mask and its load, or a bus port (then load is null). */
    record FacePin(LRDU lrdu, int leftward, int mask, ElectricalLoad load, BusPort bus) {
    }

    /** The pins on side (the face toward the neighbour), on the edges that have side pins or a bus pin; else empty. */
    List<FacePin> facePins(Direction side);
}
