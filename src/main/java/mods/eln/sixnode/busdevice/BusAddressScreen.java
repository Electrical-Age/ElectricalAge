package mods.eln.sixnode.busdevice;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** A bus device's screen (layout gui/bus_address.json): its address, 17 to 255, and whether it is on a bus. */
public class BusAddressScreen extends ElnScreen {
    final BusDeviceRender render;

    public BusAddressScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (BusDeviceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("addressl", () -> tr("Address"));
        field("address", () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.sendAddress(Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its bus address, 17 to 255 (1 to 16 are the colour channels). Two devices on a bus shouldn't share one."));
        text("status", () -> render.address == 0 ? tr("No address") : render.onBus ? tr("On a bus") : tr("No bus"),
            () -> render.address != 0 && render.onBus ? 0xFF207020 : LABEL);
    }
}
