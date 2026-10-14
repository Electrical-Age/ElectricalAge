package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (Baughn, oil thread): the small steam turbine, a turbine with its generator built in that puts out 50 or 200 V
 * like the old heat turbine, which it replaces as early-game power (coal -> heat furnace -> boiler -> this). It turns
 * what steam it gets into electricity at {@link #EFFICIENCY} of steam's heat value (2.257 J/mB), up to its nominal
 * power; the stock shaft Steam Turbine (7,200 mB/s) stays for big plants. Model: tools/models/steamturbine.py.
 */
public class SteamGeneratorDescriptor extends TransparentNodeDescriptor {
    /** Electric out per steam heat in: low-pressure steam at ~100 C, generous for the game (Carnot ~21%). */
    public static final double EFFICIENCY = 0.40;
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
        cableTerminals = mods.eln.cable.CableTerminals.FRONT_AND_BACK; // terminal plates front and back (model)
    }

    /** Steam (mB/s) at nominal power. */
    public double nominalSteam() {
        return nominalP / (EFFICIENCY * mods.eln.fluid.FuelRegistry.INSTANCE.heatEnergyPerMilliBucket("steam"));
    }

    void draw() {
        if (obj != null) {
            var p = obj.getPart("main");
            if (p != null) p.draw();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Steam in (any side), %.0f V out (front and back)", nominalU));
        list.add(String.format("Up to %.0f W from %.0f mB/s of steam", nominalP, nominalSteam()));
    }
}
