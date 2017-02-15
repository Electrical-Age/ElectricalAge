package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.wiki.Data;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class WirelessSignalSourceDescriptor extends SixNodeDescriptor {

    int range;
    public boolean autoReset;
    ElectricalGateSourceRenderObj render;

    public WirelessSignalSourceDescriptor(String name,
                                          ElectricalGateSourceRenderObj render,
                                          int range, boolean autoReset) {
        super(name, WirelessSignalSourceElement.class, WirelessSignalSourceRender.class);
        this.range = range;
        this.autoReset = autoReset;
        this.render = render;

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw(float factor, float distance, TileEntity e) {
        render.draw(factor, distance, e);
    }

    @Override
    public boolean use2DIcon() {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glScalef(1.5f, 1.5f, 1.5f);
            draw(0f, 1f, null);
        }
    }
}
