package mods.eln.oil;

import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The air blower (todo #471): an electric blower taking air from around it and pushing it into the pipe on its right
 * flank, for forced-draught burners (a Fuel Heat Furnace's air port, todo #504: its damper passes FORCED_MAX x the air
 * of the burner's setting at DAMPER_DP, as the square root of the pressure across it). A centrifugal machine: its pressure falls with its flow from SHUTOFF_PA (blocked, it stalls
 * there) to none at FREE_M3_S. Sized for the mid game: one blower feeds two Big burners at ~1.5 bar (20 g/s of air,
 * ~2.8 kW) or, slowed by its speed signal, less air at a lower pressure for a few hundred watts. It draws the work it does on the air (the core's flow x rise at the inlet's density, about 1.45 x the
 * isentropic work at 1.6 bar: an isentropic efficiency near 0.7) over its motor's efficiency, plus its idling.
 * Its model (mods.eln.cad.models.AirBlower): the Inline Pump's machine with an intake grille on its left flank; the
 * Inline Pump's render.
 */
public class AirBlowerDescriptor extends InlinePumpDescriptor {
    /** Shut-off pressure rise (Pa), free flow (m^3/s of inlet air), motor efficiency, idle draw (W). */
    public static final double SHUTOFF_PA = 1.8e5, FREE_M3_S = 0.04, MOTOR_ETA = 0.9, BLOWER_IDLE_W = 20;
    /**
     * Air moved per watt against a plain fan (todo #504, Baughn 2026-11-17: "what about increasing the blower's output
     * per watt?"): the work it does on the air is drawn divided by this (its curve, pressure and flow, unchanged; its
     * idling not scaled). 1: the physical machine (motor 0.9, the core's flow x rise ~1.45 x isentropic). 2: pending
     * Baughn's choice (the table in the commit that set it).
     */
    public static double OUTPUT_PER_WATT = 2;

    public AirBlowerDescriptor(String name, double nominalU, ElectricalCableDescriptor cable, String model) {
        super(name, AirBlowerElement.class, nominalU, cable, model);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        list.add("Blows air from around it into the pipe on its right:");
        list.add(String.format("up to %.1f bar gauge (blocked, it stalls there), less the more it moves.", SHUTOFF_PA / 1e5));
        list.add("For forced draught: piped to a Fuel Heat Furnace's air port, ~1.4 bar fires it 2.5x its setting.");
        list.add("Feeds two Big burners at ~1.5 bar for ~2.8 kW; slowed down, less air for less power.");
        list.add(String.format("%.0f V on its front; it draws power with the air it moves. A speed signal on its back sets its pressure.", nominalU));
    }
}
