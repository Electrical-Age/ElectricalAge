package mods.eln.transparentnode.eggincubator;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Egg incubator (P4b; layout gui/egg_incubator.json): the egg slot and the supply voltage. */
public class EggIncubatorScreen extends ElnScreen {
    final EggIncubatorRender render;

    public EggIncubatorScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (EggIncubatorRender) render;
    }

    @Override
    protected void init() {
        super.init();
        gauge("voltage", () -> render.voltage / render.descriptor.nominalVoltage,
            () -> Component.literal(Utils.plotVolt("", render.voltage)), 1.05, 1.2);
    }
}
