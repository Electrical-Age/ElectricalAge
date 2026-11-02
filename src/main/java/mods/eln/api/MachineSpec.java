package mods.eln.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a machine is, electrically: a consumer or producer at a nominal voltage, its power ratings, protections
 * and which face does what. Immutable; made by consumer(volts or tier) or producer(volts or tier), then the
 * builder's build(), which checks every setting and lists all problems in one IllegalArgumentException. Baughn: a mod hands over a nominal machine spec
 * and gets volts and joules back; every default is the safe one, and the unsafe options say so in their names.
 */
public final class MachineSpec {
    /** Draws power from the line, or feeds it. */
    public enum Kind {CONSUMER, PRODUCER}

    /** A signal input: read 0..1 by Machine.signal(name); defaultValue (0..1) when nothing drives it. */
    public record SignalIn(String name, RelativeFace face, double defaultValue) {
    }

    /** A signal output: written 0..1 by Machine.setSignal(name, v); high impedance until first written. */
    public record SignalOut(String name, RelativeFace face) {
    }

    /**
     * A first-order heat model: heat capacity (J/K), loss to ambient (W/K) and the temperature that trips the
     * machine (degrees C). The time constant is heatCapacityJPerK / heatLossWPerK seconds. Heat in: a consumer's
     * idle watts and the delivered energy its buffer couldn't hold, a producer's droop loss, and Machine.heatJoules.
     */
    public record Thermal(double heatCapacityJPerK, double heatLossWPerK, double overheatCelsius) {
    }

    /** The ambient temperature the heat model starts at and cools to, degrees C. */
    public static final double AMBIENT_CELSIUS = 20;
    /** An overheat trip resets only this far (K) below its trip point (or halfway to ambient, if that is nearer). */
    public static final double OVERHEAT_RESET_KELVIN = 10;
    /** The shortest trip cooldown build() accepts, s: shorter lets a trip chatter. */
    public static final double MIN_TRIP_COOLDOWN_SECONDS = 0.5;
    /** The highest undervoltage floor build() accepts: a line always sags a little under load. */
    public static final double MAX_MIN_VOLTAGE_RATIO = 0.95;

    private final Kind kind;
    private final double nominalVolts, ratedWatts, idleWatts, minVoltageRatio, tripCooldownSeconds;
    private final boolean burnsOnOvervoltage;
    private final Brownout brownout;
    private final Thermal thermal;
    private final Set<RelativeFace> powerFaces;
    private final List<SignalIn> signalsIn;
    private final List<SignalOut> signalsOut;
    private final RelativeFace busFace;

    private MachineSpec(Builder b, Set<RelativeFace> powerFaces) {
        kind = b.kind;
        nominalVolts = b.nominalVolts;
        ratedWatts = b.ratedWatts;
        idleWatts = b.idleWatts;
        minVoltageRatio = b.brownout == Brownout.SLOW ? 0 : b.minVoltageRatio;
        brownout = b.brownout;
        tripCooldownSeconds = b.tripCooldownSeconds;
        burnsOnOvervoltage = b.burnsOnOvervoltage;
        thermal = b.heatSet ? new Thermal(b.heatCapacityJPerK, b.heatLossWPerK, b.overheatCelsius) : null;
        this.powerFaces = Collections.unmodifiableSet(EnumSet.copyOf(powerFaces));
        signalsIn = List.copyOf(b.signalsIn);
        signalsOut = List.copyOf(b.signalsOut);
        busFace = b.busFace;
    }

    /** A machine that draws power at one of EA's cable voltages. ratedWatts is required. */
    public static Builder consumer(Tier tier) {
        return consumer(Objects.requireNonNull(tier, "tier").volts());
    }

    /** A machine that draws power at nominalVolts (positive, finite). ratedWatts is required. */
    public static Builder consumer(double nominalVolts) {
        return new Builder(Kind.CONSUMER, nominalVolts);
    }

