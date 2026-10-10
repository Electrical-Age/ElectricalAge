package mods.eln.sixnode.batterycharger;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaGauge;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Battery charger (P4b, 1.12 BatteryChargerGui): charging on/off. */
public class BatteryChargerScreen extends ElnScreen {
    final BatteryChargerRender render;

    public BatteryChargerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (BatteryChargerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button(100, 13, 68, () -> render.powerOn ? tr("Charging") : tr("Off"), () -> render.clientSend(BatteryChargerElement.toogleCharge));
    }
}
