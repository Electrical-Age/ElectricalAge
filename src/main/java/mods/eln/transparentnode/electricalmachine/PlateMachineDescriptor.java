package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;

public class PlateMachineDescriptor extends ElectricalMachineDescriptor {
    private Obj3DPart main;
    private Obj3DPart rot1;
    private Obj3DPart rot2;

    public PlateMachineDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                  ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);

        if (obj != null) {
            main = obj.getPart("main");
            rot1 = obj.getPart("rot1");
            rot2 = obj.getPart("rot2");
        }
    }

    class PlateMachineDescriptorHandle {
        float counter = 0;
        final RcInterpolator interpolator = new RcInterpolator(0.5f);
        float itemCounter = 0;
    }

    @Override
    Object newDrawHandle() {
        return new PlateMachineDescriptorHandle();
    }

    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: refresh() removed (client rendering; redone in P3)

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
