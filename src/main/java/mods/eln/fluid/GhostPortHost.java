package mods.eln.fluid;

import mods.eln.sim.fluid.FluidDevice;

import java.util.List;

/**
 * A multiblock whose fluid ports sit on several of its blocks (the Large Boiler's, at its node or on its ghost blocks),
 * or one of its ghost nodes: the ports on one block, for FluidPlacement.portsAt (the pipe tool asks the node at a block
 * for its ports, and a multiblock's other blocks have none of their own).
 */
public interface GhostPortHost {
    /** Its ports on block pos (BlockPos.asLong): a fluid device's own ports there (a ghost node overrides it). */
    default List<FluidDevice.FluidPort> portsAt(long pos) {
        List<FluidDevice.FluidPort> out = new java.util.ArrayList<>();
        if (this instanceof FluidDevice d) for (FluidDevice.FluidPort p : d.fluidPorts()) if (p.pos() == pos) out.add(p);
        return out;
    }
}
