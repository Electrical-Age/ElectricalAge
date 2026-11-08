package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;

public class MagnetizerDescriptor extends ElectricalMachineDescriptor {
    private Obj3DPart main;
    private Obj3DPart rot;
    /** Where the item is drawn (block units from the centre; the restyled models' rot.txt says, 2026-11-07). */
    private float inX = 0f, inY = 0.125f, inZ = 0f;

    public MagnetizerDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS_AND_BACK;   // (the restyled models' floor terminal plates, 2026-11-07)
        if (obj != null) {
            rot = obj.getPart("rot");
            main = obj.getPart("main");
            if (rot != null && (rot.getFloat("iny") != 0 || rot.getFloat("inx") != 0)) {
                inX = rot.getFloat("inx");
                inY = rot.getFloat("iny");
                inZ = rot.getFloat("inz");
            }
        }
        initLeds(obj);
    }

    class MaceratorDescriptorHandle {
        float counter = 0;
        float itemCounter = 0;
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
        final mods.eln.anim.Emitter sparks = new mods.eln.anim.Emitter();
    }

    @Override
    Object newDrawHandle() {
        return new MaceratorDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        main.draw();
        rot.draw(handle.counter, 0f, 0f, 1f);

        // the piece being magnetised (Baughn 2026-11-07: more than a turning item) lifts off its seat in the field and
        // spins up as it charges (refresh), sparks crackling round it
        float lift = 0.04f * mods.eln.anim.ItemMotion.ease(render == null ? 0 : render.progress.shown(), 0, 0.3f);
        GL11.glScalef(0.5f, 0.5f, 0.5f);
        UtilsClient.drawEntityItem(inEntity, inX / 0.5f, (inY + lift) / 0.5f, inZ / 0.5f, handle.itemCounter, 1f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;
        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 360;
        while (handle.counter >= 360f) handle.counter -= 360;

        float p = render == null ? 0 : render.progress.shown();
        handle.itemCounter += deltaT * (90 + 630 * p * p);   // (a quarter turn a second at rest, two at the end)
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
        int n = handle.sparks.step(deltaT, p > 0 ? 3 + 9 * p : 0);
        var level = render.tileEntity.getLevel();
        if (n > 0 && level != null && inEntity != null && !inEntity.getItem().isEmpty()) {
            var at = mods.eln.anim.Emitter.vent(render.tileEntity.getBlockPos(), render.front, inX, inY + 0.12, inZ);
            for (int i = 0; i < n; i++)
                level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    at.x + (Math.random() - 0.5) * 0.3, at.y + (Math.random() - 0.5) * 0.2, at.z + (Math.random() - 0.5) * 0.3, 0, 0, 0);
        }
    }
    @Override
    public boolean drawCable() {
        return true;
    }

    @Override
    CableRenderDescriptor getPowerCableRender() {
        return cable.render;
    }
}
