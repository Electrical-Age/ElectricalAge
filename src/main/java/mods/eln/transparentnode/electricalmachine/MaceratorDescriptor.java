package mods.eln.transparentnode.electricalmachine;

import mods.eln.Eln;
import mods.eln.api.recipe.RecipesList;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.UtilsClient;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.item.EntityItem;
import org.lwjgl.opengl.GL11;

public class MaceratorDescriptor extends ElectricalMachineDescriptor {
    private final Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart rot1;
    private Obj3DPart rot2;

    public MaceratorDescriptor(String name, String modelName, double nominalU, double nominalP, double maximalU,
                               ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        obj = Eln.obj.getObj(modelName);
        if (obj != null) {
            rot1 = obj.getPart("rot1");
            rot2 = obj.getPart("rot2");
            main = obj.getPart("main");
        }
    }

    @Override
    Object newDrawHandle() {
        return new MaceratorDescriptorHandle();
    }

    @Override
    void draw(ElectricalMachineRender render, Object handleO, EntityItem inEntity, EntityItem outEntity,
              float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        main.draw();
        rot1.draw(handle.counter, 0f, 0f, -1f);
        rot2.draw(handle.counter, 0f, 0f, 1f);

        //UtilsClient.enableDepthTest();
        GL11.glScalef(0.7f, 0.7f, 0.7f);
        UtilsClient.drawEntityItem(inEntity, 0.0, 0.4f, 0f, handle.itemCounter, 1f);
        UtilsClient.drawEntityItem(outEntity, 0.0, -0.5f, 0f, 130 + handle.itemCounter, 1f);
    }

    @Override
    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, EntityItem inEntity,
                 EntityItem outEntity, float powerFactor, float processState) {
        MaceratorDescriptorHandle handle = (MaceratorDescriptorHandle) handleO;

        handle.interpolator.setTarget(powerFactor);
        handle.interpolator.step(deltaT);
        handle.counter += deltaT * handle.interpolator.get() * 180;
        while (handle.counter >= 360f) handle.counter -= 360;

        handle.itemCounter += deltaT * 90;
        while (handle.itemCounter >= 360f) handle.itemCounter -= 360;
    }

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }

    class MaceratorDescriptorHandle {
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
        float counter = 0, itemCounter = 0;
    }
}
