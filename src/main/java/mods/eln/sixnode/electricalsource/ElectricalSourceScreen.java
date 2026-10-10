package mods.eln.sixnode.electricalsource;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical source (P4b; layout gui/electrical_source.json): the output voltage, typed in. */
public class ElectricalSourceScreen extends ElnScreen {
    final ElectricalSourceRender render;

    public ElectricalSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("now", () -> tr("Output voltage") + "  (" + tr("now %1$", Utils.plotVolt("", render.voltage)) + ")");
        text("unit", () -> "V");
        EaField f = field("voltage", () -> num(render.voltage), v -> {
            float x = EaField.parse(v);
            if (!Float.isNaN(x)) render.clientSetFloat(ElectricalSourceElement.setVoltageId, x);
        }, tr("Output voltage"));
        button("set", () -> tr("Set"), () -> {
            float x = EaField.parse(f.getValue());
            if (!Float.isNaN(x)) render.clientSetFloat(ElectricalSourceElement.setVoltageId, x);
        });
        setInitialFocus(f);
    }
}
