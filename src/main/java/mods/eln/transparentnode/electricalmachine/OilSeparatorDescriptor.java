package mods.eln.transparentnode.electricalmachine;

import mods.eln.misc.RecipesList;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * 26.x oil (design: notes/oil): the tar sand separator, hot-water extraction. A macerator-style machine (for now on
 * the macerator's model) whose recipe turns tar sand into sand and puts 100 mB of crude into its tank; it stops when
 * the tank can't take the next 100 mB. Pipes (Pipez, Energized Power...) drain the tank through the fluid capability.
 */
public class OilSeparatorDescriptor extends MaceratorDescriptor {
    public static final int CRUDE_PER_ITEM = 100;
    public static final int TANK_MB = 4000;

    public OilSeparatorDescriptor(String name, String modelName, double nominalU, double nominalP, double maximalU,
                                  ThermalLoadInitializer thermal, ElectricalCableDescriptor cable, RecipesList recipe) {
        super(name, modelName, nominalU, nominalP, maximalU, thermal, cable, recipe);
        ElementClass = OilSeparatorElement.class;
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // mods.eln.cad.models.TarSandSeparator terminal plates
    }
}
