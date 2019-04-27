package mods.eln.transparentnode.powercapacitor;

import mods.eln.Eln;
import mods.eln.init.Cable;
import mods.eln.item.DielectricItem;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class PowerCapacitorDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;

    public PowerCapacitorDescriptor(
        String name,
        Obj3D obj,
        ISerie serie,
        double dischargeTao

    ) {
        super(name, PowerCapacitorElement.class, PowerCapacitorRender.class);
        this.serie = serie;
        this.dischargeTao = dischargeTao;
        this.obj = obj;
        if (obj != null) {

        }

    }

    ISerie serie;
    public double dischargeTao;

    public double getCValue(int cableCount, double nominalDielVoltage) {
        if (cableCount == 0) return 0;
        double uTemp = nominalDielVoltage / Cable.LVU;
        return serie.getValue(cableCount - 1) / uTemp / uTemp;
    }

    public double getCValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerCapacitorContainer.redId);
        ItemStack diel = inventory.getStackInSlot(PowerCapacitorContainer.dielectricId);
        if (core.isEmpty() || diel.isEmpty())
            return getCValue(0, 0);
        else {
            return getCValue(core.getCount(), getUNominalValue(inventory));
        }
    }

    public double getUNominalValue(IInventory inventory) {
        ItemStack diel = inventory.getStackInSlot(PowerCapacitorContainer.dielectricId);
        if (diel.isEmpty())
            return 10000;
        else {
            DielectricItem desc = (DielectricItem) DielectricItem.getDescriptor(diel);
            return desc.uNominal * diel.getCount();
        }
    }

    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {

    }

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//                                         ItemRendererHelper helper) {
//        return true;
//    }
//
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        draw();
//    }
}
