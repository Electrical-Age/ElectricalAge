package mods.eln.api.impl;

import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;

import java.util.Optional;

/** A MachineView that only reads another and never exposes it, so the view handed out can't be used to write. */
public final class ReadOnlyView implements MachineView {
    private final MachineView of;

    public ReadOnlyView(MachineView of) {
        this.of = java.util.Objects.requireNonNull(of, "of");
    }

    @Override
    public MachineSpec spec() {
        return of.spec();
    }

    @Override
    public MachineState state() {
        return of.state();
    }

    @Override
    public Optional<TripCause> lastTrip() {
        return of.lastTrip();
    }

    @Override
    public double volts() {
        return of.volts();
    }

    @Override
    public double voltageRatio() {
        return of.voltageRatio();
    }

    @Override
    public double amps() {
        return of.amps();
    }

    @Override
    public double watts() {
        return of.watts();
    }

    @Override
    public double bufferJoules() {
        return of.bufferJoules();
    }

    @Override
    public double celsius() {
        return of.celsius();
    }

    @Override
    public double bufferCapacityJoules() {
        return of.bufferCapacityJoules();
    }

    @Override
    public double joulesDelivered() {
        return of.joulesDelivered();
    }

    @Override
    public double joulesTaken() {
        return of.joulesTaken();
    }

    @Override
    public double joulesGiven() {
        return of.joulesGiven();
    }

    @Override
    public double joulesLost() {
        return of.joulesLost();
    }
}
