package mods.eln.sixnode.electricalgatesource;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Signal source / gate (P4b, 1.12 ElectricalGateSourceGui): output level 0-100 %. */
public class ElectricalGateSourceScreen extends ElnScreen {
    final ElectricalGateSourceRender render;
    EaSlider level;

    public ElectricalGateSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, 176, 48);
        this.render = (ElectricalGateSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        // 1.12: a track bar 0..50 in steps of 0.5, shown as % (x2), sent as is
        level = addRenderableWidget(new EaSlider(leftPos + 8, topPos + 20, 160, 0, 50, 0.5, render.voltageSyncValue,
            v -> Component.literal(tr("Output: %1$%", (int) Math.round(v * 2))),
            v -> render.clientSetFloat(ElectricalGateSourceElement.setVoltagerId, (float) v)));
    }

    @Override
    protected void update() {
        if (render.voltageSyncNew) {
            level.setFromDevice(render.voltageSyncValue);
            render.voltageSyncNew = false;
        }
    }
}
