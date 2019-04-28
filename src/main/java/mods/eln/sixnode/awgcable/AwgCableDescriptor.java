package mods.eln.sixnode.awgcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class AwgCableDescriptor extends SixNodeDescriptor {

    public CableRenderDescriptor render;

    double electricalRs;
    double electricalMaxI;

    double thermalC;
    double thermalRp;
    double thermalRs;
    double thermalWarmLimit;
    double thermalCoolLimit;

    public AwgCableDescriptor(String name, CableRenderDescriptor render) {
        super(name, AwgCableElement.class, AwgCableRender.class);

        this.render = render;

    }

    public void setCableType(double cableAwg, double insulationThickness, double thermalWarmLimit, double thermalCoolLimit, double thermalNominalHeatTime, double thermalConductivityTao) {

        //TODO: Base on the cableAwg
        electricalMaxI = cableAwg;

        electricalRs = (electricalMaxI / 2000);

        this.thermalWarmLimit = thermalWarmLimit;
        this.thermalCoolLimit = thermalCoolLimit;

        double thermalMaximalPowerDissipated = electricalMaxI * electricalMaxI * electricalRs * 2;
        thermalC = (thermalMaximalPowerDissipated * thermalNominalHeatTime) / thermalWarmLimit;
        thermalRp = thermalWarmLimit / thermalMaximalPowerDissipated;
        thermalRs = thermalConductivityTao / thermalC / 2;

        Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC);

        voltageLevelColor = VoltageLevelColor.None;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    public void applyTo(ElectricalLoad electricalLoad, double rsFactor) {
        electricalLoad.setRs(electricalRs * rsFactor);
    }

    public void applyTo(ElectricalLoad electricalLoad) {
        applyTo(electricalLoad, 1);
    }

    public void applyTo(Resistor resistor) {
        applyTo(resistor, 1);
    }

    public void applyTo(Resistor resistor, double factor) {
        resistor.setR(electricalRs * factor);
    }

    public void applyTo(ThermalLoad thermalLoad) {
        thermalLoad.Rs = this.thermalRs;
        thermalLoad.C = this.thermalC;
        thermalLoad.Rp = this.thermalRp;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Nominal Ratings:"));
        list.add("  " + tr("Current: %1$A", Utils.plotValue(electricalMaxI)));
        list.add("  " + tr("Serial resistance: %1$\u2126", Utils.plotValue(electricalRs * 2)));
    }

    public int getNodeMask() {
        return NodeBase.maskElectricalPower;
    }

    public static CableRenderDescriptor getCableRender(ItemStack cable) {
        if (cable == null) return null;
        GenericItemBlockUsingDamageDescriptor desc = ElectricalCableDescriptor.getDescriptor(cable);
        if (desc instanceof ElectricalCableDescriptor)
            return ((ElectricalCableDescriptor) desc).render;
        else
            return null;
    }

    public void bindCableTexture() {
        this.render.bindCableTexture();
    }
}
