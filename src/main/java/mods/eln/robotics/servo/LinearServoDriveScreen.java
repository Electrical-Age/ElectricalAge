package mods.eln.robotics.servo;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** The Linear Servo Drive's screen (layout gui/servo_drive.json): its RS485 address, the car panel's first address, whether it is on a bus. */
public class LinearServoDriveScreen extends ElnScreen {
    final LinearServoDriveRender render;

    public LinearServoDriveScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (LinearServoDriveRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("addressl", () -> tr("Address"));
        field("address", () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(LinearServoDriveElement.setAddressId, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its bus address, 17 to 255 (1 to 16 are the colour channels). Its registers: 0 target, 1 position, 2 speed limit, 3 state, 4 load."));
        text("panell", () -> tr("Car panel from"));
        field("panel", () -> render.panelBase == 0 ? "" : String.valueOf(render.panelBase), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(LinearServoDriveElement.setPanelBaseId, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("The car panel's first address: its 12 slots, left to right and top to bottom, have this address and the 11 after it."));
        text("status", () -> render.address == 0 ? tr("No address") : render.onBus ? tr("On a bus") : tr("No bus"),
            () -> render.address != 0 && render.onBus ? 0xFF207020 : LABEL);
    }
}
