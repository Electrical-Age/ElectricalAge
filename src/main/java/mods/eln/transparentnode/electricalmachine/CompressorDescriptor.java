package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.item.ItemEntity;
import org.lwjgl.opengl.GL11;

public class CompressorDescriptor extends ElectricalMachineDescriptor {
    private float tyOn;
    private float tyOff;

    private Obj3DPart main;
    private Obj3DPart move;

    public CompressorDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
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

    class CompressorDescriptorHandle {
        final RcInterpolator interpolator = new RcInterpolator(0.25f);
        float itemCounter = 0f;
    }

    @Override
    Object newDrawHandle() {
        return new CompressorDescriptorHandle();
    }

    @Override
    public float volumeForRunningSound(float processState, float powerFactor) {
        if (processState < 0.1)
            return 0f;
        else if (processState < 0.3)
            return super.volumeForRunningSound(processState, powerFactor) * (processState - 0.1f) * 5f;
        else
            return super.volumeForRunningSound(processState, powerFactor);
    }

    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: refresh() removed (client rendering; redone in P3)

    @Override
    public boolean powerLrdu(Direction side, Direction front) {
        return side != front && side != front.getInverse();
    }
}
