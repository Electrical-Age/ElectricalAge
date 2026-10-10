package mods.eln.transparentnode.thermaldissipatoractive;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ThermalDissipatorActiveDescriptor extends TransparentNodeDescriptor {

    double nominalP, nominalT;
    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart rot;

    public ThermalDissipatorActiveDescriptor(
        String name,
        Obj3D obj,
        double nominalElectricalU, double electricalNominalP,
        double nominalElectricalCoolingPower,
        ElectricalCableDescriptor cableDescriptor,
        double warmLimit, double coolLimit,
        double nominalP, double nominalT,
        double nominalTao, double nominalConnectionDrop
    ) {
        super(name, ThermalDissipatorActiveElement.class, null);
        this.cableDescriptor = cableDescriptor;
        this.electricalNominalP = electricalNominalP;
        this.nominalElectricalU = nominalElectricalU;
        this.nominalElectricalCoolingPower = nominalElectricalCoolingPower;
        electricalRp = nominalElectricalU * nominalElectricalU / electricalNominalP;
        electricalToThermalRp = nominalT / nominalElectricalCoolingPower;
        thermalC = (nominalP + nominalElectricalCoolingPower) * nominalTao / nominalT;
        thermalRp = nominalT / nominalP;
        thermalRs = nominalConnectionDrop / (nominalP + nominalElectricalCoolingPower);
        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);
        this.coolLimit = coolLimit;
        this.warmLimit = warmLimit;
        this.nominalP = nominalP;
        this.nominalT = nominalT;
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            rot = obj.getPart("rot");
        }

        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cableDescriptor);
    }

    double warmLimit, coolLimit;
    double nominalElectricalU;
    double nominalElectricalCoolingPower;

    public void applyTo(ThermalLoad load) {
        load.set(thermalRs, thermalRp, thermalC);
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addThermal(newItemStack());
    }

    public double thermalRs, thermalRp, thermalC;
    double electricalRp;
    double electricalToThermalRp;
    public double electricalNominalP;
    ElectricalCableDescriptor cableDescriptor;

    public void applyTo(ElectricalLoad load, Resistor r) {
        cableDescriptor.applyTo(load);
        r.setR(electricalRp);
    }
// 26.x port: draw() removed (client rendering; redone in P3)


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer,
                               List list, boolean par4) {

        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Used to cool down turbines."));
        list.add(tr("Max. temperature: %1$°C", Utils.plotValue(warmLimit)));
        list.add(tr("Nominal usage:"));
        list.add("  " + tr("Temperature: %1$°C", Utils.plotValue(nominalT)));
        list.add("  " + tr("Cooling power: %1$W", Utils.plotValue(nominalP)));
        list.add("  " + tr("Fan voltage: %1$V", Utils.plotValue(nominalElectricalU)));
        list.add("  " + tr("Fan power consumption: %1$W", Utils.plotValue(electricalNominalP)));
        list.add("  " + tr("Fan cooling power: %1$W", Utils.plotValue(nominalElectricalCoolingPower)));

    }
}
