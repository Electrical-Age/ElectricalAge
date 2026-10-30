package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (Baughn, oil thread): the small steam turbine, a turbine with its generator built in that puts out 50 or 200 V
 * like the old heat turbine, which it replaces as early-game power (coal -> heat furnace -> boiler -> this). It gets
 * {@link #ETA} of what the steam gives expanding from its inlet to the exhaust (Baughn 2026-10-26: by the expansion,
 * Water.isentropicDrop), up to its nominal power, which it reaches on steam at {@link #NOMINAL_PA} venting to air; the
 * stock shaft Steam Turbine (7,200 mB/s) stays for big plants. Model: mods.eln.cad.models.SteamTurbine.
 */
public class SteamGeneratorDescriptor extends TransparentNodeDescriptor {
    /** Electric work per kg out of the ideal (isentropic) expansion: turbine and generator together. */
    public static final double ETA = 0.70;
    /** The inlet pressure it is rated for (saturated steam, venting to air), and the one its casing bursts over (Pa). */
    public static final double NOMINAL_PA = 10e5, BURST_PA = 25e5;
    /** Work per kg at the rating (J/kg): 0.7 x 409 kJ/kg. */
    public static final double NOMINAL_J_PER_KG = ETA * mods.eln.sim.fluid.Water.isentropicDrop(
        mods.eln.sim.fluid.Water.tsat(NOMINAL_PA), NOMINAL_PA, mods.eln.sim.fluid.Water.P_ATM);
    /** The tank holds this many seconds of nominal steam; the output follows the tank with this time constant. */
    public static final double TANK_SECONDS = 2, RESPONSE_SECONDS = 1;

    final double nominalU, nominalP, maxU;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public SteamGeneratorDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, SteamGeneratorElement.class, SteamGeneratorRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.maxU = nominalU * 1.05;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.ALL_SIDES; // terminal plates front and back, the control signal's on the left (model)
    }

    /** Steam (mB/s) at nominal power, at the rating. */
    public double nominalSteam() {
        return nominalSteam(nominalP);
    }

    public static double nominalSteam(double nominalP) {
        return nominalP / NOMINAL_J_PER_KG / mods.eln.sim.fluid.EaUnits.KG_PER_MB_STEAM;
    }

    void draw() {
        draw(0);
    }

    /** The vent's outlet (model coordinates, front XN), from the model's .txt; null without a model. */
    float[] vent() {
        var p = obj == null ? null : obj.getPart("main");
        return p == null ? null : new float[]{p.getFloat("ventX"), p.getFloat("ventY"), p.getFloat("ventZ")};
    }

    void draw(float angle) {
        draw(angle, false);
    }

    /** The model, its rotor (shaft and coupling) turned `angle` degrees about the shaft; its vent stack, or with a
     *  condenser beside it the exhaust duct into that. */
    void draw(float angle, boolean condenser) {
        if (obj != null) {
            var p = obj.getPart("main");
            if (p != null) p.draw();
            var v = obj.getPart(condenser ? "exhaust" : "vent");
            if (v != null) v.draw();
            var r = obj.getPart("rotor");
            if (r != null) r.draw(angle, 1, 0, 0);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Steam in (any side), %.0f V out (front and back)", nominalU));
        list.add("Its screen: throttle, or a regulator holding a target inlet pressure; a signal on its left side for external control");
        list.add(String.format("Up to %.0f W from %s/s of steam at %.0f bar", nominalP, mods.eln.sim.fluid.EaUnits.steamGrams(nominalSteam()), NOMINAL_PA / 1e5));
        list.add(String.format("Its casing bursts over %.0f bar", BURST_PA / 1e5));
        list.add("Might want a boiler with that.");   // (Baughn: the heat turbine gave way to steam, with no hint)
    }
}
