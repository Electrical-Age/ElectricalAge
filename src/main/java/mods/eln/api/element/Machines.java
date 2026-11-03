package mods.eln.api.element;

import mods.eln.Eln;
import mods.eln.api.BurnHandler;
import mods.eln.api.MachineSpec;
import mods.eln.api.TripCause;
import mods.eln.api.impl.ElementBurn;
import mods.eln.api.impl.ElementHost;
import mods.eln.api.impl.ElementSite;
import mods.eln.api.impl.MnaMachine;
import mods.eln.api.impl.ServerThread;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;

import java.util.Objects;

/**
 * Makes machines. forElement puts one on an EA element: call it in the element's constructor (its parts must be in
 * the element's lists before readFromNBT), keep the ElementMachine, and delegate getElectricalLoad,
 * getConnectionMask and getWaila to it (with the cable position, when the spec puts signals at CablePositions). The work goes in the element's own slow process, which takes or gives
 * energy each tick.
 * <p>
 * Tick order in game: slow pre-processes (the machine plans the step: its load or source), the electrical step,
 * then the slow processes, the machine's first (it books what the step delivered and updates its state), then the
 * element's (the owner's takes or gives).
 */
public final class Machines {
    private Machines() {
    }

    /**
     * The overvoltage burn of an EA element: the spec's burnsOnOvervoltage(Machines.BURNS_OUT). The element burns out
     * as EA's devices do (a repair brings it back); forElement accepts no other handler.
     */
    public static final BurnHandler BURNS_OUT = ElementBurn.INSTANCE;

    /**
     * A machine on a transparent-node element: its power terminal (a small-resistance node, as a cable's), signal
     * gates and processes join the element's lists; its state saves under the element's "api.machine." keys. One
     * per element (a second throws IllegalStateException). A bus face throws IllegalArgumentException: transparent
     * nodes have no bus port. A machine that burns (burnsOnOvervoltage(BURNS_OUT)) burns out as an EA machine does (a
     * repair brings it back); another burn handler (for other mods' blocks) throws IllegalArgumentException.
     */
    public static ElementMachine forElement(TransparentNodeElement element, MachineSpec spec) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(spec, "spec");
        if (spec.busFace().isPresent())
            throw new IllegalArgumentException(spec + ": a transparent node has no bus port; drop .bus(...) from the spec");
        if (spec.burnsOnOvervoltage() && !ElementBurn.of(spec))
            throw new IllegalArgumentException(spec + ": an EA element burns out EA's way (a repair brings it back); "
                + "use burnsOnOvervoltage(Machines.BURNS_OUT)");
        if (Eln.simulator != null) MnaMachine.checkSimulator(Eln.simulator);
        ElementSite site = new ElementSite() {
            @Override
            public Direction front() {
                return element.front;
            }

            @Override
            public boolean connected(Direction side, LRDU lrdu) {
                return element.node != null && element.node.lrduCubeMask.get(side, lrdu);
            }

            @Override
            public void checkThread() {
                ServerThread.check();
            }

            @Override
            public boolean burnt() {
                return element.isBurnt();
            }

            private Failure failure;

            @Override
            public void burn(TripCause cause, double severity) {
                if (failure == null) failure = new Failure(element).machine();
                failure.fail(FailureCause.OVERVOLTAGE, cause.measured(), severity);
            }

            @Override
            public double powerRs() {
                return ElnDeviceRegistry.getSmallRs();
            }

            @Override
            public double ambientCelsius() {
                return element.ambient + mods.eln.sim.PhysicalConstant.Tamb;   // (stamped from Ambient as it joins the simulation)
            }

            @Override
            public double signalRs() {
                return ElnDeviceRegistry.signalCableDescriptor.electricalRs;
            }

            @Override
            public String where() {
                return element.getClass().getSimpleName() + (element.node == null ? "" : " at " + element.node.coordonate);
            }
        };
        return new ElementHost(spec, site, element.electricalLoadList, element.electricalComponentList,
            element.slowProcessList, element.slowPreProcessList);
    }
}
