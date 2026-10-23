package mods.eln.transparentnode.electricalfurnace;

import mods.eln.Eln;
import mods.eln.misc.IFunction;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalFurnaceDescriptor extends TransparentNodeDescriptor {

    public IFunction PfT, thermalPlostfT;
    public double thermalC;
    //public double thermalRp;
    //ThermalLoadInitializer thermal;

    public ElectricalFurnaceDescriptor(String name, IFunction PfT, IFunction thermalPlostfT, double thermalC) {
        super(name, ElectricalFurnaceElement.class, null);
        this.PfT = PfT;
        this.thermalPlostfT = thermalPlostfT;
        this.thermalC = thermalC;

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public void applyTo(ThermalLoad load) {
        load.set(Double.POSITIVE_INFINITY, thermalPlostfT.getValue(0), thermalC);
    }

    public void refreshTo(ThermalLoad load, double conductionFactor) {
        double Rp = (load.Tc / thermalPlostfT.getValue(load.Tc)) / conductionFactor;
        if (Rp < 0.1) Rp = 0.1;
        load.setRp(Rp);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        Eln.obj.draw("ElectricFurnace", "furnace");

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Similar to a vanilla furnace,\nbut heats with electricity.").split("\n"));
    }
}
