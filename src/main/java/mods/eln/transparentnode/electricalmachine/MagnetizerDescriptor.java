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

        GL11.glScalef(0.5f, 0.5f, 0.5f);
        UtilsClient.drawEntityItem(inEntity, inX / 0.5f, inY / 0.5f, inZ / 0.5f, handle.itemCounter, 1f);
    }
    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemEntity inEntity, ItemEntity outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;
        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 360;
        while (handle.counter >= 360f) handle.counter -= 360;

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
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
