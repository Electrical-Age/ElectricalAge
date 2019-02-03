package mods.eln.sixnode.thermalcable;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ThermalCableDescriptor extends SixNodeDescriptor {

    boolean addToDataEnabled = true;

    double thermalRp = 1, thermalRs = 1, thermalC = 1;

    double thermalWarmLimit, thermalCoolLimit;
    double thermalStdT, thermalStdPower;
    double thermalStdDrop, thermalStdLost;
    double thermalTao;

    String description = "todo cable";

    public CableRenderDescriptor render;

    public static final ThermalCableDescriptor[] list = new ThermalCableDescriptor[256];

    public ThermalCableDescriptor(String name,
                                  double thermalWarmLimit, double thermalCoolLimit,
                                  double thermalStdT, double thermalStdPower,
                                  double thermalStdDrop, double thermalStdLost,
                                  double thermalTao,
                                  CableRenderDescriptor render,
                                  String description) {
        super(name, ThermalCableElement.class, ThermalCableRender.class);

        this.description = description;
        this.render = render;

        this.thermalWarmLimit = thermalWarmLimit;
        this.thermalCoolLimit = thermalCoolLimit;
        this.thermalStdT = thermalStdT;
        this.thermalStdPower = thermalStdPower;
        this.thermalStdDrop = thermalStdDrop;
        this.thermalStdLost = thermalStdLost;
        this.thermalTao = thermalTao;

        thermalRs = thermalStdDrop / 2 / thermalStdPower;
        thermalRp = thermalStdT / thermalStdLost;
        //thermalC = thermalTao / (thermalRs * 2) ;
        thermalC = Eln.simulator.getMinimalThermalC(thermalRs, thermalRp);
        if (!Eln.simulator.checkThermalLoad(thermalRs, thermalRp, thermalC)) {
            Utils.println("Bad thermalCable setup");
            while (true) ;
        }
        voltageLevelColor = VoltageLevelColor.Thermal;
    }

    public void addToData(boolean enable) {
        this.addToDataEnabled = enable;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        if (addToDataEnabled) {
            Data.addWiring(newItemStack());
            Data.addThermal(newItemStack());
        }
    }

    public static ThermalCableDescriptor getDescriptorFrom(ItemStack itemStack) {
        return list[(itemStack.getItemDamage() >> 8) & 0xFF];
    }

    /*
    static void setThermalLoadFrom(ItemStack itemStack, ThermalLoad thermalLoad) {
        if (itemStack == null || itemStack.itemID != Eln.sixNodeBlock.blockID || (itemStack.getItemDamage() & 0xFF) != Eln.electricalCableId) {
            thermalLoad.setHighImpedance();
        } else {
            ThermalCableDescriptor cableDescriptor = ThermalCableDescriptor.list[(itemStack.getItemDamage() >> 8) & 0xFF];
            thermalLoad.Rp = cableDescriptor.thermalRp;
            thermalLoad.Rs = cableDescriptor.thermalRs;
            thermalLoad.C = cableDescriptor.thermalC;
        }
    }
    */
    public void setThermalLoad(ThermalLoad thermalLoad) {
        thermalLoad.Rp = thermalRp;
        thermalLoad.Rs = thermalRs;
        thermalLoad.C = thermalC;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);

        list.add(tr("Max. temperature: %1$°C", Utils.plotValue(thermalWarmLimit)));
        list.add(tr("Serial resistance: %1$K/W", Utils.plotValue(thermalRs * 2)));
        list.add(tr("Parallel resistance: %1$K/W", Utils.plotValue(thermalRp)));
        list.add("");
        Collections.addAll(list, tr("Low serialized resistance\n => High conductivity.").split("\n"));
        Collections.addAll(list, tr("High parallel resistance\n => Low power dissipation.").split("\n"));
    }
}
