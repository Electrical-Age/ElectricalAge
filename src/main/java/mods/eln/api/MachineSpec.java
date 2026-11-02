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
 * What a machine is, electrically: a consumer or producer at a tier, its power ratings, protections and which face
 * does what. Immutable; made by consumer(tier) or producer(tier), then the builder's build(), which checks every
 * setting and lists all problems in one IllegalArgumentException. Baughn: a mod hands over a nominal machine spec
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
     * machine (degrees C). The time constant is heatCapacityJperK / lossWperK seconds.
     */
    public record Thermal(double heatCapacityJperK, double lossWperK, double tripCelsius) {
    }

    /** The ambient temperature the heat model starts at and cools to, degrees C. */
    public static final double AMBIENT_CELSIUS = 20;
    /** An overheat trip resets only this far (K) below its trip point. */
    public static final double OVERHEAT_RESET_KELVIN = 10;
    /** The shortest trip cooldown build() accepts, s: shorter lets a trip chatter. */
    public static final double MIN_TRIP_COOLDOWN_SECONDS = 0.5;

    private final Kind kind;
    private final Tier tier;
    private final double ratedWatts, idleWatts, minVoltageRatio, maxWatts, tripCooldownSeconds;
    private final boolean burnsOnOvervoltage;
    private final Thermal thermal;
    private final Set<RelativeFace> powerFaces;
    private final List<SignalIn> signalsIn;
    private final List<SignalOut> signalsOut;
    private final RelativeFace busFace;

    private MachineSpec(Builder b, Set<RelativeFace> powerFaces) {
        kind = b.kind;
        tier = b.tier;
        ratedWatts = b.ratedWatts;
        idleWatts = b.idleWatts;
        minVoltageRatio = b.minVoltageRatio;
        maxWatts = b.maxSet ? b.maxWatts : 1.25 * b.ratedWatts;
        tripCooldownSeconds = b.tripCooldownSeconds;
        burnsOnOvervoltage = b.burnsOnOvervoltage;
        thermal = b.thermal;
        this.powerFaces = Collections.unmodifiableSet(EnumSet.copyOf(powerFaces));
        signalsIn = List.copyOf(b.signalsIn);
        signalsOut = List.copyOf(b.signalsOut);
        busFace = b.busFace;
    }

    /** A machine that draws power at the given tier. ratedWatts is required. */
    public static Builder consumer(Tier tier) {
        return new Builder(Kind.CONSUMER, Objects.requireNonNull(tier, "tier"));
    }

    /** A machine that feeds power into the line at the given tier. ratedWatts is required. */
    public static Builder producer(Tier tier) {
        return new Builder(Kind.PRODUCER, Objects.requireNonNull(tier, "tier"));
    }

    public Kind kind() {
        return kind;
    }

    public Tier tier() {
        return tier;
    }

    /** The tier's nominal voltage, V. */
    public double nominalVolts() {
        return tier.volts();
    }

    /** Consumer: the most it draws at nominal volts, idle included (W). Producer: what it feeds at nominal volts (W). */
    public double ratedWatts() {
        return ratedWatts;
    }

    /** Drawn whenever powered and not tripped, W (consumers only; 0 for producers). */
    public double idleWatts() {
        return idleWatts;
    }

    /** Below this share of nominal volts the machine is UNDERVOLTAGE. */
    public double minVoltageRatio() {
        return minVoltageRatio;
    }

    /** Averaged power above this trips OVERPOWER_TRIPPED, W. */
    public double maxWatts() {
        return maxWatts;
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
        return String.format(Locale.ROOT, "%s %s %.0f W", kind.name().toLowerCase(Locale.ROOT), tier, ratedWatts);
    }

    /** Collects a spec's settings; build() checks them. Not reusable after build(). */
    public static final class Builder {
        private final Kind kind;
        private final Tier tier;
        private double ratedWatts = Double.NaN, idleWatts = 0, minVoltageRatio = 0.75, maxWatts = Double.NaN;
        private double tripCooldownSeconds = 3;
        private boolean idleSet, maxSet, burnsOnOvervoltage;
        private Thermal thermal;
        private Set<RelativeFace> power;
        private boolean powerTwice, busTwice;
        private final List<SignalIn> signalsIn = new ArrayList<>();
        private final List<SignalOut> signalsOut = new ArrayList<>();
        private RelativeFace busFace;

        private Builder(Kind kind, Tier tier) {
            this.kind = kind;
            this.tier = tier;
        }

        /** Consumer: the most it draws at nominal volts, idle included. Producer: its output at nominal volts. W, required. */
        public Builder ratedWatts(double watts) {
            ratedWatts = watts;
            return this;
        }

        /** Drawn whenever powered, W; default 0. Consumers only, and less than ratedWatts. */
        public Builder idleWatts(double watts) {
            idleWatts = watts;
            idleSet = true;
            return this;
        }

        /** The undervoltage floor as a share of nominal volts, between 0 and 1; default 0.75. */
        public Builder minVoltageRatio(double ratio) {
            minVoltageRatio = ratio;
            return this;
        }

        /** The overpower trip, W, above ratedWatts; default 1.25 x ratedWatts. */
        public Builder maxWatts(double watts) {
            maxWatts = watts;
            maxSet = true;
            return this;
        }

        /** How long a trip waits after its cause is gone, s, at least 0.5; default 3. */
        public Builder tripCooldown(double seconds) {
            tripCooldownSeconds = seconds;
            return this;
        }

        /** Overvoltage destroys the machine instead of tripping it (EA's own devices burn). Off by default. */
        public Builder burnsOnOvervoltage() {
            burnsOnOvervoltage = true;
            return this;
        }

        /** Adds the heat model (see Thermal); without it heatJoules is ignored and celsius() reads ambient. */
        public Builder thermal(double heatCapacityJperK, double lossWperK, double tripCelsius) {
            thermal = new Thermal(heatCapacityJperK, lossWperK, tripCelsius);
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
            if (Double.isNaN(ratedWatts)) errors.add("ratedWatts is required");
            else if (!(Double.isFinite(ratedWatts) && ratedWatts > 0))
                errors.add("ratedWatts must be positive and finite, got " + ratedWatts);
            boolean ratedOk = Double.isFinite(ratedWatts) && ratedWatts > 0;
            if (kind == Kind.PRODUCER && idleSet)
                errors.add("idleWatts is for consumers; a producer has no idle draw");
            if (!(Double.isFinite(idleWatts) && idleWatts >= 0))
                errors.add("idleWatts must be 0 or more and finite, got " + idleWatts);
            else if (ratedOk && idleWatts >= ratedWatts)
                errors.add("idleWatts (" + idleWatts + ") must be less than ratedWatts (" + ratedWatts + ")");
            if (!(minVoltageRatio > 0 && minVoltageRatio < 1))
                errors.add("minVoltageRatio must be between 0 and 1 (exclusive), got " + minVoltageRatio);
            if (maxSet) {
                if (!Double.isFinite(maxWatts)) errors.add("maxWatts must be finite, got " + maxWatts);
                else if (ratedOk && maxWatts <= ratedWatts)
                    errors.add("maxWatts (" + maxWatts + ") must be more than ratedWatts (" + ratedWatts
                        + "), or the machine trips at its rating");
            }
            if (!(Double.isFinite(tripCooldownSeconds) && tripCooldownSeconds >= MIN_TRIP_COOLDOWN_SECONDS))
                errors.add("tripCooldown must be at least " + MIN_TRIP_COOLDOWN_SECONDS
                    + " s (shorter lets a trip chatter), got " + tripCooldownSeconds);
            if (thermal != null) {
                if (!(Double.isFinite(thermal.heatCapacityJperK()) && thermal.heatCapacityJperK() > 0))
                    errors.add("thermal heatCapacityJperK must be positive and finite, got " + thermal.heatCapacityJperK());
                if (!(Double.isFinite(thermal.lossWperK()) && thermal.lossWperK() > 0))
                    errors.add("thermal lossWperK must be positive and finite, got " + thermal.lossWperK());
                if (!(Double.isFinite(thermal.tripCelsius())
                    && thermal.tripCelsius() > AMBIENT_CELSIUS + OVERHEAT_RESET_KELVIN))
                    errors.add("thermal tripCelsius must be above " + (AMBIENT_CELSIUS + OVERHEAT_RESET_KELVIN)
                        + " C (ambient " + AMBIENT_CELSIUS + " C plus the " + OVERHEAT_RESET_KELVIN
                        + " K reset band), got " + thermal.tripCelsius());
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
