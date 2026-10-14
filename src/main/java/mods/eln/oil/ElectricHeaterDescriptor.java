package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (notes/oil): the electric heater. A resistor whose whole power goes to its thermal face (the back); heat a
 * boiler or a fractionating column with it. Resistive heating is 100% efficient, but electricity -> heat -> steam ->
 * turbine is Carnot-limited, so it never breaks even. It has a regulator slot (on/off or analog) and a set point; without one it heats at full power, and with nothing to heat it burns above 800 °C.
 * Placeholder model: the large rheostat's dissipator.
 */
public class ElectricHeaterDescriptor extends TransparentNodeDescriptor {
    final double nominalU, nominalP, resistorR;
    final ElectricalCableDescriptor cable;
    final Obj3D obj;

    public ElectricHeaterDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, ElectricHeaterElement.class, ElectricHeaterRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.resistorR = nominalU * nominalU / nominalP;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // tools/models/electricheater.py terminal plates
    }

    void draw() {
        if (obj != null) for (String part : new String[]{"main", "dissipator", "body"}) {
            var p = obj.getPart(part);
            if (p != null) p.draw();
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("%.0f V, %.0f W of heat out of its back (thermal)", nominalU, nominalP));
    }
}
