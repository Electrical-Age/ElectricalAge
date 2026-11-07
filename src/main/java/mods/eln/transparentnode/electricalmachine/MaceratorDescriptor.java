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
    }

    class MaceratorDescriptorHandle {
        float counter = 0, itemCounter = 0, soulsDue = 0;
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
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
        rot1.draw(handle.counter, 0f, 0f, -1f);
        rot2.draw(handle.counter, 0f, 0f, 1f);
        if (belt != null) belt.draw(handle.counter);

        //UtilsClient.enableDepthTest();
        GL11.glScalef(0.7f, 0.7f, 0.7f);
        UtilsClient.drawEntityItem(inEntity, 0.0, 0.4f, 0f, handle.itemCounter, 1f);
        UtilsClient.drawEntityItem(outEntity, 0.0, -0.5f, 0f, 130 + handle.itemCounter, 1f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity,
                 ItemEntity outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 180;
        while (handle.counter >= 360f) handle.counter -= 360;

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;

        // souls rise from the hopper while it grinds soul sand or soul soil (Baughn 2026-11-07)
        if (inEntity != null && isSoulBlock(inEntity.getItem()) && handle.interpolator.get() > 0.1f) {
            var level = render.tileEntity.getLevel();
            if (level != null) {
                handle.soulsDue += deltaT * 4 * handle.interpolator.get();
                var p = render.tileEntity.getBlockPos();
                while (handle.soulsDue >= 1) {
                    handle.soulsDue -= 1;
                    double jx = (Math.random() - 0.5) * 0.5, jz = (Math.random() - 0.5) * 0.5;
                    level.addParticle(net.minecraft.core.particles.ParticleTypes.SOUL,
                        p.getX() + 0.5 + jx, p.getY() + 1.05, p.getZ() + 0.5 + jz, 0, 0.03, 0);
                }
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
