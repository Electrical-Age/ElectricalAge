package mods.eln.api.impl;

import mods.eln.api.Machine;
import mods.eln.api.MachineBus;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The client's Machine for a hosted block entity: a read-only view of the snapshot the server last synced (UNPOWERED
 * and zeros before the first). What only the server has (takes, gives, signals, bus, trip listeners, respec) throws
 * IllegalStateException; the energy books read 0.
 */
final class ClientMachine implements Machine {
    private final MachineSpec spec;
    private final MachineData data;
    private final MachineView view = new ReadOnlyView(this);

    ClientMachine(MachineSpec spec, MachineData data) {
        this.spec = spec;
        this.data = data;
    }

    private static IllegalStateException serverOnly(String what) {
        return new IllegalStateException(what + " is the server's: on the client a machine is a read-only view");
    }

    private MachineData.Snapshot s() {
        return data.snapshot;
    }

    @Override
    public boolean tryTakeJoules(double joules) {
        throw serverOnly("tryTakeJoules");
    }

    @Override
    public double takeJoules(double joules) {
        throw serverOnly("takeJoules");
    }

    @Override
    public double giveJoules(double joules) {
        throw serverOnly("giveJoules");
    }

    @Override
    public double ratedJoulesPerTick() {
        double w = spec.kind() == MachineSpec.Kind.CONSUMER ? spec.ratedWatts() - spec.idleWatts() : spec.ratedWatts();
        return w * MachineCore.TICK_SECONDS;
    }

    @Override
    public double signal(String name) {
        throw serverOnly("signal");
    }

    @Override
    public boolean signalConnected(String name) {
        throw serverOnly("signalConnected");
    }

    @Override
    public void setSignal(String name, double level) {
        throw serverOnly("setSignal");
    }

    @Override
    public void releaseSignal(String name) {
        throw serverOnly("releaseSignal");
    }

    @Override
    public MachineBus bus() {
        throw serverOnly("bus");
    }

    @Override
    public void heatJoules(double joules) {
        throw serverOnly("heatJoules");
    }

    @Override
    public void onTrip(Consumer<TripCause> listener) {
        throw serverOnly("onTrip");
    }

    @Override
    public void respec(MachineSpec next) {
        throw serverOnly("respec");
    }

    @Override
    public MachineView view() {
        return view;
    }

    @Override
    public MachineSpec spec() {
        return spec;
    }

    @Override
    public MachineState state() {
        var s = s();
        return s == null ? MachineState.UNPOWERED : s.state();
    }

    @Override
    public Optional<TripCause> lastTrip() {
        var s = s();
        return s == null ? Optional.empty() : Optional.ofNullable(s.lastTrip());
    }

    @Override
    public double volts() {
        var s = s();
        return s == null ? 0 : s.volts();
    }

    @Override
    public double voltageRatio() {
        var s = s();
        return s == null ? 0 : s.voltageRatio();
    }

    @Override
    public double amps() {
        var s = s();
        return s == null ? 0 : s.amps();
    }

    @Override
    public double watts() {
        var s = s();
        return s == null ? 0 : s.watts();
    }

    @Override
    public double bufferJoules() {
        var s = s();
        return s == null ? 0 : s.bufferJoules();
    }

    @Override
    public double bufferCapacityJoules() {
        var s = s();
        return s == null ? 0 : s.bufferCapacityJoules();
    }

    @Override
    public double joulesDelivered() {
        return 0;
    }

    @Override
    public double joulesTaken() {
        return 0;
    }

    @Override
    public double joulesGiven() {
        return 0;
    }

    @Override
    public double joulesLost() {
        return 0;
    }

    @Override
    public double celsius() {
        var s = s();
        return s == null ? MachineSpec.AMBIENT_CELSIUS : s.celsius();
    }
}
