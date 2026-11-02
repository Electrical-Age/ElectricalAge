package mods.eln.api.element;

import mods.eln.api.Machine;
import mods.eln.api.RelativeFace;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalLoad;

import java.util.Map;

/**
 * A machine hosted by one of EA's own elements (Machines.forElement). Its parts are in the element's lists, so the
 * element's connect, disconnect, burn, repair and NBT handle them; the element delegates its faces and Jade lines
 * here (wailaLines). Faces are resolved against the element's front each call (RelativeFace.FRONT is the element's front,
 * LEFT its front.left(), UP its front.up()), so a machine turned with the wrench follows.
 */
public interface ElementMachine extends Machine {
    /**
     * The load a cable on side connects to: the power terminal, a signal's gate, or null where the spec has nothing.
     * Null on a face whose signals sit at CablePositions: ask electricalLoad(side, lrdu).
     */
    ElectricalLoad electricalLoad(Direction side);

    /** The load a cable on side at position lrdu connects to (signals at CablePositions take only theirs), or null. */
    ElectricalLoad electricalLoad(Direction side, LRDU lrdu);

    /**
     * The element's connection mask on side, for every cable position on it: maskElectricalPower on power faces,
     * maskElectricalInputGate / maskElectricalOutputGate on a signal's face, 0 elsewhere (and on a face whose
     * signals sit at CablePositions: ask connectionMask(side, lrdu)). An element that only takes some positions
     * filters them.
     */
    int connectionMask(Direction side);

    /** The connection mask on side at position lrdu: as connectionMask(side), with signals only at their positions. */
    int connectionMask(Direction side, LRDU lrdu);

    /** The face side is on, relative to the element's front; empty before the element is placed. */
    java.util.Optional<RelativeFace> relativeFace(Direction side);

    /** The power terminal, for the element's meters. */
    ElectricalLoad powerLoad();

    /**
     * The machine's Jade lines, in order, for the element to place among its own (putAll where it wants them):
     * "Voltage", "Power consumption" (a producer: "Power output"), and "Machine" with its state when it isn't
     * running. Keys are translated; none is "State", which stays the device's own.
     */
    Map<String, String> wailaLines();
}
