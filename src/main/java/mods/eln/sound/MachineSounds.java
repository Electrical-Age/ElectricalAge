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

    static float clamp(double v, double lo, double hi) {
        return (float) Math.max(lo, Math.min(hi, v));
    }
}
