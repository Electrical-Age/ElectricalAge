package mods.eln.sixnode.wirelesssignal.repeater;

import mods.eln.gl.GL11;

import mods.eln.misc.UtilsClient;

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

public class WirelessSignalRepeaterDescriptor extends SixNodeDescriptor {

    public int range;
    private Obj3D obj;
    Obj3DPart main, led;

    public WirelessSignalRepeaterDescriptor(String name,
                                            Obj3D obj,
                                            int range) {
        super(name, WirelessSignalRepeaterElement.class, null);
        this.range = range;
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            led = obj.getPart("led");
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

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

    public void draw() {
        if (main != null) main.draw();

		/*if (led != null) {
			UtilsClient.ledOnOffColor(connection);
			UtilsClient.drawLight(led);
		}*/
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
