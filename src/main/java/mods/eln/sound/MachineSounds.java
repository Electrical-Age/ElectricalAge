package mods.eln.sound;

import mods.eln.misc.Coordonate;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * The sound of a spinning electric machine (motor, generator, turbine-generator) in the three layers a real one has,
 * driven apart (jrddunbr's recordings of a blower motor starting and of an inverter locomotive):
 * an electrical hum whose pitch never moves and whose loudness follows the current; rotor tones whose pitch follows
 * the speed, so a machine running up chirps; and the air it moves. Renders add the loops with addLoopedSound.
 */
public final class MachineSounds {
    private MachineSounds() {
    }

    /** Pitch of the rotor tones at nominal speed: room above and below inside the engine's 0.5-2 pitch range. */
    static final float ROTOR_PITCH = 1.6f;
    static final float HUM_VOLUME = 0.16f, ROTOR_VOLUME = 0.12f, AIR_VOLUME = 0.15f;

    /**
     * speed: rotor speed over nominal; load: current over nominal (0..1); pitch: a bigger, slower machine below 1;
     * waver: a cheap rotor's wobble in pitch (fraction); volume: the machine's overall loudness.
     */
    public static List<LoopedSound> electric(Coordonate at, DoubleSupplier speed, DoubleSupplier load, float pitch,
                                             float waver, float volume) {
        LoopedSound hum = new LoopedSound("eln:electric_hum", at) {
            @Override
            public float getVolume() {
                return volume * HUM_VOLUME * clamp(load.getAsDouble(), 0, 1);
            }
        };
        LoopedSound rotor = new LoopedSound("eln:rotor_tone", at) {
            @Override
            public float getVolume() {
                // (below ~30% speed the tones would sit at the engine's lowest pitch: they fade in above it)
                return volume * ROTOR_VOLUME * clamp((speed.getAsDouble() - 0.15) / 0.3, 0, 1);
            }

            @Override
            public float getPitch() {
                double w = waver == 0 ? 1 : 1 + waver * Math.sin(System.nanoTime() * 1e-9 * 2 * Math.PI * 2.7);
                return clamp(pitch * ROTOR_PITCH * speed.getAsDouble() * w, 0.5, 2);
            }
        };
        LoopedSound air = new LoopedSound("eln:rotor_air", at) {
            @Override
            public float getVolume() {
                double s = clamp(speed.getAsDouble(), 0, 1.5);
                return volume * AIR_VOLUME * (float) (s * s);
            }

            @Override
            public float getPitch() {
                return clamp(pitch * (0.6 + 0.3 * speed.getAsDouble()), 0.5, 2);
            }
        };
        return List.of(hum, rotor, air);
    }

    /** Loudness of the turbines' layers: the flow (steam hiss or gas roar), the rotor tones, the overspeed whistle. */
    static final float FLOW_VOLUME = 0.35f, TURBINE_ROTOR_VOLUME = 0.1f, WHISTLE_VOLUME = 0.3f;

    /**
     * A turbine (shaft steam or gas turbine): its flow by flow (0..1: steam hiss, or a gas turbine's roar whose pitch
     * also follows speed), rotor tones by speed, and over 105 % of its rated speed a whistle rising with the overspeed
     * (Baughn 2026-10-29: hear a runaway coming). speed: shaft speed over rated.
     */
    public static List<LoopedSound> turbine(Coordonate at, DoubleSupplier speed, DoubleSupplier flow, boolean gas, float volume) {
        LoopedSound flowing = new LoopedSound(gas ? "eln:gas_turbine_roar" : "eln:steam_hiss", at) {
            @Override
            public float getVolume() {
                double f = clamp(flow.getAsDouble(), 0, 1);
                return volume * FLOW_VOLUME * (float) (gas ? Math.sqrt(f) * clamp(speed.getAsDouble() * 1.5, 0, 1) : f);
            }

            @Override
            public float getPitch() {
                return gas ? clamp(0.6 + 0.5 * speed.getAsDouble(), 0.5, 2) : 1f;
            }
        };
        LoopedSound rotor = new LoopedSound("eln:rotor_tone", at) {
            @Override
            public float getVolume() {
                return volume * TURBINE_ROTOR_VOLUME * clamp((speed.getAsDouble() - 0.15) / 0.3, 0, 1);
            }

            @Override
            public float getPitch() {
                return clamp(ROTOR_PITCH * speed.getAsDouble(), 0.5, 2);
            }
        };
        // (an alarm: not faded over the 16 blocks the machine's other layers are, so it's heard down the line)
        LoopedSound whistle = new LoopedSound("eln:overspeed_whistle", at, LoopedSound.Attenuation.NONE) {
            @Override
            public float getVolume() {
                return volume * whistle(speed.getAsDouble());
            }

            @Override
            public float getPitch() {
                return clamp(speed.getAsDouble(), 0.5, 2);
            }
        };
        return List.of(flowing, rotor, whistle);
    }

    /** The overspeed whistle's loudness at a speed (share of rated): from 105 %, full at 110 % (the trip). */
    public static float whistle(double speed) {
        return WHISTLE_VOLUME * clamp((speed - 1.05) / 0.05, 0, 1);
    }

    static float clamp(double v, double lo, double hi) {
        return (float) Math.max(lo, Math.min(hi, v));
    }
}
