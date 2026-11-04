package mods.eln.introspect;

import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.RelativeFace;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A machine on the EA API (element-hosted or a foreign block entity) in a report. Its spec and state go in a section
 * "machine" of spec and state, so a device that describes itself too can't collide with them; a tripped machine adds
 * a fault named after its state; a hosted machine (no EA element) gets its ports from the spec.
 */
public final class MachineDescribe {
    private MachineDescribe() {
    }

    /** The machine's spec, state and trip into r (server half). */
    public static void into(Report r, MachineView v) {
        spec(r.spec().section("machine"), v.spec());
        Section st = r.state().section("machine");
        state(st, v);
        v.lastTrip().ifPresent(t -> {
            Section trip = st.section("last_trip");
            trip.choice("state", t.state());
            trip.num("measured", t.measured(), unitOf(t.state()));
            trip.num("limit", t.limit(), unitOf(t.state()));
            trip.bool("burns", t.burns());
        });
        MachineState s = v.state();
        if (s.tripped())
            r.fault(s, v.lastTrip().map(Object::toString).orElse(s.name()));
    }

    /** Ports from the spec: power faces, signals in and out, the bus face (for machines that have no EA element). */
    public static void ports(Report r, MachineSpec spec) {
        List<String> power = new ArrayList<>();
        for (RelativeFace f : spec.powerFaces()) power.add(face(f));
        if (!power.isEmpty()) r.port("power", Port.Kind.POWER, String.join(" ", power), null, null);
        for (MachineSpec.SignalIn s : spec.signalsIn())
            r.port(portName("in", s.name()), Port.Kind.SIGNAL, face(s.face()), null, null);
        for (MachineSpec.SignalOut s : spec.signalsOut())
            r.port(portName("out", s.name()), Port.Kind.SIGNAL, face(s.face()), null, null);
        spec.busFace().ifPresent(f -> r.port("bus", Port.Kind.BUS, face(f), null, null));
    }

    /** What both halves of a hosted machine must agree on: its state (the client's view is the server's last snapshot). */
    public static void sync(Report r, MachineView v) {
        r.sync().choice("machine_state", v.state());
    }

    static void spec(Section s, MachineSpec spec) {
        s.choice("kind", spec.kind());
        s.num(Keys.NOMINAL_VOLTAGE, spec.nominalVolts(), Unit.V);
        s.num(Keys.RATED_POWER, spec.ratedWatts(), Unit.W);
        s.num(Keys.IDLE_POWER, spec.idleWatts(), Unit.W);
        s.num(Keys.MIN_VOLTAGE_RATIO, spec.minVoltageRatio(), Unit.RATIO);
        s.choice("brownout", spec.brownout());
        s.num("trip_cooldown", spec.tripCooldownSeconds(), Unit.S);
        s.bool("burns_on_overvoltage", spec.burnsOnOvervoltage());
        spec.thermal().ifPresent(t -> {
            Section th = s.section("thermal");
            th.num("heat_capacity", t.heatCapacityJPerK(), Unit.J_PER_K);
            th.num("heat_loss", t.heatLossWPerK(), Unit.W_PER_K);
            if (t.overheatRelative()) th.num("overheat_rise", t.overheatKelvin(), Unit.K);
            else th.num("overheat_at", t.overheatKelvin(), Unit.K);
            th.num("overheat_reset", t.overheatResetKelvin(), Unit.K);
        });
    }

    static void state(Section s, MachineView v) {
        s.choice("machine_state", v.state());
        s.num(Keys.VOLTAGE, v.volts(), Unit.V);
        s.num(Keys.VOLTAGE_RATIO, v.voltageRatio(), Unit.RATIO);
        s.num(Keys.CURRENT, v.amps(), Unit.A);
        s.num(Keys.POWER, v.watts(), Unit.W);
        s.num(Keys.BUFFER_ENERGY, v.bufferJoules(), Unit.J);
        s.num("buffer_capacity", v.bufferCapacityJoules(), Unit.J);
        s.num("energy_delivered", v.joulesDelivered(), Unit.J);
        s.num("energy_taken", v.joulesTaken(), Unit.J);
        s.num("energy_given", v.joulesGiven(), Unit.J);
        s.num("energy_lost", v.joulesLost(), Unit.J);
        s.num(Keys.TEMPERATURE, v.celsius(), Unit.C);
    }

    private static Unit unitOf(MachineState s) {
        return s == MachineState.OVERHEAT_TRIPPED ? Unit.C : Unit.V;
    }

    private static String face(RelativeFace f) {
        return f.name().toLowerCase(Locale.ROOT);
    }

    /** "in_enable" for a signal named "enable"; a name that isn't a key is made one. */
    private static String portName(String dir, String name) {
        String k = Describe.snake(name);
        return dir + "_" + (k.isEmpty() ? "signal" : k);
    }
}
