package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.misc.UtilsClient;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class WirelessSignalRxDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart main, led;

    public WirelessSignalRxDescriptor(String name, Obj3D obj) {
        super(name, WirelessSignalRxElement.class, null);

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

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    public void draw(boolean connection) {
        if (main != null) main.draw();

        if (led != null) {
            UtilsClient.ledOnOffColor(connection);
            UtilsClient.drawLight(led);
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
