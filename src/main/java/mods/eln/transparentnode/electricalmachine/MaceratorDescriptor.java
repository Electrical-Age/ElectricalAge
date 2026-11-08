package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;

public class MaceratorDescriptor extends ElectricalMachineDescriptor {
    private final Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart rot1;
    private Obj3DPart rot2;
    /** The 230V macerator's flywheel belt (its model's beltlink part; null on the 48V). */
    private mods.eln.anim.Belt belt;

    public MaceratorDescriptor(String name, String modelName, double nominalU, double nominalP, double maximalU,
                               ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // (the restyled models' floor terminal plates, 2026-11-07)
        obj = Eln.obj.getObj(modelName);
        if (obj != null) {
            rot1 = obj.getPart("rot1");
            rot2 = obj.getPart("rot2");
            main = obj.getPart("main");
            belt = mods.eln.anim.Belt.of(obj.getPart("beltlink"));
            if (belt != null) belt.mark = obj.getPart("beltmark");
        }
        initLeds(obj);
        if (rot1 != null) sinkY = Math.min(IN_Y, (rot1.getFloat("originY") + 0.06f) / 0.7f);
    }

    /** Where the piece being ground is drawn (in the 0.7-scaled item frame): its start in the hopper, and how far it
     *  sinks, to just over the rollers' axis (rot1's .txt origin). */
    static final float IN_Y = 0.4f;
    float sinkY = 0.05f;
    /** How long a new piece takes to drop in (s). */
    static final float ENTRY_S = 0.35f;

    class MaceratorDescriptorHandle {
        /** The rollers: half a turn a second at full power, easing over half a second; the items a quarter turn a second. */
        final mods.eln.anim.Spin rollers = new mods.eln.anim.Spin(180, 0.5), items = new mods.eln.anim.Spin(90, 0).speed(1);
        final mods.eln.anim.Emitter souls = new mods.eln.anim.Emitter();
        /** Seconds since the current piece went in (a new job), and the progress last frame. */
        float entered = ENTRY_S, lastP;
        final mods.eln.anim.Belt.Run belt = new mods.eln.anim.Belt.Run();
    }

    @Override
    Object newDrawHandle() {
        return new MaceratorDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity,
              float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        main.draw();
        handle.rollers.draw(rot1, 0f, 0f, -1f);
        handle.rollers.draw(rot2, 0f, 0f, 1f);
        if (belt != null) belt.draw(handle.rollers.deg(), handle.belt);

        GL11.glScalef(0.7f, 0.7f, 0.7f);
        // the piece being ground (Baughn 2026-11-07: more than a turning item): it sinks from the hopper toward the
        // rollers as the job goes on, shrinking as it's broken up, and rattles while they turn
        float p = render == null ? 0 : render.progress.shown();
        // (a new piece drops in from the hopper's mouth: AdventurAgent saw it pop in at full size)
        float drop = 0.3f * (1 - mods.eln.anim.ItemMotion.ease(handle.entered, 0, ENTRY_S));
        float y = mods.eln.anim.ItemMotion.lerp(IN_Y, sinkY, mods.eln.anim.ItemMotion.ease(p, 0, 0.8f)) + drop;
        float size = mods.eln.anim.ItemMotion.lerp(1, 0.3f, mods.eln.anim.ItemMotion.ease(p, 0.25f, 1));
        float amp = 0.035f * Math.min(1, handle.rollers.speedNow() * 2) * (p > 0 ? 1 : 0);
        mods.eln.anim.ItemMotion.draw(inEntity, mods.eln.anim.ItemMotion.shake(handle.rollers.deg(), amp, 0), y,
            mods.eln.anim.ItemMotion.shake(handle.rollers.deg(), amp, 1), handle.items.deg(), size, size, size);
        UtilsClient.drawEntityItem(outEntity, 0.0, -0.5f, 0f, 130 + handle.items.deg(), 1f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity,
                 ItemEntity outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        handle.rollers.speed(powerFactor).step(deltaT);
        handle.items.step(deltaT);
        float shown = render.progress.shown();
        if (shown < handle.lastP - 0.3f) handle.entered = 0;   // (the progress started over: the next piece)
        handle.entered = Math.min(ENTRY_S, handle.entered + deltaT);
        handle.lastP = shown;

        // souls rise from the hopper while it grinds soul sand or soul soil (Baughn 2026-11-07)
        boolean grinding = inEntity != null && isSoulBlock(inEntity.getItem()) && handle.rollers.speedNow() > 0.1f;
        int n = handle.souls.step(deltaT, grinding ? 4 * handle.rollers.speedNow() : 0);
        var level = render.tileEntity.getLevel();
        if (n > 0 && level != null) {
            var p = render.tileEntity.getBlockPos();
            for (int i = 0; i < n; i++) {
                double jx = (Math.random() - 0.5) * 0.5, jz = (Math.random() - 0.5) * 0.5;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL,
                    p.getX() + 0.5 + jx, p.getY() + 1.05, p.getZ() + 0.5 + jz, 0, 0.03, 0);
            }
        }
    }
    /** Soul sand and soul soil: what makes the macerator give off souls. */
    public static boolean isSoulBlock(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.SOUL_SAND) || stack.is(net.minecraft.world.item.Items.SOUL_SOIL);
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
