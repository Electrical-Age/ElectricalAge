package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x oil (notes/oil): the pump. Sits on a column of well casing; if the casing's bottom reaches an oil lake it pumps
 * from the lake's record, if it reaches water it pumps water (Baughn: "yes", for the boiler). It runs at its nominal
 * power while its tank has room; the flow is power / energy per mB: lift (m g h at EA's fuel scale, tiny) plus 5% of
 * crude's heat value (the overhead), or a flat 20 J/mB for water. Placeholder model: the compressor's.
 */
public class OilPumpDescriptor extends TransparentNodeDescriptor {
    /** Real litres per game mB (design v2's litresPerMilliBucket, = Eln.fuelHeatValueFactor). */
    public static final double LITRES_PER_MB = 6.75e-5;
    public static final double CRUDE_KG_PER_L = 0.85, WATER_KG_PER_L = 1.0;
    /** Crude's heat value per mB (38 MJ/L real), and the pumping overhead: 5% of it. */
    public static final double CRUDE_J_PER_MB = 38e6 * LITRES_PER_MB, OVERHEAD = 0.05;
    public static final double WATER_J_PER_MB = 20;
    public static final int TANK_MB = 8000;

    final double nominalU, nominalP, resistorR;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public OilPumpDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, OilPumpElement.class, OilPumpRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.resistorR = nominalU * nominalU / nominalP;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.ALL_SIDES;   // tools/models/oilpump.py terminal plates
    }

    /** Energy (J) to lift one mB by h blocks and push it out. */
    public static double energyPerMb(boolean water, double h) {
        double mass = (water ? WATER_KG_PER_L : CRUDE_KG_PER_L) * LITRES_PER_MB;
        double lift = mass * 9.81 * Math.max(0, h);
        return lift + (water ? WATER_J_PER_MB : OVERHEAD * CRUDE_J_PER_MB);
    }

    @Override
    public boolean mustHaveFloor() {
        return true;
    }

    void draw() {
        if (obj != null) {
            var main = obj.getPart("main");
            if (main != null) main.draw();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("%.0f V, %.0f W: about %.0f buckets of crude an hour", nominalU, nominalP,
            nominalP / energyPerMb(false, 40) * 3.6));
        list.add("Place on well casing that reaches an oil lake or water");
    }
}
