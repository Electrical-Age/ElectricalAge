package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.gl.GL11;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart main;

    int range;

    public WirelessSignalTxDescriptor(String name,
                                      Obj3D obj,
                                      int range) {
        super(name, WirelessSignalTxElement.class, null);
        this.range = range;
        this.obj = obj;
        if (obj != null) main = obj.getPart("main");

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    public void draw() {
        if (main != null) main.draw();
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
        if (entity) {

            GL11.glScalef(2.8f, 2.8f, 2.8f);
        }
        draw();

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    /** Small: placed anywhere on a face of a voxel block, at its drawn size (Baughn). */
    @Override
    public boolean placeAnywhere() {
        return true;
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, mods.eln.misc.LRDU front) {
        front.glRotateOnX();
        draw();
    }

    @Override
    protected float[] bodyBounds() {
        return boundsOf(main);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, java.util.List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(mods.eln.i18n.I18N.tr("Broadcasts a 0–5 V signal on a named channel (range: %1$ blocks).", range));
    }
}
