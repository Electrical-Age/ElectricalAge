package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.item.EntityItem;
import org.lwjgl.opengl.GL11;

public class ArcFurnaceDescriptor extends ElectricalMachineDescriptor {
    private float tyOn;
    private float tyOff;

    private Obj3DPart main;
    private Obj3DPart move;

    public ArcFurnaceDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);

        if (obj != null) {
            main = obj.getPart("main");
            move = obj.getPart("move");
            if (move != null) {
                tyOn = move.getFloat("tyon");
                tyOff = move.getFloat("tyoff");
            }
        }
    }

    class ArcFurnaceDescriptorHandle {
        final RcInterpolator interpolator = new RcInterpolator(0.25f);
        float itemCounter = 0f;
    }

    @Override
    Object newDrawHandle() {
        return new ArcFurnaceDescriptorHandle();
    }

    @Override
    public float volumeForRunningSound(float processState, float powerFactor) {
        /*if (processState < 0.1)
            return 0f;
        else if (processState < 0.3)
            return super.volumeForRunningSound(processState, powerFactor) * (processState - 0.1f) * 5f;
        else*/
            return super.volumeForRunningSound(processState, powerFactor);
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity,
              float powerFactor, float processState) {
        ArcFurnaceDescriptorHandle handle = (ArcFurnaceDescriptorHandle) handleO;

        UtilsClient.drawEntityItem(inEntity, -0.35f, 0.04f, 0.3f, handle.itemCounter, 1f);
        UtilsClient.drawEntityItem(outEntity, 0.35f, 0.04f, 0.3f, -handle.itemCounter + 139f, 1f);

        main.draw();
        GL11.glTranslatef(0f, tyOff + (float) Math.sqrt(handle.interpolator.get()) * (tyOn - tyOff), 0f);
        move.draw();
    }

    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity, float powerFactor, float processState) {
        ArcFurnaceDescriptorHandle handle = (ArcFurnaceDescriptorHandle) handleO;
        handle.interpolator.setTarget(processState);
        handle.interpolator.step(deltaT);

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
