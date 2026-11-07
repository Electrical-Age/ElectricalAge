package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.Obj3D;
import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * The 230V compressor (Baughn 2026-11-07: a pipe output, so the compressor gets two flanges on its back, blue IN low and
 * red OUT higher): a compressor with the dandelion recipe and latex ports (CompressorPortsElement).
 */
public class CompressorPortsDescriptor extends CompressorDescriptor {
    /** Its tanks (mB) and its ports' rate and head, as the oil devices'. */
    public static final int TANK_MB = 4000;
    public static final double PORT_MB_PER_S = 200, OUT_HEAD_PA = 6e5;
    /** Its flanges' centres on the back face (px, model frame; the model is mods.eln.cad.models.Compressor, IN_Y etc.). */
    public static final int IN_Y = 4, OUT_Y = 9, FLANGE_Z = 13;
    /** Pressing latex to Raw Rubber: 250 mB a rubber, this much energy (J). */
    public static final double LATEX_JOULES = 2000;

    public CompressorPortsDescriptor(String name, Obj3D obj, double nominalU, double nominalP, double maximalU,
                                     ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, obj, nominalU, nominalP, maximalU, thermal, cable, new PressRecipes(recipe));
        ElementClass = CompressorPortsElement.class;
    }
}
