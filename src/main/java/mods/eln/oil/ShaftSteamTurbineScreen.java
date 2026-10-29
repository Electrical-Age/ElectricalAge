package mods.eln.oil;

import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The shaft Steam Turbine's screen (layout gui/shaft_steam_turbine.json): the 50V/200V turbines' controls, and
 * automatic or manual control (Baughn 2026-10-29: manual takes the governor and the overspeed trip off).
 */
public class ShaftSteamTurbineScreen extends SteamGeneratorScreen {
    public ShaftSteamTurbineScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, render);
    }

    @Override
    protected void init() {
        super.init();
        button("manual", () -> render.manual ? tr("Manual: no governor, no overspeed trip!") : tr("Automatic: governor and overspeed trip"),
            render::clientToggleManual);
    }
}
