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

    /** Where its render draws the hot coils' halo (blocks from the centre, in the model's frame before its turn). */
    float haloX = -0.36f, haloY = -0.1f;

    public ElectricHeaterDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        this(name, ElectricHeaterElement.class, nominalU, nominalP, cable, model);
    }

    /** For a heater with its own element (the Pipe Heater), sharing the regulator, screen and render. */
    @SuppressWarnings("rawtypes")
    protected ElectricHeaterDescriptor(String name, Class element, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, element, ElectricHeaterRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.resistorR = nominalU * nominalU / nominalP;
        this.cable = cable;
        this.obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;   // mods.eln.cad.models.ElectricHeater terminal plates
    }

    void draw() {
        if (obj != null) for (String part : new String[]{"main", "dissipator", "body", "element"}) {
            var p = obj.getPart(part);
            if (p != null) p.draw();
        }
    }

    /** The heating coils alone (part element), drawn again glowing when hot (Incandescence). */
    void drawElement() {
        var p = obj == null ? null : obj.getPart("element");
        if (p != null) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        addLines(list);
    }

    /** Its own tooltip lines, after the common ones. */
    protected void addLines(List<String> list) {
        list.add(String.format("Converts electricity into heat (%.0f W).", nominalP));
        mods.eln.client.ItemTooltips.detail(list, "Accepts a Regulator for temperature control.");
    }
}
