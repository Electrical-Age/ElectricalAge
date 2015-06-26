package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.item.EntityItem;

public class PlateMachineDescriptor extends ElectricalMachineDescriptor {

    Obj3D obj;
    Obj3DPart main, rot1, rot2;

    public PlateMachineDescriptor(String name,
                                  Obj3D obj,
                                  double nominalU, double nominalP,
                                  double maximalU, ThermalLoadInitializer thermal,
                                  ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);

        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            rot1 = obj.getPart("rot1");
            rot2 = obj.getPart("rot2");
        }
    }

    class PlateMachineDescriptorHandle {
        float counter = 0;
        RcInterpolator interpolator = new RcInterpolator(0.5f);
        float itemCounter = 0;
    }

    @Override
    Object newDrawHandle() {
        return new PlateMachineDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;

        main.draw();
        rot1.draw(handle.counter, 0f, 0f, -1f);
        rot2.draw(handle.counter, 0f, 0f, 1f);

        UtilsClient.drawEntityItem(inEntity, -0.35f, 0.1f, 0f, handle.itemCounter, 1f);
        UtilsClient.drawEntityItem(outEntity, 0.35f, 0.1f, 0f, -handle.itemCounter + 139f, 1f);
    }

    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {
        PlateMachineDescriptorHandle handle = (PlateMachineDescriptorHandle) handleO;
        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 360;
        while (handle.counter >= 360f) handle.counter -= 360;

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
