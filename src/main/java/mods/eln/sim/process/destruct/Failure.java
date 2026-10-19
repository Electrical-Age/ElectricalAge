package mods.eln.sim.process.destruct;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.function.DoubleSupplier;

/**
 * What happens when a watchdog trips. Replaces 1.7.10's WorldExplosion, where everything exploded.
 * Most devices burn out in place (see IBurnable): they leave the simulation, look scorched and wait for repair.
 * The outcome depends on the kind of device and the cause; see notes/failure-modes.md for the table.
 * Only a shaft overspeed (and a big battery's thermal runaway) is a real explosion.
 */
public class Failure implements IDestructable {
    public enum Kind {
        CABLE, ELECTRONICS, MACHINE, TRANSFORMER, CAPACITOR, BATTERY, ROTATING, HV_LINE, PRESSURE_VESSEL;

        public static Kind fromOrdinal(int i) {
            Kind[] v = values();
            return i >= 0 && i < v.length ? v[i] : MACHINE;
        }
    }

    /** Faults on HV equipment at or above this voltage cause an arc flash. */
    public static final double ARC_FLASH_VOLTAGE = 3200;

    private final Coordonate c;
    private final String type;
    @Nullable
    private final IBurnable target;
    @Nullable
    private final NodeBase node;
    private Kind kind = Kind.MACHINE;
    @Nullable
    private DoubleSupplier energy;

    @Nullable
    private FailureCause pendingCause;
    private double pendingValue;
    private double pendingSeverity;

    public Failure(SixNodeElement e) {
        this.c = e.getCoordonate();
        this.type = e.toString();
        this.target = e;
        this.node = e.sixNode;
    }

    public Failure(TransparentNodeElement e) {
        this.c = e.coordonate();
        this.type = e.toString();
        this.target = e;
        this.node = e.node;
    }

    /** Anything else that burns out in place (a voxel node's cable conductor). */
    public Failure(IBurnable target, NodeBase node, Coordonate c, String type) {
        this.c = c;
        this.type = type;
        this.target = target;
        this.node = node;
    }

    public Failure cable() {
        kind = Kind.CABLE;
        return this;
    }

    public Failure electronics() {
        kind = Kind.ELECTRONICS;
        return this;
    }

    public Failure machine() {
        kind = Kind.MACHINE;
        return this;
    }

    public Failure transformer() {
        kind = Kind.TRANSFORMER;
        return this;
    }

    public Failure capacitor() {
        kind = Kind.CAPACITOR;
        return this;
    }

    /** @param storedEnergy energy in J at the moment of failure; a big battery explodes. */
    public Failure battery(DoubleSupplier storedEnergy) {
        kind = Kind.BATTERY;
        energy = storedEnergy;
        return this;
    }

    /** @param shaftEnergy kinetic energy of the shaft in J; scales an overspeed burst. */
    public Failure rotating(DoubleSupplier shaftEnergy) {
        kind = Kind.ROTATING;
        energy = shaftEnergy;
        return this;
    }

    /** @param flashEnergy what the contents would release flashing to 1 atm (J); scales the burst. */
    public Failure pressureVessel(DoubleSupplier flashEnergy) {
        kind = Kind.PRESSURE_VESSEL;
        energy = flashEnergy;
        return this;
    }

