package mods.eln.sixnode.powercapacitorsix;

import mods.eln.gl.GL11;


import mods.eln.misc.Utils;
import mods.eln.Eln;
import mods.eln.item.DielectricItem;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class PowerCapacitorSixDescriptor extends SixNodeDescriptor {

    private Obj3D obj;

    private Obj3DPart CapacitorCore;
    private Obj3DPart CapacitorCables;
    private Obj3DPart Base;

    ISerie serie;
    public double dischargeTao;

    public PowerCapacitorSixDescriptor(String name,
                                       Obj3D obj,
                                       ISerie serie,
                                       double dischargeTao) {
        super(name, PowerCapacitorSixElement.class, null);
        this.serie = serie;
        this.dischargeTao = dischargeTao;
        this.obj = obj;
        if (obj != null) {
            CapacitorCables = obj.getPart("CapacitorCables");
            CapacitorCore = obj.getPart("CapacitorCore");
            Base = obj.getPart("Base");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public double getCValue(int cableCount, double nominalDielVoltage) {
        if (cableCount == 0) return 1e-6;
        double uTemp = nominalDielVoltage / Eln.LVU;
        return serie.getValue(cableCount - 1) / uTemp / uTemp;
    }

    public double getCValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerCapacitorSixContainer.redId);
        ItemStack diel = inventory.getItem(PowerCapacitorSixContainer.dielectricId);
        if (Utils.isEmpty(core) || Utils.isEmpty(diel))
            return getCValue(0, 0);
        else {
            return getCValue(core.getCount(), getUNominalValue(inventory));
        }
    }

    public double getUNominalValue(Container inventory) {
        ItemStack diel = inventory.getItem(PowerCapacitorSixContainer.dielectricId);
        if (Utils.isEmpty(diel))
            return 10000;
        else {
            DielectricItem desc = (DielectricItem) DielectricItem.getDescriptor(diel);
            return desc.uNominal * diel.getCount();
        }
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {
        if (null != Base) Base.draw();
        if (null != CapacitorCables) CapacitorCables.draw();
        if (null != CapacitorCore) CapacitorCore.draw();
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(0.0f, 0.0f, -0.2f);
        GL11.glScalef(1.25f, 1.25f, 1.25f);
        GL11.glRotatef(-90.f, 0.f, 1.f, 0.f);
        draw();

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
