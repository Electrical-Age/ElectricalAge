package mods.eln.transparentnode.electricalmachine;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;
import org.lwjgl.opengl.GL11;

public class MagnetizerDescriptor extends ElectricalMachineDescriptor {
    private Obj3DPart main;
    private Obj3DPart rot;

    public MagnetizerDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, nominalU, nominalP, maximalU, thermal, cable, recipe);
        if (obj != null) {
            rot = obj.getPart("rot");
            main = obj.getPart("main");
        }
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

    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: refresh() removed (client rendering; redone in P3)

    @Override
    public boolean drawCable() {
        return true;
    }

    @Override
    CableRenderDescriptor getPowerCableRender() {
        return cable.render;
    }
}
