package mods.eln.sim.mna.misc;

import mods.eln.Eln;
import mods.eln.misc.materials.MaterialType;

public class MnaConst {
    public static final double ultraImpedance = 1e16;
    public static final double highImpedance = 1e9;
    public static final double pullDown = 1e9;
    public static final double noImpedance = Eln.mp.getElectricalResistivity(MaterialType.COPPER) * (25 / 1000000 / 1.0);
}
