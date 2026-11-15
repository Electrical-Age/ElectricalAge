package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The air blower (todo #471, prototype 2026-11-15): an electric blower taking air from around it and pushing it into
 * the pipe on its right flank, for pressure-fed burners (FuelHeatFurnace's OVERFIRE_PA). Power on its front. A
 * centrifugal machine: its pressure falls with its flow from SHUTOFF_PA (blocked, it stalls there: under the furnace's
 * 3 bar relief, so it cannot burst a tank) to none at FREE_M3_S. It draws the work it does on the air (the core's
 * flow x rise at the inlet's density, about 1.45 x the isentropic work at 1.6 bar: an isentropic efficiency near
 * 0.7) over its motor's efficiency, plus its idling. Stand-in model and render: the Inline Pump's.
 */
public class AirBlowerDescriptor extends InlinePumpDescriptor {
    /** Shut-off pressure rise (Pa), free flow (m^3/s of inlet air), motor efficiency, idle draw (W). */
    public static final double SHUTOFF_PA = 2.0e5, FREE_M3_S = 0.4, MOTOR_ETA = 0.9, BLOWER_IDLE_W = 40;

    public AirBlowerDescriptor(String name, double nominalU, ElectricalCableDescriptor cable, String model) {
        super(name, AirBlowerElement.class, nominalU, cable, model);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        list.add("Blows air from around it into the pipe on its right:");
        list.add(String.format("up to %.1f bar gauge (blocked, it stalls there), less the more it moves.", SHUTOFF_PA / 1e5));
        list.add("For pressure-fed burners: 1 bar on a Fuel Heat Furnace's tank doubles its fire.");
        list.add(String.format("%.0f V on its front; it draws power with the air it moves.", nominalU));
    }
}