    /** A machine that feeds power into the line at one of EA's cable voltages. ratedWatts is required. */
    public static Builder producer(Tier tier) {
        return producer(Objects.requireNonNull(tier, "tier").volts());
    }

    /** A machine that feeds power into the line at nominalVolts (positive, finite). ratedWatts is required. */
    public static Builder producer(double nominalVolts) {
        return new Builder(Kind.PRODUCER, nominalVolts);
    }

    public Kind kind() {
        return kind;
    }

    /** The voltage the machine is built for, V. */
    public double nominalVolts() {
        return nominalVolts;
    }

    /** Consumer: the most it draws at nominal volts, idle included (W). Producer: what it feeds at nominal volts (W). */
    public double ratedWatts() {
        return ratedWatts;
    }

    /** Drawn whenever powered and not tripped, W (consumers only; 0 for producers). */
    public double idleWatts() {
        return idleWatts;
    }

    /** Below this share of nominal volts the machine is UNDERVOLTAGE; 0 with Brownout.SLOW (no floor). */
    public double minVoltageRatio() {
        return minVoltageRatio;
    }

    /** What the machine does when its line sags; STOP for producers. */
    public Brownout brownout() {
        return brownout;
    }

    /** How long a trip waits, once its cause is gone, before it resets, s. */
    public double tripCooldownSeconds() {
        return tripCooldownSeconds;
    }

    /** Overvoltage destroys the machine instead of tripping it. */
    public boolean burnsOnOvervoltage() {
        return burnsOnOvervoltage;
    }

    /** The heat model, if the spec has one. */
    public Optional<Thermal> thermal() {
        return Optional.ofNullable(thermal);
    }

    /** The faces power cables connect to. */
    public Set<RelativeFace> powerFaces() {
        return powerFaces;
    }

    public List<SignalIn> signalsIn() {
        return signalsIn;
    }

    public List<SignalOut> signalsOut() {
        return signalsOut;
    }

    /** The face with the field bus port, if any. */
    public Optional<RelativeFace> busFace() {
        return Optional.ofNullable(busFace);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %.0f V %.0f W", kind.name().toLowerCase(Locale.ROOT), nominalVolts, ratedWatts);
    }

    /** Collects a spec's settings; build() checks them. Not reusable after build(). */
    public static final class Builder {
        private final Kind kind;
        private final double nominalVolts;
        private double ratedWatts, idleWatts = 0, minVoltageRatio = 0.75, tripCooldownSeconds = 3;
        private double heatCapacityJPerK = Double.NaN, heatLossWPerK = Double.NaN, overheatCelsius = Double.NaN;
        private boolean ratedSet, idleSet, minSet, heatSet, burnsOnOvervoltage;
        private Brownout brownout = Brownout.STOP;
        private boolean brownoutSet;
        private Set<RelativeFace> power;
        private boolean powerTwice, busTwice;
        private final List<SignalIn> signalsIn = new ArrayList<>();
        private final List<SignalOut> signalsOut = new ArrayList<>();
        private RelativeFace busFace;

        private Builder(Kind kind, double nominalVolts) {
            this.kind = kind;
            this.nominalVolts = nominalVolts;
        }

        /** Consumer: the most it draws at nominal volts, idle included. Producer: its output at nominal volts. W, required. */
        public Builder ratedWatts(double watts) {
            ratedWatts = watts;
            ratedSet = true;
            return this;
        }

        /** Drawn whenever powered, W; default 0. Consumers only, and less than ratedWatts. */
        public Builder idleWatts(double watts) {
            idleWatts = watts;
            idleSet = true;
            return this;
        }

        /** The undervoltage floor as a share of nominal volts, above 0 and at most 0.95; default 0.75. */
        public Builder minVoltageRatio(double ratio) {
            minVoltageRatio = ratio;
            minSet = true;
            return this;
        }

        /** What a consumer does when its line sags: Brownout.STOP (the default) or Brownout.SLOW. */
        public Builder onBrownout(Brownout b) {
            brownout = Objects.requireNonNull(b, "brownout");
            brownoutSet = true;
            return this;
        }

