package mods.eln.transparentnode.turbine;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TurbineDescriptor extends TransparentNodeDescriptor {
    final CableRenderDescriptor eRender;

    public TurbineDescriptor(String name, String modelName, CableRenderDescriptor eRender,
                             FunctionTable TtoU, FunctionTable PoutToPin, double nominalDeltaT, double nominalU,
                             double nominalP, double nominalPowerLost, double electricalRs,
                             double thermalC, double DeltaTForInput,
                             double powerOutPerDeltaU, String soundFile) {
        super(name, TurbineElement.class, TurbineRender.class);
        double nominalEff = Math.abs(1 - (0 + PhysicalConstant.Tref) / (nominalDeltaT + PhysicalConstant.Tref));
        this.TtoU = TtoU;
        this.PoutToPin = PoutToPin;
        this.nominalDeltaT = nominalDeltaT;
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.thermalC = thermalC;
        this.thermalRs = DeltaTForInput / (nominalP / nominalEff);
        this.thermalRp = nominalDeltaT / nominalPowerLost;
        this.electricalRs = electricalRs;
        this.powerOutPerDeltaU = powerOutPerDeltaU;
        this.eRender = eRender;
        this.soundFile = soundFile;
        Obj3D obj = Eln.obj.getObj(modelName);
        if (obj != null) {
            main = obj.getPart("main");
        }

        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalU);
    }

    private Obj3DPart main;

    public final double powerOutPerDeltaU;
    public final FunctionTable TtoU;
    public final FunctionTable PoutToPin;
    public final double nominalDeltaT;
    public final double nominalU;
    final double nominalP;
    private final double thermalC;
    private final double thermalRs;
    private final double thermalRp;
    final double electricalRs;
    public final String soundFile;

    public void applyTo(ThermalLoad load) {
        load.C = thermalC;
        load.Rp = thermalRp;
        load.Rs = thermalRs;
    }

    @Override
    public void setParent(Item item, int damage) {

        super.setParent(item, damage);
        Data.addThermal(newItemStack());
        Data.addEnergy(newItemStack());
    }

    public void applyTo(ElectricalLoad load) {
        load.setRs(electricalRs);
    }

    void draw() {
        if (main != null) main.draw();
    }

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, data);
//        } else {
//            draw();
//        }
//    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Generates electricity using heat."));
        list.add(tr("Nominal usage:"));
        list.add("  " + tr("Temperature difference: %1$°C", Utils.plotValue(nominalDeltaT)));
        list.add("  " + tr("Voltage: %1$V", Utils.plotValue(nominalU)));
        list.add("  " + tr("Power: %1$W", Utils.plotValue(nominalP)));
    }
}
