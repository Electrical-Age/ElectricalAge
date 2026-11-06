package mods.eln.sixnode.electricalgatesource;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Signal trimmer / gate source (P4b; layout gui/gate_source.json): output level 0-100 %. */
public class ElectricalGateSourceScreen extends ElnScreen {
    final ElectricalGateSourceRender render;

    public ElectricalGateSourceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalGateSourceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        // 1.12: a track bar over the signal range in 100 steps, shown as %, sent in volts
        level = slider("level", 0, mods.eln.Eln.SVU, mods.eln.Eln.SVU / 100, render.voltageSyncValue,
            v -> Component.literal(tr("Output: %1$%", (int) Math.round(v / mods.eln.Eln.SVU * 100))),
            v -> render.clientSetFloat(ElectricalGateSourceElement.setVoltagerId, (float) v));
    }

    EaSlider level;

    @Override
    protected void update() {
        if (render.voltageSyncNew) {
            level.setFromDevice(render.voltageSyncValue);
            render.voltageSyncNew = false;
        }
    }
}
