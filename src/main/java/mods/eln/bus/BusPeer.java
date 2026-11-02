package mods.eln.bus;

import mods.eln.misc.Direction;

/**
 * A node that isn't a voxel node but has a bus port on a face (a machine hosted at another mod's block): a voxel bus
 * cable ending on that face links to it (VoxelNode.connectJob), as the node links to the cable from its side.
 */
public interface BusPeer {
    /** The bus port on side (the face toward the cable), or null. */
    BusPort busPortOn(Direction side);
}
