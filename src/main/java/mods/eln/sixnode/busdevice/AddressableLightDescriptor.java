package mods.eln.sixnode.busdevice;

import mods.eln.gl.GL11;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.UtilsClient;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Addressable Light (Baughn 2026-11-05: an RS485 bus device): the LED's model, as bright as the last `power` sent
 * to it (0..1), giving block light up to MAX_LIGHT.
 */
public class AddressableLightDescriptor extends SixNodeDescriptor {
    /** Its block light at full power (a small lamp: the alarm's). */
    public static final int MAX_LIGHT = 7;
    final Obj3D.Obj3DPart main, led, halo;

    public AddressableLightDescriptor(String name, Obj3D obj) {
        super(name, AddressableLightElement.class, null);
        main = obj == null ? null : obj.getPart("main");
        led = obj == null ? null : obj.getPart("Led");
        halo = obj == null ? null : obj.getPart("halo");
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    /** The body and the LED at `level` (0..1): dark red off, through to bright red-orange. */
    void draw(float level, BlockEntity e) {
        if (main != null) main.draw();
        float k = Math.max(0, Math.min(1, level));
        float r = 0.25f + 0.75f * k, g = 0.05f + 0.35f * k, b = 0.05f + 0.1f * k;
        GL11.glColor3f(r, g, b);
        if (led != null) UtilsClient.drawLight(led);
        if (halo != null && k > 0.02f) {
            if (e != null) UtilsClient.drawHalo(halo, r * k, g * k, b * k, e, false);
            else UtilsClient.drawLight(halo);
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("A lamp on the RS485 bus: as bright as the power sent to its address."));
        list.add("  " + tr("Set its address in its screen."));
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(0, null);
    }

    @Override
    public LRDU modelTurn(mods.eln.misc.Direction side, LRDU front) {
        return side.isY() ? front.right() : null;
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, LRDU front) {
        LRDU turn = modelTurn(side, front);
        if (turn != null) turn.glRotateOnX();
        draw(0, null);
    }

    @Override
    protected float[] bodyBounds() {
        return boundsOf(main, led);
    }
}
