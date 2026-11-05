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

    /**
     * A signal input: read 0..1 by Machine.signal(name); defaultValue (0..1) when nothing is connected. Its cable
     * meets face at position (ANY: anywhere on it).
     */
    public record SignalIn(String name, RelativeFace face, CablePosition position, double defaultValue) {
    }

    /** A signal output: written 0..1 by Machine.setSignal(name, v); high impedance until first written. */
    public record SignalOut(String name, RelativeFace face, CablePosition position) {
    }

    /**
     * A first-order heat model: heat capacity (J/K), loss to ambient (W/K), the trip point and how far under it (K) a
     * trip resets. The time constant is heatCapacityJPerK / heatLossWPerK seconds. Heat in: a consumer's idle watts and
     * a producer's droop loss, and Machine.heatJoules. It starts at and
     * cools to the ambient where the machine stands (EA's Ambient, by biome and depth, for an element: the Nether is
     * 90 C; the bench's setting on a Bench).
     * <p>
     * The trip point is either absolute (overheatKelvin is a temperature, K: a machine that trips at 300 C does so
     * whatever its ambient) or relative (overheatKelvin is a rise over the ambient, K: it runs in a hot place as in a
     * cool one). The builder makes its user choose (Baughn 2026-11-04: neither is the default).
     */
    public record Thermal(double heatCapacityJPerK, double heatLossWPerK, double overheatKelvin, boolean overheatRelative,
                          double overheatResetKelvin) {
        /** The temperature that trips it, degrees C, where the ambient is ambientCelsius. */
        public double overheatCelsius(double ambientCelsius) {
            return overheatRelative ? ambientCelsius + overheatKelvin : overheatKelvin - KELVIN_AT_0_CELSIUS;
        }
    }

    /** 0 C in K, for the Celsius variants of the trip point. */
    public static final double KELVIN_AT_0_CELSIUS = 273.15;

    /** The nominal ambient, degrees C: a machine's temperature before it knows its own (and a Bench's default). */
    public static final double AMBIENT_CELSIUS = 20;
    /**
     * By default an overheat trip resets only this far (K) below its trip point (or halfway down to ambient, if that is
     * nearer); see Builder.overheatResetKelvin.
     */
    public static final double OVERHEAT_RESET_KELVIN = 10;
    /** The shortest trip cooldown build() accepts, s: shorter lets a trip chatter. */
    public static final double MIN_TRIP_COOLDOWN_SECONDS = 0.5;
    /** The highest undervoltage floor build() accepts: a line always sags a little under load. */
    public static final double MAX_MIN_VOLTAGE_RATIO = 0.95;

    private final Kind kind;
    private final double nominalVolts, ratedWatts, idleWatts, minVoltageRatio, tripCooldownSeconds;
    private final boolean burnsOnOvervoltage, jadeLines;
    private final BurnHandler burnHandler;
    private final Brownout brownout;
    private final Thermal thermal;
    private final Set<RelativeFace> powerFaces;
    private final List<SignalIn> signalsIn;
    private final List<SignalOut> signalsOut;
    private final RelativeFace busFace;
    private final CablePosition busPosition;

    private MachineSpec(Builder b, Set<RelativeFace> powerFaces) {
        kind = b.kind;
        nominalVolts = b.nominalVolts;
        ratedWatts = b.ratedWatts;
        idleWatts = b.idleWatts;
        minVoltageRatio = b.brownout == Brownout.SLOW ? 0 : b.minVoltageRatio;
        brownout = b.brownout;
        tripCooldownSeconds = b.tripCooldownSeconds;
        burnsOnOvervoltage = b.burnHandler != null;
        jadeLines = b.jadeLines;
        burnHandler = b.burnHandler;
        thermal = b.heatSet ? new Thermal(b.heatCapacityJPerK, b.heatLossWPerK, b.overheatKelvin, b.overheatRelative, b.overheatResetKelvin) : null;
        this.powerFaces = Collections.unmodifiableSet(EnumSet.copyOf(powerFaces));
        signalsIn = List.copyOf(b.signalsIn);
        signalsOut = List.copyOf(b.signalsOut);
        busFace = b.busFace;
        busPosition = b.busPosition;
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

    /**
     * Overvoltage burns the machine instead of tripping it (burnsOnOvervoltage(handler)): it stays OVERVOLTAGE_TRIPPED
     * for good (an EA element until repaired).
     */
    public boolean burnsOnOvervoltage() {
        return burnsOnOvervoltage;
    }

    /** Does EA add its Jade lines at the machine's block (Builder.jadeLines). */
    public boolean jadeLines() {
        return jadeLines;
    }

    /** What a burn does to the machine's block (burnsOnOvervoltage(handler)); present exactly when it burns. */
    public Optional<BurnHandler> burnHandler() {
        return Optional.ofNullable(burnHandler);
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

    /** Where on its face the bus cable meets it (ANY: anywhere). */
    public CablePosition busPosition() {
        return busPosition;
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
        private double heatCapacityJPerK = Double.NaN, heatLossWPerK = Double.NaN, overheatKelvin = Double.NaN;
        private boolean overheatRelative;
        private int overheatSet;
        private double overheatResetKelvin = OVERHEAT_RESET_KELVIN;
        private boolean ratedSet, idleSet, minSet, heatSet, resetSet, burnTwice;
        private BurnHandler burnHandler;
        private Brownout brownout = Brownout.STOP;
        private boolean brownoutSet, jadeLines = true;
        private Set<RelativeFace> power;
        private boolean powerTwice, busTwice;
        private final List<SignalIn> signalsIn = new ArrayList<>();
        private final List<SignalOut> signalsOut = new ArrayList<>();
        private RelativeFace busFace;
        private CablePosition busPosition = CablePosition.ANY;

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

        /**
         * Whether EA adds its Jade lines (voltage, power, state, buffer, temperature) at the machine's block; default
         * true. Give false when your mod shows its own (Baughn 2026-11-04). An EA element machine's lines are always
         * its element's to place (ElementMachine.wailaLines).
         */
        public Builder jadeLines(boolean show) {
            jadeLines = show;
            return this;
        }

        /** How long a trip waits after its cause is gone, s, at least 0.5; default 3. */
        public Builder tripCooldownSeconds(double seconds) {
            tripCooldownSeconds = seconds;
            return this;
        }

        /**
         * Overvoltage burns the machine instead of tripping it. It stays OVERVOLTAGE_TRIPPED for good (saved so), and
         * handler is called once, on the server thread, to do what a burn means for the block: break it, drop it, set
         * it on fire, or nothing. Baughn: "a callback. Let the API user choose how to handle it". There is no burn
         * without a handler, so a machine at another mod's block entity can't burn with nothing to say what that does
         * (EA's own elements pass EA's burn-out, which they alone accept). Off by default.
         */
        public Builder burnsOnOvervoltage(BurnHandler handler) {
            if (burnHandler != null) burnTwice = true;
            burnHandler = Objects.requireNonNull(handler, "handler");
            return this;
        }

        /**
         * The heat model's heat capacity, J/K. The heat model (see Thermal) needs this, heatLossWPerK and one trip point
         * (overheatAbsoluteKelvin or overheatRiseKelvin, or their Celsius variants) together; without it heatJoules is
         * ignored and celsius() reads ambient.
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

        /**
         * The temperature (K) at which the machine trips OVERHEAT_TRIPPED, whatever its ambient: 573.15 trips at 300 C,
         * in a cool place or in the Nether alike, and an ambient over it keeps it tripped. Give this or
         * overheatRiseKelvin (or a Celsius variant), once; see heatCapacityJPerK.
         */
        public Builder overheatAbsoluteKelvin(double kelvin) {
            return overheat(kelvin, false);
        }

        /** overheatAbsoluteKelvin in degrees C: overheatAbsoluteCelsius(300) trips at 300 C. */
        public Builder overheatAbsoluteCelsius(double celsius) {
            return overheat(celsius + KELVIN_AT_0_CELSIUS, false);
        }

        /**
         * How far over its ambient (K) the machine trips OVERHEAT_TRIPPED: at 20 C ambient a rise of 80 trips at
         * 100 C, in the Nether (90 C) at 170 C. More than the reset band. Give this or overheatAbsoluteKelvin (or a
         * Celsius variant), once; see heatCapacityJPerK.
         */
        public Builder overheatRiseKelvin(double kelvin) {
            return overheat(kelvin, true);
        }

        /** overheatRiseKelvin in degrees C (the same number: a rise of 1 C is 1 K). */
        public Builder overheatRiseCelsius(double celsius) {
            return overheat(celsius, true);
        }

        private Builder overheat(double kelvin, boolean relative) {
            overheatKelvin = kelvin;
            overheatRelative = relative;
            overheatSet++;
            heatSet = true;
            return this;
        }

        /**
         * How far (K, positive) under the trip point an overheat trip resets, after its cooldown; default
         * OVERHEAT_RESET_KELVIN (10), or halfway down to ambient if that is nearer. Part of the heat model: needs
         * heatCapacityJPerK and the rest.
         */
        public Builder overheatResetKelvin(double kelvin) {
            overheatResetKelvin = kelvin;
            resetSet = true;
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

        /** A signal input on the whole of face that reads 0 when nothing is connected. */
        public Builder signalIn(String name, RelativeFace face) {
            return signalIn(name, face, CablePosition.ANY, 0);
        }

        /** A signal input on the whole of face that reads defaultValue (0..1) when nothing is connected. */
        public Builder signalIn(String name, RelativeFace face, double defaultValue) {
            return signalIn(name, face, CablePosition.ANY, defaultValue);
        }

        /**
         * A signal input whose cable meets face at position (a face can hold several signals at different
         * positions); it reads defaultValue (0..1) when nothing is connected there.
         */
        public Builder signalIn(String name, RelativeFace face, CablePosition position, double defaultValue) {
            signalsIn.add(new SignalIn(name, Objects.requireNonNull(face, "face"),
                Objects.requireNonNull(position, "position"), defaultValue));
            return this;
        }

        /** A signal output on the whole of face, high impedance until first written. */
        public Builder signalOut(String name, RelativeFace face) {
            return signalOut(name, face, CablePosition.ANY);
        }

        /** A signal output whose cable meets face at position, high impedance until first written. */
        public Builder signalOut(String name, RelativeFace face, CablePosition position) {
            signalsOut.add(new SignalOut(name, Objects.requireNonNull(face, "face"), Objects.requireNonNull(position, "position")));
            return this;
        }

        /** The face with a field bus port (one per machine). */
        public Builder bus(RelativeFace face) {
            return bus(face, CablePosition.ANY);
        }

        /** The field bus port, where its cable meets face at position (one per machine; a voxel bus cable only). */
        public Builder bus(RelativeFace face, CablePosition position) {
            if (busFace != null) busTwice = true;
            busFace = Objects.requireNonNull(face, "face");
            busPosition = Objects.requireNonNull(position, "position");
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
                if (!(Double.isFinite(overheatResetKelvin) && overheatResetKelvin > 0))
                    errors.add("overheatResetKelvin must be positive and finite, got " + overheatResetKelvin);
                else if (overheatSet == 0)
                    errors.add("the heat model needs a trip point: overheatAbsoluteKelvin (a temperature) or overheatRiseKelvin "
                        + "(a rise over the ambient), or their Celsius variants");
                else if (overheatSet > 1)
                    errors.add("give one trip point: overheatAbsolute* or overheatRise*, once");
                else if (overheatRelative && !(Double.isFinite(overheatKelvin) && overheatKelvin > overheatResetKelvin))
                    errors.add("overheatRiseKelvin must be more than the " + overheatResetKelvin
                        + " K reset band (it is K over the ambient where the machine stands, not degrees C)" + got(overheatKelvin));
                else if (!overheatRelative && !(Double.isFinite(overheatKelvin) && overheatKelvin > KELVIN_AT_0_CELSIUS))
                    errors.add("overheatAbsoluteKelvin must be a temperature above 0 C (273.15 K; for degrees C use "
                        + "overheatAbsoluteCelsius)" + got(overheatKelvin));
            } else if (resetSet) errors.add("overheatResetKelvin is part of the heat model: set heatCapacityJPerK, "
                + "heatLossWPerK and a trip point too");

            // signal names, then one role per face (or per cable position on it)
            Set<String> names = new HashSet<>();
            Map<RelativeFace, Map<CablePosition, String>> roles = new EnumMap<>(RelativeFace.class);
            for (SignalIn s : signalsIn) {
                checkSignal(s.name(), names, errors);
                if (!(s.defaultValue() >= 0 && s.defaultValue() <= 1))
                    errors.add("signal '" + s.name() + "' default must be 0..1 (a signal level, not volts), got "
                        + s.defaultValue());
                claim(roles, s.face(), s.position(), "signal '" + s.name() + "'", errors);
            }
            for (SignalOut s : signalsOut) {
                checkSignal(s.name(), names, errors);
                claim(roles, s.face(), s.position(), "signal '" + s.name() + "'", errors);
            }
            if (busTwice) errors.add("bus is given twice; a machine has one bus port");
            if (burnTwice) errors.add("burnsOnOvervoltage is given twice; give it once");
            if (busFace != null) claim(roles, busFace, busPosition, "the bus", errors);
            Set<RelativeFace> powerFaces;
            if (power != null) {
                if (powerTwice) errors.add("power faces are given twice");
                if (power.isEmpty()) errors.add("power needs at least one face");
                for (RelativeFace f : power) claim(roles, f, CablePosition.ANY, "power", errors);
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

        /**
         * Claims face (at position) for role. A face is one role's, but for signals at different cable positions;
         * those only on the side faces.
         */
        private static void claim(Map<RelativeFace, Map<CablePosition, String>> roles, RelativeFace face, CablePosition position,
                                  String role, List<String> errors) {
            if (position != CablePosition.ANY && (face == RelativeFace.UP || face == RelativeFace.DOWN))
                errors.add(role + ": " + position + " is a cable position on a side face (FRONT, BACK, LEFT, RIGHT), not " + face);
            Map<CablePosition, String> at = roles.computeIfAbsent(face, f -> new EnumMap<>(CablePosition.class));
            for (Map.Entry<CablePosition, String> e : at.entrySet()) {
                if (e.getKey() == position && position != CablePosition.ANY)
                    errors.add("the " + position + " cable of face " + face + " is given to both " + e.getValue() + " and " + role);
                else if (e.getKey() == CablePosition.ANY || position == CablePosition.ANY)
                    errors.add("face " + face + " is given to both " + e.getValue() + " and " + role);
            }
            at.putIfAbsent(position, role);
        }
    }
}
