package mods.eln.sim.mna.misc;

public class MnaConst {
    /** An open breaker, fuse holder or meter shunt. 1e16 (1.12) is beyond double precision against cable conductances
     *  (~100 S): a floating 51.2 kV line behind an open breaker solved to -3e20 V and 32 kA (HvOpenSwitchTest). */
    public static final double ultraImpedance = 1e12;
    public static final double highImpedance = 1e9;
    public static final double pullDown = 1e9;
    public static final double noImpedance = 1e-9;
}
