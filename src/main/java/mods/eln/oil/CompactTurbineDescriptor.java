package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The compact steam turbine (Baughn 2026-10-27, the bootstrap option): boiler, turbine, generator and a tiny air-cooled
 * condenser in one block, with all its control built in. Heat in on its left flank (a heat furnace against it), water
 * by bucket, by EA pipe on its top flange or by the fluid capability, 50 V out of the front and back. It turns
 * {@link #ETA} of the heat it takes into electricity (Baughn: 10%, so one stone heat furnace with all its combustion
 * chambers runs it at full), against ~10% for a boiler and steam turbine venting and ~18% condensing; the cost of not
 * having to run a plant. Model: tools/models/compactturbine.py.
 */
public class CompactTurbineDescriptor extends TransparentNodeDescriptor {
    /** Electricity out of the heat taken. */
    public static final double ETA = 0.10;
    /** Boiler temperatures (C): it starts making power over T_START, at full power by T_RUN; over T_BYPASS the steam
     *  the load doesn't take goes straight to the condenser, over T_VENT out of the safety valve. */
    public static final double T_START = 105, T_RUN = 140, T_BYPASS = 150, T_VENT = 180;
    /** The bypass: W per K over T_BYPASS, up to BYPASS_MAX (more than a stone heat furnace's 3 kW, so one can't
     *  make it vent); the safety valve: W per K over T_VENT, up to VENT_MAX. */
    public static final double BYPASS_K = 500, BYPASS_MAX = 3500, VENT_K = 1000, VENT_MAX = 20000;
    /** Its tank (mB of water), and the share of the steam the condenser doesn't return (the makeup water). */
    public static final int WATER_MB = 4000;
    public static final double MAKEUP_SHARE = 0.03;
    /** Heat that turns 1 mB of water (67.5 mg in EA's scale) at ~20 C into low-pressure steam (J). */
    public static final double J_PER_MB = 2.6e6 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
    /** The boiler's heat capacity (J/K; from cold to boiling in ~2 minutes on a bare 1 kW heat furnace), its
     *  insulation (K/W to the ambient) and its contact with the heat source (K/W). */
    public static final double BOILER_C = 1500, BOILER_RP = 3, BOILER_RS = 0.005;
    /** It burns out over this (C): never from a heat furnace (780 C). */
    public static final double T_MAX = 1000;

    final double nominalU, nominalP, maxU;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public CompactTurbineDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, CompactTurbineElement.class, CompactTurbineRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.maxU = nominalU * 1.05;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FRONT_AND_BACK;   // (terminal plates front and back, the model)
    }

    /** Heat it takes at full power (W). */
    public double nominalHeat() {
        return nominalP / ETA;
    }

    /** The vent's outlet (model coordinates, front XN), from the model's .txt; null without a model. */
    float[] vent() {
        var p = obj == null ? null : obj.getPart("main");
        return p == null ? null : new float[]{p.getFloat("ventX"), p.getFloat("ventY"), p.getFloat("ventZ")};
    }

    void draw() {
        draw(0);
    }

    /** The model, its rotor turned `angle` degrees about the shaft. */
    void draw(float angle) {
        if (obj == null) return;
        var p = obj.getPart("main");
        if (p != null) p.draw();
        var r = obj.getPart("rotor");
        if (r != null) r.draw(angle, 1, 0, 0);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("Heat in on its left side: put a heat furnace against it");
        list.add(String.format("Water: a bucket, or a pipe on top (%d mB tank)", WATER_MB));
        list.add(String.format("%.0f V out, front and back; nothing to set", nominalU));
        list.add(String.format("Up to %.0f W from %.0f W of heat (%.0f%%)", nominalP, nominalHeat(), ETA * 100));
        list.add("A stone heat furnace with 4 combustion chambers runs it at full");
    }
}
