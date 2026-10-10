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

    class MaceratorDescriptorHandle {
        float counter = 0, itemCounter = 0;
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
    }

    @Override
    Object newDrawHandle() {
        return new MaceratorDescriptorHandle();
    }

    // 26.x port: draw() removed (client rendering; redone in P3)
    // 26.x port: refresh() removed (client rendering; redone in P3)
    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
