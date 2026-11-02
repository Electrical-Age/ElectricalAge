package mods.eln.api;

import mods.eln.misc.Direction;
import mods.eln.sim.ElectricalLoad;

import java.util.Map;

/**
 * A machine hosted by one of EA's own elements (Machines.forElement). Its parts are in the element's lists, so the
 * element's connect, disconnect, burn, repair and NBT handle them; the element delegates its faces and Jade lines
 * here. Faces are resolved against the element's front each call (RelativeFace.FRONT is the element's front,
 * LEFT its front.left(), UP its front.up()), so a machine turned with the wrench follows.
 */
public interface ElementMachine extends Machine {
    /** The load a cable on side connects to: the power terminal, a signal's gate, or null where the spec has nothing. */
    ElectricalLoad electricalLoad(Direction side);

    /**
     * The element's connection mask on side, for every cable position on it: maskElectricalPower on power faces,
     * maskElectricalGate on signal faces, 0 elsewhere. An element that only takes some positions filters them.
     */
    int connectionMask(Direction side);

    /** The face side is on, relative to the element's front; empty before the element is placed. */
    java.util.Optional<RelativeFace> relativeFace(Direction side);

    /** The power terminal, for the element's meters. */
    ElectricalLoad powerLoad();

    /** Adds the machine's Jade lines to info: volts, watts, and its state when not running. */
    void addWaila(Map<String, String> info);
}
