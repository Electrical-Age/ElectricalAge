package mods.eln.transparentnode.turret;

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

/** Defence turret (P4b, 1.12 TurretGui): attack or spare what the filter matches, and the recharge power. */
public class TurretScreen extends ElnScreen {
    final TurretRender render;

    public TurretScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TurretRender) render;
    }

    EaSlider charge;

    @Override
    protected void init() {
        super.init();
        button(8, 6, 106, () -> render.filterIsSpare ? tr("Do not attack:") : tr("Attack:"), render::clientToggleFilterMeaning,
            tr("What happens to the entities the filter in the slot matches"));
        charge = addRenderableWidget(new EaSlider(leftPos + 8, topPos + 28, 160, 100, 5000, 100, render.chargePower,
            v -> Component.literal(tr("Recharge power: %1$W", (int) v)), v -> render.clientSetChargePower((float) v)));
    }

    @Override
    protected void update() {
        charge.setFromDevice(render.chargePower);
    }
}