        /** How long a trip waits after its cause is gone, s, at least 0.5; default 3. */
        public Builder tripCooldownSeconds(double seconds) {
            tripCooldownSeconds = seconds;
            return this;
        }

        /** Overvoltage destroys the machine instead of tripping it (EA's own devices burn). Off by default. */
        public Builder burnsOnOvervoltage() {
            burnsOnOvervoltage = true;
            return this;
        }

        /**
         * The heat model's heat capacity, J/K. The heat model (see Thermal) needs this, heatLossWPerK and
         * overheatCelsius together; without it heatJoules is ignored and celsius() reads ambient.
         */
        public Builder heatCapacityJPerK(double jPerK) {
            heatCapacityJPerK = jPerK;
            heatSet = true;
            return this;
        }

        /** The heat model's loss to ambient, W/K (see heatCapacityJPerK). */
        public Builder heatLossWPerK(double wPerK) {
            heatLossWPerK = wPerK;
            heatSet = true;
            return this;
        }

        /** The temperature that trips OVERHEAT_TRIPPED, degrees C, above 30 (see heatCapacityJPerK). */
        public Builder overheatCelsius(double celsius) {
            overheatCelsius = celsius;
            heatSet = true;
            return this;
        }

        /** The faces power connects to. Default: every face not given a signal or the bus. */
        public Builder power(RelativeFace... faces) {
            Objects.requireNonNull(faces, "faces");
            if (power != null) powerTwice = true;
            power = EnumSet.noneOf(RelativeFace.class);
            for (RelativeFace f : faces) power.add(Objects.requireNonNull(f, "face"));
            return this;
        }

        /** A signal input that reads 0 when nothing drives it. */
        public Builder signalIn(String name, RelativeFace face) {
            return signalIn(name, face, 0);
        }

        /** A signal input that reads defaultValue (0..1) when nothing drives it. */
        public Builder signalIn(String name, RelativeFace face, double defaultValue) {
            signalsIn.add(new SignalIn(name, Objects.requireNonNull(face, "face"), defaultValue));
            return this;
        }

        /** A signal output, high impedance until first written. */
        public Builder signalOut(String name, RelativeFace face) {
            signalsOut.add(new SignalOut(name, Objects.requireNonNull(face, "face")));
            return this;
        }

        /** The face with a field bus port (one per machine). */
        public Builder bus(RelativeFace face) {
            if (busFace != null) busTwice = true;
            busFace = Objects.requireNonNull(face, "face");
            return this;
        }