    public Failure hvLine() {
        kind = Kind.HV_LINE;
        return this;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * Watchdogs call this from inside simulator steps, so the failure itself is queued and runs at the end of the
     * slow tick (Simulator.queueDestruct), where it is safe to change the simulation.
     */
    @Override
    public void fail(FailureCause cause, double value) {
        fail(cause, value, 0);
    }

    @Override
    public void fail(FailureCause cause, double value, double severity) {
        if (pendingCause != null) {
            // a second watchdog of the same device in the same tick: keep the worse one
            if (severity > pendingSeverity) {
                pendingCause = cause;
                pendingValue = value;
                pendingSeverity = severity;
            }
            return;
        }
        pendingCause = cause;
        pendingValue = value;
        pendingSeverity = severity;
        Eln.simulator.queueDestruct(this);
    }

    @Override
    public double severity() {
        return pendingSeverity;
    }

    /** The device's node; Simulator groups failures by the network of connected nodes (FaultNetwork). */
    @Override
    public Object circuit() {
        return node;
    }

    @Override
    public void cancelFail() {
        pendingCause = null;
    }

    /** Direct callers (not watchdogs) without a cause: treated as an overheat. */
    @Override
    public void destructImpl() {
        FailureCause cause = pendingCause != null ? pendingCause : FailureCause.OVERHEAT;
        double value = pendingValue;
        pendingCause = null;
        if (target != null && target.isBurnt()) return;
        if (node != null && node.isDestructing()) return;
        Level w = c.world();
        if (w == null || w.isClientSide()) return;
        Eln.LOG.info(String.format("%s fails: %s (%s, %.0f)", describe(), kind, cause, value));
        apply(w, cause, value);
    }

    /** The outcome table. Public outcome per (kind, cause); see the class comment. */
    public enum Outcome {BURN, BURN_FIRE, BURN_FIRE_WIDE, POP, ARC_FLASH, EXPLODE}

    public static Outcome outcome(Kind kind, FailureCause cause, double value, double energy) {
        switch (kind) {
            case CABLE:
            case ELECTRONICS:
            case MACHINE:
                return cause == FailureCause.OVERHEAT ? Outcome.BURN_FIRE : Outcome.BURN;
            case TRANSFORMER:
                return Outcome.BURN_FIRE_WIDE;
            case CAPACITOR:
                return cause == FailureCause.OVERVOLTAGE ? Outcome.POP : Outcome.BURN_FIRE;
            case BATTERY:
                return energy >= BATTERY_EXPLOSION_ENERGY ? Outcome.EXPLODE : Outcome.BURN_FIRE;
            case ROTATING:
                return cause == FailureCause.OVERSPEED ? Outcome.EXPLODE
                    : cause == FailureCause.OVERHEAT ? Outcome.BURN_FIRE : Outcome.BURN;
            case PRESSURE_VESSEL:
                return cause == FailureCause.OVERPRESSURE ? Outcome.EXPLODE : cause == FailureCause.OVERHEAT ? Outcome.BURN_FIRE : Outcome.BURN;
            case HV_LINE:
                if (cause == FailureCause.OVERVOLTAGE && Math.abs(value) >= ARC_FLASH_VOLTAGE) return Outcome.ARC_FLASH;
                return cause == FailureCause.OVERHEAT ? Outcome.BURN_FIRE : Outcome.BURN;
        }
        return Outcome.BURN;
    }

    /** Stored energy above which a failing battery explodes instead of only venting and burning (J). */
    public static final double BATTERY_EXPLOSION_ENERGY = 1e6;

    /** Explosion strength for a burst holding this much energy: 1 (small) .. 4 (TNT). */
    public static float explosionStrength(double energyJ) {
        return (float) Math.max(1, Math.min(4, Math.sqrt(Math.max(0, energyJ) / 15000)));
    }

    private void apply(Level w, FailureCause cause, double value) {
        double e = energy != null ? energy.getAsDouble() : 0;
        Outcome outcome = outcome(kind, cause, value, e);
        if (outcome == Outcome.EXPLODE && !Eln.explosionEnable) outcome = Outcome.BURN_FIRE;
        BlockPos pos = new BlockPos(c.x, c.y, c.z);

        switch (outcome) {
            case EXPLODE:
                if (node != null) {
                    node.physicalSelfDestruction(explosionStrength(e));
                } else {
                    w.explode(null, c.x + .5, c.y + .5, c.z + .5, explosionStrength(e), Level.ExplosionInteraction.BLOCK);
                }
                return;
            case POP:
                // A capacitor bursting: a bang and a shove, no crater.
                w.explode(null, c.x + .5, c.y + .5, c.z + .5, 1.0f, Level.ExplosionInteraction.NONE);
                break;
            case ARC_FLASH:
                arcFlash(w, pos, value);
                break;
            default:
                break;
        }

        burnOut(w, pos, cause);
        if (outcome == Outcome.BURN_FIRE || outcome == Outcome.ARC_FLASH) ignite(w, pos, false);
        if (outcome == Outcome.BURN_FIRE_WIDE) ignite(w, pos, true);
    }

    private void burnOut(Level w, BlockPos pos, FailureCause cause) {
        if (target != null) {
            target.burn(cause, kind);
        } else if (node != null) {
            node.physicalSelfDestruction(0);
        }
        particles(w, pos, ParticleTypes.LARGE_SMOKE, 12, 0.3);
        if (cause == FailureCause.OVERVOLTAGE) {
            particles(w, pos, ParticleTypes.CRIT, 20, 0.5);
            sound(w, pos, SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.6f);
        } else {
            particles(w, pos, ParticleTypes.LAVA, 4, 0.2);
            sound(w, pos, SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.8f);
        }
    }

    /** Set fire to air next to the device where there's something to burn; wide = oil fire, burns even on stone. */
    static void ignite(Level w, BlockPos pos, boolean wide) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!w.isEmptyBlock(p)) continue;
            boolean fuel = wide && p.getY() >= pos.getY() && w.getBlockState(p.below()).isFaceSturdy(w, p.below(), Direction.UP);
            if (!fuel) {
                for (Direction f : Direction.values()) {
                    BlockPos n = p.relative(f);
                    if (w.getBlockState(n).isFlammable(w, n, f.getOpposite())) {
                        fuel = true;
                        break;
                    }
                }
            }
            if (fuel) w.setBlock(p.immutable(), Blocks.FIRE.defaultBlockState(), 11);
        }
    }

    /** A flash and a bang; everything nearby gets burnt. Strength grows with the voltage. */
    private static void arcFlash(Level w, BlockPos pos, double voltage) {
        double r = Math.min(8, 2 + Math.abs(voltage) / ARC_FLASH_VOLTAGE);
        sound(w, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5f, 1.8f);
        particles(w, pos, ParticleTypes.FIREWORK, 60, 0.8);
        particles(w, pos, ParticleTypes.FLAME, 30, 0.6);
        AABB box = new AABB(pos).inflate(r);
        if (!(w instanceof ServerLevel)) return;
        ServerLevel sl = (ServerLevel) w;
        for (LivingEntity ent : w.getEntitiesOfClass(LivingEntity.class, box)) {
            double d = Math.sqrt(ent.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5));
            if (d > r) continue;
            ent.igniteForSeconds((float) Math.ceil(8 * (1 - d / r)) + 1);
            ent.hurtServer(sl, w.damageSources().inFire(), (float) (10 * (1 - d / r)) + 1);
        }
    }

    private static void particles(Level w, BlockPos pos, ParticleOptions type, int n, double spread) {
        if (w instanceof ServerLevel) {
            ((ServerLevel) w).sendParticles(type, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, n, spread, spread, spread, 0.02);
        }
    }

    private static void sound(Level w, BlockPos pos, SoundEvent s, float volume, float pitch) {
        w.playSound(null, pos, s, SoundSource.BLOCKS, volume, pitch);
    }

    @Override
    public String describe() {
        return String.format("%s (%s)", this.type, this.c.toString());
    }
}
