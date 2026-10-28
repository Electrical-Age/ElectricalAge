package mods.eln.oil;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** The steam oven (layout gui/steam_oven.json): input -> progress -> output, the steam's pressure and the fan's power. */
public class SteamOvenScreen extends ElnScreen {
    final SteamOvenRender render;

    public SteamOvenScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (SteamOvenRender) render;
    }

    @Override
    protected void init() {
        super.init();
        progress("process", () -> render.progress);
        text("steam", () -> tr("Steam %1$ bar, using %2$ mB/s", String.format("%.2f", render.pressureBar), String.format("%.0f", render.steamMbS)));
        text("power", () -> render.powered ? tr("Fan: powered") : tr("Fan: no power (50V on a side)"),
            () -> render.powered ? LABEL : 0xFFA02020);   // (ARGB: alpha 0 drew nothing)
    }
}
