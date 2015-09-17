package mods.eln.sixnode.resistor;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import java.util.List;

/**
 * Created by svein on 05/08/15.
 */
public class ResistorDescriptor extends SixNodeDescriptor {

    public final boolean isRheostat;
    public double thermalCoolLimit = -100;
    public double thermalWarmLimit = Eln.cableWarmLimit;
    public double thermalMaximalPowerDissipated = 1000;
    public double thermalNominalHeatTime = 120;
    public double thermalConductivityTao = Eln.cableThermalConductionTao;
    public double tempCoef;
    Obj3D.Obj3DPart ResistorBaseExtension, ResistorCore, ResistorTrack, Base;
    ISerie series;
    private Obj3D obj;


    public ResistorDescriptor(String name,
                              Obj3D obj,
                              ISerie series,
                              double tempCoef,
                              boolean isRheostat) {
        super(name, ResistorElement.class, ResistorRender.class);
        this.obj = obj;
        this.series = series;
        this.tempCoef = tempCoef;
        this.isRheostat = isRheostat;
        if (obj != null) {
            ResistorBaseExtension = obj.getPart("ResistorBaseExtention");
            ResistorCore = obj.getPart("ResistorCore");
            ResistorTrack = obj.getPart("ResistorTrack");
            Base = obj.getPart("Base");
        }
    }

    @Override
    public boolean use2DIcon() {
        return false;
    }

    public double getRsValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(ResistorContainer.coreId);

        if (core == null) return series.getValue(0);
        return series.getValue(core.stackSize);
    }

    @Override
    public void setParent(net.minecraft.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {
        //UtilsClient.disableCulling();
        //UtilsClient.disableTexture();
        if (null != Base) Base.draw();
        if (null != ResistorBaseExtension) ResistorBaseExtension.draw();
        if (null != ResistorCore) ResistorCore.draw();
        if (isRheostat) {
            ResistorTrack.draw();
        }
    }

    @Override
    public boolean shouldUseRenderHelper(IItemRenderer.ItemRenderType type, ItemStack item, IItemRenderer.ItemRendererHelper helper) {
        return true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, IItemRenderer.ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(IItemRenderer.ItemRenderType type, ItemStack item, Object... data) {
        draw();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
    }
}
