package mods.eln.transparentnode.turret;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Defence turret (P4b; layout gui/turret.json): attack or spare what the filter matches, and the recharge power. */
public class TurretScreen extends ElnScreen {
    final TurretRender render;

    public TurretScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TurretRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button("meaning", () -> render.filterIsSpare ? tr("Mode: Ignore filtered") : tr("Mode: Target filtered"),
            render::clientToggleFilterMeaning, tr("Invert filter target"));
        charge = slider("charge", 100, 5000, 100, render.chargePower,
            v -> Component.literal(tr("Recharge power: %1$", mods.eln.api.Meters.watts(v))), v -> render.clientSetChargePower((float) v));
    }

    EaSlider charge;

    @Override
    protected void update() {
        charge.setFromDevice(render.chargePower);
    }
}
