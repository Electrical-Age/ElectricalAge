package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Set;

/** The flare: particles over the stack by what burns (gas clean, liquid fuels smoky, heavy black, steam a plume). */
public class FlareStackRender extends TransparentNodeElementRender {
    final FlareStackDescriptor desc;
    String burning = "";
    double due;
    static final Random RND = new Random();
    static final Set<String> HEAVY = Set.of("heavyoil", "residue", "crude_oil", "reservoir_oil", "creosote", "oil", "lava", "tar");
    static final Set<String> VAPOUR = Set.of("steam", "ic2steam", "water");

    enum Look {NONE, VAPOUR, GAS, LIGHT, HEAVY}

    public FlareStackRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (FlareStackDescriptor) d;
    }

    static Look look(String fluid) {
        if (fluid.isEmpty()) return Look.NONE;
        if (VAPOUR.contains(fluid)) return Look.VAPOUR;
        if (HEAVY.contains(fluid)) return Look.HEAVY;
        if (java.util.Arrays.asList(mods.eln.fluid.FuelRegistry.INSTANCE.getGasList()).contains(fluid)) return Look.GAS;
        return Look.LIGHT;
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            burning = stream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        Look l = look(burning);
        var level = tileEntity.getLevel();
        if (l == Look.NONE || level == null) return;
        due += deltaT * 20;   // ~20 puffs a second
        var p = tileEntity.getBlockPos();
        double x = p.getX() + 0.5, y = p.getY() + 1.05, z = p.getZ() + 0.5;
        while (due >= 1) {
            due -= 1;
            double jx = (RND.nextDouble() - 0.5) * 0.25, jz = (RND.nextDouble() - 0.5) * 0.25;
            switch (l) {
                case VAPOUR -> level.addParticle(ParticleTypes.CLOUD, x + jx, y, z + jz, jx * 0.1, 0.12, jz * 0.1);
                case GAS -> {
                    level.addParticle(ParticleTypes.FLAME, x + jx, y, z + jz, 0, 0.10 + RND.nextDouble() * 0.06, 0);
                    if (RND.nextInt(3) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + jx * 0.5, y, z + jz * 0.5, 0, 0.05, 0);
                }
                case LIGHT -> {
                    level.addParticle(ParticleTypes.FLAME, x + jx, y, z + jz, 0, 0.08 + RND.nextDouble() * 0.05, 0);
                    if (RND.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, x + jx, y + 0.4, z + jz, 0, 0.08, 0);
                }
                case HEAVY -> {
                    level.addParticle(ParticleTypes.FLAME, x + jx, y, z + jz, 0, 0.06 + RND.nextDouble() * 0.04, 0);
                    ParticleOptions smoke = RND.nextInt(4) == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.LARGE_SMOKE;
                    if (RND.nextInt(2) == 0) level.addParticle(smoke, x + jx, y + 0.5, z + jz, 0, 0.07, 0);
                }
                default -> {
                }
            }
        }
    }
}
