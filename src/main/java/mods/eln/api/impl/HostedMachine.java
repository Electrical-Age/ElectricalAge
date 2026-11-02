package mods.eln.api.impl;

import mods.eln.api.Machine;
import mods.eln.api.MachineBus;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The server's Machine for a hosted block entity: the live ElementHost while a node hosts it, else inert and
 * DETACHED (removed or unloaded, or a type never registered). One per block entity, so a handle the owner kept goes
 * inert with it. Trip listeners added while live stay with that machine.
 */
final class HostedMachine implements Machine {
    private static final Logger LOG = LoggerFactory.getLogger("eln");
    private static final MachineBus DEAD_BUS = new MachineBus() {
        @Override
        public double read(int channel) {
            return 0;
        }

        @Override
        public void write(int channel, double level) {
        }
    };

    private final String what;
    private final MachineView view = new ReadOnlyView(this);
    private volatile ElementHost live;
    private MachineSpec spec;
    private boolean warned;

    /** An inert machine of spec until attached; what names it in the log. */
    HostedMachine(MachineSpec spec, String what) {
        this.spec = spec;
        this.what = what;
    }

    void attach(ElementHost host) {
        live = host;
        spec = host.spec();
    }

    void detach() {
        ElementHost h = live;
        if (h != null) spec = h.spec();
        live = null;
    }

    /** The live machine, or null (inert): logs once that a detached machine is used. */
    private ElementHost live() {
        ElementHost h = live;
        if (h == null && !warned) {
            warned = true;
            LOG.warn("Electrical Age: the machine of {} is detached (its block entity is gone, or its type isn't registered); it does nothing", what);
        }
        return h;
    }

    private static void amount(double joules) {
        if (!(joules >= 0 && Double.isFinite(joules)))
            throw new IllegalArgumentException("an amount of energy is finite and not negative, got " + joules);
    }

    @Override
    public boolean tryTakeJoules(double joules) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) return h.tryTakeJoules(joules);
        amount(joules);
        return false;
    }

    @Override
    public double takeJoules(double joules) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) return h.takeJoules(joules);
        amount(joules);
        return 0;
    }

    @Override
    public double giveJoules(double joules) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) return h.giveJoules(joules);
        amount(joules);
        return 0;
    }

    @Override
    public double ratedJoulesPerTick() {
        ElementHost h = live;
        if (h != null) return h.ratedJoulesPerTick();
        double w = spec.kind() == MachineSpec.Kind.CONSUMER ? spec.ratedWatts() - spec.idleWatts() : spec.ratedWatts();
        return w * MachineCore.TICK_SECONDS;
    }

    @Override
    public double signal(String name) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) return h.signal(name);
        for (MachineSpec.SignalIn s : spec.signalsIn()) if (s.name().equals(name)) return s.defaultValue();
        return 0;
    }

    @Override
    public boolean signalConnected(String name) {
        ServerThread.check();
        ElementHost h = live();
        return h != null && h.signalConnected(name);
    }

    @Override
    public void setSignal(String name, double level) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) h.setSignal(name, level);
    }

    @Override
    public void releaseSignal(String name) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) h.releaseSignal(name);
    }

    @Override
    public MachineBus bus() {
        ServerThread.check();
        ElementHost h = live();
        return h != null ? h.bus() : DEAD_BUS;
    }

    @Override
    public void heatJoules(double joules) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) h.heatJoules(joules);
    }

    @Override
    public void onTrip(Consumer<TripCause> listener) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) h.onTrip(listener);
    }

    @Override
    public void respec(MachineSpec next) {
        ServerThread.check();
        ElementHost h = live();
        if (h != null) h.respec(next);
    }

    @Override
    public MachineView view() {
        return view;
    }

    // ---- MachineView: the live machine's, else DETACHED and zeros

    @Override
    public MachineSpec spec() {
        ElementHost h = live;
        return h != null ? h.spec() : spec;
    }

    @Override
    public MachineState state() {
        ElementHost h = live;
        return h != null ? h.state() : MachineState.DETACHED;
    }

    @Override
    public Optional<TripCause> lastTrip() {
        ElementHost h = live;
        return h != null ? h.lastTrip() : Optional.empty();
    }

    @Override
    public double volts() {
        ElementHost h = live;
        return h != null ? h.volts() : 0;
    }

    @Override
    public double voltageRatio() {
        ElementHost h = live;
        return h != null ? h.voltageRatio() : 0;
    }

    @Override
    public double amps() {
        ElementHost h = live;
        return h != null ? h.amps() : 0;
    }

    @Override
    public double watts() {
        ElementHost h = live;
        return h != null ? h.watts() : 0;
    }

    @Override
    public double bufferJoules() {
        ElementHost h = live;
        return h != null ? h.bufferJoules() : 0;
    }

    @Override
    public double bufferCapacityJoules() {
        ElementHost h = live;
        return h != null ? h.bufferCapacityJoules() : 0;
    }

    @Override
    public double joulesDelivered() {
        ElementHost h = live;
        return h != null ? h.joulesDelivered() : 0;
    }

    @Override
    public double joulesTaken() {
        ElementHost h = live;
        return h != null ? h.joulesTaken() : 0;
    }

    @Override
    public double joulesGiven() {
        ElementHost h = live;
        return h != null ? h.joulesGiven() : 0;
    }

    @Override
    public double joulesLost() {
        ElementHost h = live;
        return h != null ? h.joulesLost() : 0;
    }

    @Override
    public double celsius() {
        ElementHost h = live;
        return h != null ? h.celsius() : MachineSpec.AMBIENT_CELSIUS;
    }

    @Override
    public String toString() {
        return "machine of " + what + " (" + state() + ")";
    }
}
