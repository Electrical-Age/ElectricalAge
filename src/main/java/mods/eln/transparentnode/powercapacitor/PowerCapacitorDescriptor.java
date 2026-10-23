package mods.eln.transparentnode.powercapacitor;


import mods.eln.misc.Utils;
import mods.eln.Eln;
import mods.eln.item.DielectricItem;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class PowerCapacitorDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;

    public PowerCapacitorDescriptor(
        String name,
        Obj3D obj,
        ISerie serie,
        double dischargeTao

    ) {
        super(name, PowerCapacitorElement.class, null);
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
        double uTemp = nominalDielVoltage / Eln.LVU;
        return serie.getValue(cableCount - 1) / uTemp / uTemp;
    }

    public double getCValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerCapacitorContainer.redId);
        ItemStack diel = inventory.getItem(PowerCapacitorContainer.dielectricId);
        if (Utils.isEmpty(core) || Utils.isEmpty(diel))
            return getCValue(0, 0);
        else {
            return getCValue(core.getCount(), getUNominalValue(inventory));
        }
    }

    public double getUNominalValue(Container inventory) {
        ItemStack diel = inventory.getItem(PowerCapacitorContainer.dielectricId);
        if (Utils.isEmpty(diel))
            return 10000;
        else {
            DielectricItem desc = (DielectricItem) DielectricItem.getDescriptor(diel);
            return desc.uNominal * diel.getCount();
        }
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        //Data.addEnergy(newItemStack());
    }

    void draw() {

    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return false; // its draw() is empty (an invisible block in 1.12 too): the flat icon everywhere
    }

    @Override
    public void drawItem(boolean entity) {
        draw();
    }
}
