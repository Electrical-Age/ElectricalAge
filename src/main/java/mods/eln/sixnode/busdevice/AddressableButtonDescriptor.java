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

import java.awt.Color;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Addressable Button (Baughn 2026-11-05: an RS485 bus device): the Signal Button's model; a press sends `press`
 * to everyone, from its address, and its lamp follows `power` sent to it (lit: the call was taken).
 */
public class AddressableButtonDescriptor extends SixNodeDescriptor {
    final Obj3D.Obj3DPart main, lever, led, halo;
    final float leverTx;

    public AddressableButtonDescriptor(String name, Obj3D obj) {
        super(name, AddressableButtonElement.class, null, "button");
        main = obj == null ? null : obj.getPart("main");
        lever = obj == null ? null : obj.getPart("button");
        led = obj == null ? null : obj.getPart("led");
        halo = obj == null ? null : obj.getPart("halo");
        leverTx = lever == null ? 0 : lever.getFloat("tx");
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    /** The body, the button pushed in by `pressed` (0..1), and its lamp, lit or not. */
    void draw(float pressed, boolean lit, BlockEntity e) {
        if (main != null) main.draw();
        GL11.glPushMatrix();
        GL11.glTranslatef(leverTx * pressed, 0f, 0f);
        if (lever != null) lever.draw();
        GL11.glPopMatrix();
        UtilsClient.ledOnOffColor(lit);
        UtilsClient.disableLight();
        if (led != null) led.draw();
        UtilsClient.enableBlend();
        if (halo != null) {
            if (e == null) UtilsClient.drawLight(halo);
            else {
                Color c = UtilsClient.ledOnOffColorC(lit);
                UtilsClient.drawHaloNoLightSetup(halo, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, e, false);
            }
        }
        UtilsClient.disableBlend();
        UtilsClient.enableLight();
        GL11.glColor4f(1, 1, 1, 1);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("A button on the RS485 bus: a press is sent to every device, from its address."));
        list.add("  " + tr("Its lamp lights when it is sent power (a call taken). Sneak and click it to set its address."));
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glScalef(1.5f, 1.5f, 1.5f);
        draw(0, false, null);
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, LRDU front) {
        if (side.isY()) front.glRotateOnX();
        else LRDU.Down.glRotateOnX();
        draw(0, false, null);
    }
}
