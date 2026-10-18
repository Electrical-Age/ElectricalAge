package mods.eln.node.voxel;

import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What comes out of a burst pipe or a blowing safety valve (Baughn: walking through a jet of superheated steam
 * should kill you; numbers from the second opinion, notes/pipes/a-overpressure.md): a jet along dir, as long as its
 * heat flux carries it (up to MAX_LENGTH blocks, stopped by solid blocks), seen and heard, scalding what stands in it.
 * The heat flux counts only what's above 55 C (warm water doesn't scald). Damage every DAMAGE_EVERY ticks, through
 * armour: DAMAGE_PER_10KW hearts' worth at point-blank for each 10 kW, less along the jet.
 */
public final class SteamJet {
    private SteamJet() {
    }

    public static final ResourceKey<DamageType> SCALD = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("eln", "scald"));
    static final double SCALD_K = 328.15, MAX_LENGTH = 4, DAMAGE_PER_10KW = 3.5, MAX_DAMAGE = 8;
    static final int DAMAGE_EVERY = 5;

    /** The heat flux (W) out of the vent edge this step (from the device's node into the air). */
    static double heatFlux(FluidEdge e) {
        if (e == null) return 0;
        double m = Math.abs(e.fLiquid) + Math.abs(e.fVapour) + Math.abs(e.fAir);
        return Math.max(0, Math.abs(e.fEnthalpy) - m * Water.hLiquid(SCALD_K));
    }

    /** The share of what flows out that is gas (whistle or sputter). */
    static double gasShare(FluidEdge e) {
        double m = Math.abs(e.fLiquid) + Math.abs(e.fVapour) + Math.abs(e.fAir);
        return m <= 0 ? 0 : (Math.abs(e.fVapour) + Math.abs(e.fAir)) / m;
    }

    /**
     * One tick of a jet from origin (world) along dir. length limit scales the reach (a safety valve's outlet is short).
     * Returns the heat flux (W).
     */
    public static double tick(ServerLevel level, Vec3 origin, Direction dir, FluidEdge edge, double maxLength) {
        double q = heatFlux(edge);
        if (q < 50) return q;
        double reach = Math.min(maxLength, 0.5 + 3.5 * Math.sqrt(q / 10000));
        // stopped by the first solid block
        Vec3 step = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        double len = 0;
        while (len < reach) {
            BlockPos b = BlockPos.containing(origin.add(step.scale(len + 0.5)));
            var s = level.getBlockState(b);
            if (!s.isAir() && s.isCollisionShapeFullBlock(level, b)) break;
            len += 0.5;
        }
        len = Math.min(len, reach);
        boolean gas = gasShare(edge) > 0.5;
        long t = level.getGameTime();
        if (len > 0) {
            int n = (int) Math.ceil(len * 3);
            for (int i = 0; i < n; i++) {
                Vec3 p = origin.add(step.scale(len * (i + 0.5) / n));
                level.sendParticles(gas ? ParticleTypes.CLOUD : ParticleTypes.SPLASH, p.x, p.y, p.z, 1, 0.08, 0.08, 0.08,
                    gas ? 0.08 : 0.3);
            }
        }
        if (t % 10 == 0)
            level.playSound(null, BlockPos.containing(origin), gas ? net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH
                : net.minecraft.sounds.SoundEvents.BUCKET_EMPTY, net.minecraft.sounds.SoundSource.BLOCKS,
                (float) Math.min(1.5, 0.3 + q / 20000), gas ? 0.6f : 1.4f);
        if (t % DAMAGE_EVERY == 0 && len > 0) {
            Vec3 end = origin.add(step.scale(len));
            AABB box = new AABB(origin, end).inflate(0.35);
            var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SCALD);
            DamageSource src = new DamageSource(holder);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                double d = e.position().add(0, e.getBbHeight() / 2, 0).subtract(origin).dot(step);
                double fall = 1 - 0.7 * Math.max(0, Math.min(1, d / Math.max(0.5, len)));
                float dmg = (float) Math.min(MAX_DAMAGE, DAMAGE_PER_10KW * 2 * q / 10000 * fall);   // (hearts -> HP: x2)
                if (dmg >= 0.5f) e.hurtServer(level, src, dmg);
            }
        }
        return q;
    }

    /** The open side for a jet out of a pipe at a point (block-local voxels) with this axis: of the four sides across
     *  the pipe, the one with the most free blocks within 3 (ties: sideways before up before down). */
    static int chooseDir(ServerLevel level, BlockPos pos, int axis) {
        int best = -1, bestScore = -1;
        Direction[] order = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
        for (Direction d : order) {
            if (d.getAxis().ordinal() == axis) continue;
            int score = 0;
            for (int i = 1; i <= 3; i++) {
                BlockPos b = pos.relative(d, i);
                var s = level.getBlockState(b);
                if (!s.isAir() && !s.canBeReplaced()) break;
                score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = d.get3DDataValue();
            }
        }
        return best;
    }
}
