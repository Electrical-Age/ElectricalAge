package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.gl.GL11;


import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import mods.eln.wiki.Data;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WirelessSignalSourceDescriptor extends SixNodeDescriptor {

    int range;
    public boolean autoReset;
    ElectricalGateSourceRenderObj render;

    public WirelessSignalSourceDescriptor(String name,
                                          ElectricalGateSourceRenderObj render,
                                          int range, boolean autoReset) {
        super(name, WirelessSignalSourceElement.class, null);
        this.range = range;
        this.autoReset = autoReset;
        this.render = render;

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }
    void draw(float factor, float distance, BlockEntity e) {
        render.draw(factor, distance, e);
    }
    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glScalef(1.5f, 1.5f, 1.5f);
        draw(0f, 1f, null);

    }
}
