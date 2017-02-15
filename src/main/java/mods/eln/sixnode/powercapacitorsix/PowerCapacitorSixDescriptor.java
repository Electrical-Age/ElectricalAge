package mods.eln.sixnode.powercapacitorsix;

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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

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
        super(name, PowerCapacitorSixElement.class, PowerCapacitorSixRender.class);
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

    @Override
    public boolean use2DIcon() {
        return true;
    }

    public double getCValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerCapacitorSixContainer.redId);
        ItemStack diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId);
        if (core == null || diel == null)
            return getCValue(0, 0);
        else {
            return getCValue(core.stackSize, getUNominalValue(inventory));
        }
    }

    public double getUNominalValue(IInventory inventory) {
        ItemStack diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId);
        if (diel == null)
            return 10000;
        else {
            DielectricItem desc = (DielectricItem) DielectricItem.getDescriptor(diel);
            return desc.uNominal * diel.stackSize;
        }
    }

    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {
        if (null != Base) Base.draw();
        if (null != CapacitorCables) CapacitorCables.draw();
        if (null != CapacitorCore) CapacitorCore.draw();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type != ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f);
            GL11.glScalef(1.25f, 1.25f, 1.25f);
            GL11.glRotatef(-90.f, 0.f, 1.f, 0.f);
            draw();
        } else {
            super.renderItem(type, item, data);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
