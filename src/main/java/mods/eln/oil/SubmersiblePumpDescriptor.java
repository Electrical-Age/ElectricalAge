package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The submersible pump (Baughn 2026-10-25): a small electric pump that stands in water (it must be waterlogged) and
 * pushes it up a pipe from the flange on its top, for a boiler's feed. Its nominal flow is a few times what a turbine of
 * its voltage turns into steam (NOMINAL_TURBINES), falling to nothing at SHUTOFF_PA against the pipe. Power on its
 * flanks; a signal on its front sets its speed (no cable there: full speed). Model: tools/models/submersiblepump.py.
 */
public class SubmersiblePumpDescriptor extends TransparentNodeDescriptor {
    /** Nominal flow, in turbines' worth of feedwater (a turbine of the same voltage at full power). */
    public static final double NOMINAL_TURBINES = 3.5;
    /**
     * Pressure against which the flow falls to nothing (Pa, absolute): a small submersible pump's ~20 m of head
     * (Baughn: 9 bar was a pressure washer's). A running boiler sits near 1.3 bar, so it still feeds one well.
     */
    public static final double SHUTOFF_PA = 3e5;

    final double nominalU, nominalP, resistorR;
    /** Nominal flow (mB of water a second). */
    public final double nominalMbPerS;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public SubmersiblePumpDescriptor(String name, double nominalU, double nominalP, double turbineP, ElectricalCableDescriptor cable, String model) {
        super(name, SubmersiblePumpElement.class, SubmersiblePumpRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.resistorR = nominalU * nominalU / nominalP;
        // a turbine of turbineP: turbineP / (EFFICIENCY x 2.257 J/mB) mB/s of steam, 67.5 mB of steam per mB of water
        double steam = turbineP / (SteamGeneratorDescriptor.EFFICIENCY * mods.eln.fluid.FuelRegistry.INSTANCE.heatEnergyPerMilliBucket("steam"));
        this.nominalMbPerS = NOMINAL_TURBINES * steam * EaUnits.KG_PER_MB_STEAM / EaUnits.KG_PER_MB_WATER;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FRONT_AND_FLANKS;
    }

    /** It must stand in a water source (Baughn); and, as every device, on a floor. */
    @Override
    public String checkCanPlace(Coordonate coord, Direction front) {
        var f = coord.world().getFluidState(coord.pos());
        if (!f.is(Fluids.WATER) || !f.isSource()) return tr("A submersible pump goes in water");
        return super.checkCanPlace(coord, front);
    }

    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("%.0f V, %.0f W: up to %.0f mB/s of water", nominalU, nominalP, nominalMbPerS));
        Collections.addAll(list, tr("Place it in water; the pipe goes on top.\nPower on its sides, a signal on its front sets its speed.").split("\n"));
    }
}
