package mods.eln.anim;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import java.util.Random;
import java.util.Set;

/**
 * What a device lets out into the air, and how that looks: one effect per kind, so steam, smoke and flames are the same
 * on every device (Baughn 2026-11-13). The device says where (its outlet's centre and half width, and a sideways push
 * for an outlet in a side face) and how many puffs a second (an {@link Emitter}); the kind gives the particles and how
 * they rise. {@link #vented} and {@link #burnt} name the kind for a fluid.
 */
public enum Plume {
    /** Nothing to see (air). */
    NONE,
    /** Water vapour: calm white wisps. */
    STEAM,
    /** A clean fire's flue gas: grey smoke. */
    SMOKE,
    /** A fouled fire's smoke, carbon black: thick and dark. */
    SOOT,
    /** A fuel gas burning at an open tip: a tall clean flame, blue at its root. */
    FLAME_GAS,
    /** A light liquid fuel burning: a flame under wisps of smoke. */
    FLAME_LIGHT,
    /** A heavy oil burning: a low flame under black smoke, a column of it now and then. */
    FLAME_HEAVY;

    /** Fluids by name (the registry path, as FuelRegistry.fluidName; a species without a fluid by its key's path). */
    static final Set<String> WATER = Set.of("steam", "ic2steam", "water");
    static final Set<String> FLUE = Set.of("flue_gas");
    static final Set<String> GAS = Set.of("naturalgas", "syngas", "butadiene_gas");
    static final Set<String> HEAVY = Set.of("heavyoil", "residue", "crude_oil", "reservoir_oil", "creosote", "oil", "lava", "tar");

    static final Random RND = new Random();

    /** fluid let out without a flame: steam for water, smoke for flue gas; nothing to see of anything else. */
    public static Plume vented(String fluid) {
        if (WATER.contains(fluid)) return STEAM;
        if (FLUE.contains(fluid)) return SMOKE;
        return NONE;
    }

    /** fluid at an open flame (a flare's tip): what doesn't burn goes up as {@link #vented}, the rest by how it burns. */
    public static Plume burnt(String fluid) {
        if (fluid == null || fluid.isEmpty()) return NONE;
        Plume v = vented(fluid);
        if (v != NONE) return v;
        if (GAS.contains(fluid)) return FLAME_GAS;
        return HEAVY.contains(fluid) ? FLAME_HEAVY : FLAME_LIGHT;
    }

    /** Is there a flame in it. */
    public boolean flame() {
        return this == FLAME_GAS || this == FLAME_LIGHT || this == FLAME_HEAVY;
    }

    /** The particle of a jet of it under pressure (a leak, a relief valve: sent by the server). */
    public ParticleOptions jet() {
        return switch (this) {
            case SMOKE -> ParticleTypes.SMOKE;
            case SOOT -> ParticleTypes.LARGE_SMOKE;
            default -> ParticleTypes.CLOUD;
        };
    }

    /** One puff out of an upward outlet at (x, y, z), somewhere within halfWidth of its centre (client side). */
    public void puff(Level level, double x, double y, double z, double halfWidth) {
        puff(level, x, y, z, halfWidth, 0, 0);
    }

    /**
     * One puff at (x, y, z) (client side). An upward outlet (no push) spreads it within halfWidth of the centre; an
     * outlet in a side face spreads it across the face and gives it the push (pushX, pushZ: blocks a tick, out of the
     * face) before it rises.
     */
    public void puff(Level level, double x, double y, double z, double halfWidth, double pushX, double pushZ) {
        if (this == NONE) return;
        double j1 = (RND.nextDouble() - 0.5) * 2 * halfWidth, j2 = (RND.nextDouble() - 0.5) * 2 * halfWidth;
        double push = Math.hypot(pushX, pushZ), jx, jy, jz;
        if (push > 0) {   // (across the face: along it and up it)
            jx = -pushZ / push * j1;
            jy = j2;
            jz = pushX / push * j1;
        } else {
            jx = j1;
            jy = 0;
            jz = j2;
        }
        double px = x + jx, py = y + jy, pz = z + jz;
        switch (this) {
            case STEAM -> level.addParticle(ParticleTypes.WHITE_SMOKE, px, py, pz, pushX + jx * 0.2, 0.03 + RND.nextDouble() * 0.03, pushZ + jz * 0.2);
            case SMOKE -> level.addParticle(ParticleTypes.SMOKE, px, py, pz, pushX, 0.06, pushZ);
            case SOOT -> level.addParticle(ParticleTypes.LARGE_SMOKE, px, py, pz, pushX, 0.07, pushZ);
            case FLAME_GAS -> {
                level.addParticle(ParticleTypes.FLAME, px, py, pz, pushX, 0.10 + RND.nextDouble() * 0.06, pushZ);
                if (RND.nextInt(3) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + jx * 0.5, py, z + jz * 0.5, pushX, 0.05, pushZ);
            }
            case FLAME_LIGHT -> {
                level.addParticle(ParticleTypes.FLAME, px, py, pz, pushX, 0.08 + RND.nextDouble() * 0.05, pushZ);
                if (RND.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, px, py + 0.4, pz, pushX, 0.08, pushZ);
            }
            case FLAME_HEAVY -> {
                level.addParticle(ParticleTypes.FLAME, px, py, pz, pushX, 0.06 + RND.nextDouble() * 0.04, pushZ);
                ParticleOptions smoke = RND.nextInt(4) == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.LARGE_SMOKE;
                if (RND.nextInt(2) == 0) level.addParticle(smoke, px, py + 0.5, pz, pushX, 0.07, pushZ);
            }
            default -> {
            }
        }
    }
}