        /** Checks every setting; throws IllegalArgumentException listing every problem found. */
        public MachineSpec build() {
            List<String> errors = new ArrayList<>();
            if (!(Double.isFinite(nominalVolts) && nominalVolts > 0))
                errors.add("nominal volts must be positive and finite, got " + nominalVolts);
            if (!ratedSet) errors.add("ratedWatts is required");
            else if (!(Double.isFinite(ratedWatts) && ratedWatts > 0))
                errors.add("ratedWatts must be positive and finite, got " + ratedWatts);
            boolean ratedOk = Double.isFinite(ratedWatts) && ratedWatts > 0;
            if (kind == Kind.PRODUCER && idleSet)
                errors.add("idleWatts is for consumers; a producer has no idle draw");
            if (!(Double.isFinite(idleWatts) && idleWatts >= 0))
                errors.add("idleWatts must be 0 or more and finite, got " + idleWatts);
            else if (ratedOk && idleWatts >= ratedWatts)
                errors.add("idleWatts (" + idleWatts + ") must be less than ratedWatts (" + ratedWatts + ")");
            if (kind == Kind.PRODUCER && brownoutSet)
                errors.add("onBrownout is for consumers; a producer's output only stops on a trip");
            if (brownout == Brownout.SLOW && minSet)
                errors.add("minVoltageRatio is the floor of Brownout.STOP; Brownout.SLOW has none");
            if (!(minVoltageRatio > 0 && minVoltageRatio <= MAX_MIN_VOLTAGE_RATIO))
                errors.add("minVoltageRatio must be above 0 and at most " + MAX_MIN_VOLTAGE_RATIO
                    + " (a line sags under load), got " + minVoltageRatio);
            if (!(Double.isFinite(tripCooldownSeconds) && tripCooldownSeconds >= MIN_TRIP_COOLDOWN_SECONDS))
                errors.add("tripCooldownSeconds must be at least " + MIN_TRIP_COOLDOWN_SECONDS
                    + " s (shorter lets a trip chatter), got " + tripCooldownSeconds);
            if (heatSet) {
                if (!(Double.isFinite(heatCapacityJPerK) && heatCapacityJPerK > 0))
                    errors.add("heatCapacityJPerK must be positive and finite" + got(heatCapacityJPerK));
                if (!(Double.isFinite(heatLossWPerK) && heatLossWPerK > 0))
                    errors.add("heatLossWPerK must be positive and finite" + got(heatLossWPerK));
                if (!(Double.isFinite(overheatCelsius) && overheatCelsius > AMBIENT_CELSIUS + OVERHEAT_RESET_KELVIN))
                    errors.add("overheatCelsius must be above " + (AMBIENT_CELSIUS + OVERHEAT_RESET_KELVIN)
                        + " C (ambient " + AMBIENT_CELSIUS + " C plus the " + OVERHEAT_RESET_KELVIN
                        + " K reset band)" + got(overheatCelsius));
            }

            // signal names, then one role per face
            Set<String> names = new HashSet<>();
            Map<RelativeFace, String> roles = new EnumMap<>(RelativeFace.class);
            for (SignalIn s : signalsIn) {
                checkSignal(s.name(), names, errors);
                if (!(s.defaultValue() >= 0 && s.defaultValue() <= 1))
                    errors.add("signal '" + s.name() + "' default must be 0..1 (a signal level, not volts), got "
                        + s.defaultValue());
                claim(roles, s.face(), "signal '" + s.name() + "'", errors);
            }
            for (SignalOut s : signalsOut) {
                checkSignal(s.name(), names, errors);
                claim(roles, s.face(), "signal '" + s.name() + "'", errors);
            }
            if (busTwice) errors.add("bus is given twice; a machine has one bus port");
            if (busFace != null) claim(roles, busFace, "the bus", errors);
            Set<RelativeFace> powerFaces;
            if (power != null) {
                if (powerTwice) errors.add("power faces are given twice");
                if (power.isEmpty()) errors.add("power needs at least one face");
                for (RelativeFace f : power) claim(roles, f, "power", errors);
                powerFaces = power;
            } else {
                powerFaces = EnumSet.allOf(RelativeFace.class);
                powerFaces.removeAll(roles.keySet());
                if (powerFaces.isEmpty())
                    errors.add("no face is left for power: signals and the bus take all six");
            }
            if (!errors.isEmpty())
                throw new IllegalArgumentException("Invalid " + kind.name().toLowerCase(Locale.ROOT) + " spec: "
                    + String.join("; ", errors));
            return new MachineSpec(this, powerFaces);
        }

        /** ", got x", or " (not set)" for a heat setting left out. */
        private static String got(double v) {
            return Double.isNaN(v) ? " (not set)" : ", got " + v;
        }

        private static void checkSignal(String name, Set<String> names, List<String> errors) {
            if (name == null || name.isBlank()) errors.add("a signal needs a name");
            else if (!names.add(name)) errors.add("signal name '" + name + "' is used twice");
        }

        private static void claim(Map<RelativeFace, String> roles, RelativeFace face, String role, List<String> errors) {
            String had = roles.putIfAbsent(face, role);
            if (had != null) errors.add("face " + face + " is given to both " + had + " and " + role);
        }
    }
}
