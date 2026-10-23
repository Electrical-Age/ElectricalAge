package mods.eln.transparentnode.smelter;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** The smelter (layout gui/smelter.json): input -> progress -> output, the temperature and whether its controls have power. */
public class SmelterScreen extends ElnScreen {
    final SmelterRender render;

    public SmelterScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (SmelterRender) render;
    }

    @Override
    protected void init() {
        super.init();
        progress("process", () -> render.progress);
        text("temperature", () -> tr("Temperature %1$", Utils.plotValue(render.temperature + PhysicalConstant.Tamb) + "°C"));
        text("power", () -> render.powered ? tr("Controls: powered") : tr("Controls: no power (50V on a side)"),
            () -> render.powered ? 0x404040 : 0xA02020);
    }
}
