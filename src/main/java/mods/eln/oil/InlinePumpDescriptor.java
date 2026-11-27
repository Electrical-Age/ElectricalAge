package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The inline pump (Baughn 2026-10-29, the shaft redo): an electric centrifugal pump in a pipe line, for feedwater and
 * condensate. Water in on its left flank, out of its right; power on its front, a speed signal on its back (0-5 V;
 * none: full speed). Head goes with the square of its speed (up to HEAD_PA at full), flow with its speed; the power it
 * draws with what it moves. It needs water at its inlet with some margin over boiling (NPSH_PA): fed vapour, or water
 * at its boiling point, it cavitates: it moves next to nothing and rattles (so it goes after the condenser or cooler,
 * not after a preheater: Argon). Model: mods.eln.cad.models.InlinePump.
 */
public class InlinePumpDescriptor extends TransparentNodeDescriptor {
    /**
     * Placed with its line along the player's view, water in behind and out ahead (AdventurAgent 2026-10-29: placed
     * looking along a pipe run, its line came out across it): its front to the player's right (EA's left() of the view),
     * so its left (the inlet) is behind them and its right (the outlet) ahead.
     */
    @Override
    public mods.eln.misc.Direction getFrontFromPlace(mods.eln.misc.Direction side, net.minecraft.world.entity.LivingEntity entityLiving) {
        return alongView(entityLiving);
    }

    /** The front that puts a line through its flanks along the player's view (the Pipe Heater's too). */
    static mods.eln.misc.Direction alongView(net.minecraft.world.entity.LivingEntity entityLiving) {
        return entityLiving == null ? mods.eln.misc.Direction.XN : mods.eln.misc.Utils.entityLivingHorizontalViewDirection(entityLiving).left();
    }

    /** Shutoff head (Pa), the flow at no head (kg/s), electric power at full duty (W), efficiency, idle draw (W). */
    public static final double HEAD_PA = 25e5, RATED_KG_PER_S = 0.1, NOMINAL_P = 500, EFFICIENCY = 0.6, IDLE_W = 40;
    /** The margin over the water's vapour pressure it needs at its inlet (Pa), and the liquid share of the inlet. */
    public static final double NPSH_PA = 0.2e5, MIN_LIQUID = 0.5;
    final double nominalU;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public InlinePumpDescriptor(String name, double nominalU, ElectricalCableDescriptor cable, String model) {
        this(name, InlinePumpElement.class, nominalU, cable, model);
    }

    /** For a machine sharing its model and render (the Air Blower's stand-in). */
    @SuppressWarnings("rawtypes")
    protected InlinePumpDescriptor(String name, Class element, double nominalU, ElectricalCableDescriptor cable, String model) {
        super(name, element, InlinePumpRender.class);
        this.nominalU = nominalU;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.ALL_SIDES;
    }

    void draw(float angle) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        var r = obj.getPart("rotor");
        if (r != null) r.draw(angle, 1, 0, 0);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Inline booster pump: up to %.0f mB/s, %.0f bar head (%.0f W).",
            RATED_KG_PER_S / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER, HEAD_PA / 1e5, NOMINAL_P));
        list.add("Liquid only — cavitates on steam or boiling water.");
        mods.eln.client.ItemTooltips.detail(list,
            "Places along your view (inlet behind you, outlet ahead). 0–5 V speed signal.");
    }
}
