package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.fluid.Water;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A steam turbine's condenser (Baughn 2026-10-26: an optional block beside the turbine; the models connect). Set against
 * the 50V/200V Steam Turbine's exhaust side (right, seen from its front), it takes the exhaust in place of the air: a
 * cooled vessel at the saturation pressure of its water (~0.1 bar at 46 C), so the steam expands further and gives
 * nearly twice the work per kg. Cooled to the local ambient by its own air cooler (sized for its turbine at full power),
 * plus whatever a thermal cable on a side takes. Its air ejector keeps air out; its condensate pump, driven by the
 * turbine's shaft, pushes the water out of its front flange (up to PUMP_PA) back to the boiler's feed. Water comes in
 * (makeup) by the flange on top. Model: tools/models/condenser.py.
 */
public class CondenserDescriptor extends TransparentNodeDescriptor {
    /** Heat (W) it rejects at its turbine's full power on a 10 bar inlet (0.1 bar, condensate at 46 C). */
    public final double nominalHeat;
    /** Its turbine's power (W), the vessel (m^3), and the condensate (kg/s) its pump moves against 1 atm. */
    public final double turbineP, volume, pumpKgPerS;
    /** The condensate pump's shutoff head (Pa, absolute; a boiler at a turbine's 25 bar burst can't be fed past it). */
    public static final double PUMP_PA = 15e5;
    /** The vessel's temperature over its shell's, and the shell's over ambient, at nominal heat (K). */
    public static final double DT_VESSEL = 6, DT_SHELL = 20;
    /** The air ejector's time constant (s): the air in it falls by e in this time. */
    public static final double EJECTOR_TAU = 3;
    /** ... down to this much air (Pa): a good condenser's leak-in. */
    public static final double EJECTOR_FLOOR_PA = 1000;
    /** Over this share of its volume as water, the turbine's exhaust floods: water damage. */
    public static final double FLOOD = 0.5;
    final Obj3D obj;

    public CondenserDescriptor(String name, double turbineP, double volume, String model) {
        super(name, CondenserElement.class, CondenserRender.class);
        this.turbineP = turbineP;
        this.volume = volume;
        double eta = SteamGeneratorDescriptor.ETA;
        double tIn = Water.tsat(SteamGeneratorDescriptor.NOMINAL_PA), drop = Water.isentropicDrop(tIn, SteamGeneratorDescriptor.NOMINAL_PA, 0.1e5);
        double kgPerS = turbineP / (eta * drop);
        this.nominalHeat = kgPerS * (Water.hVapour(tIn) - eta * drop - Water.hLiquid(Water.tsat(0.1e5)));
        this.pumpKgPerS = 2 * kgPerS;
        this.obj = Eln.obj.getObj(model);
    }

    /** The model, and its duct toward the turbine: "duct_right", "duct_back", "duct_left" (relative to its front), or none. */
    void draw(String duct) {
        if (obj == null) return;
        var p = obj.getPart("main");
        if (p != null) p.draw();
        var d = duct == null ? null : obj.getPart(duct);
        if (d != null) d.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Place it against a %.0f W steam turbine's exhaust side", turbineP));
        list.add("Nearly twice the work per kg of steam, if kept cool");
        list.add(String.format("Rejects %.1f kW of heat at full power; more with a thermal cable", nominalHeat / 1000));
        list.add("Condensate out of its front (to the boiler's feed), makeup water in on top");
    }
}
