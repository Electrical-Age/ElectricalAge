package mods.eln.transparentnode.solarpanel;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Rotating solar panel (P4b; layout gui/solar_panel.json): the panel angle (locked by a tracker) and the sun. */
public class SolarPanelScreen extends ElnScreen {
    final SolarPanelRender render;

    public SolarPanelScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (SolarPanelRender) render;
    }

    @Override
    protected void init() {
        super.init();
        angle = slider("angle", render.descriptor.alphaMin, render.descriptor.alphaMax,
            (render.descriptor.alphaMax - render.descriptor.alphaMin) / 180,
            render.pannelAlphaSyncValue < -1000 ? render.descriptor.alphaMin : render.pannelAlphaSyncValue,
            v -> Component.literal(tr("Panel angle: %1$°", deg(v))), v -> render.clientSetPannelAlpha((float) v));
        text("sun", () -> {
            int sun = deg(SolarPannelSlowProcess.getSolarAlpha(render.tileEntity.getLevel()));
            return Math.abs(sun) > 90 ? tr("It is night") : tr("Sun angle: %1$°", sun);
        });
        text("tracker", () -> render.hasTracker ? tr("The tracker steers the panel") : "");
    }

    EaSlider angle;

    static int deg(double rad) {
        return (int) (180 / Math.PI * rad) - 90;
    }

    @Override
    protected void update() {
        if (render.pannelAlphaSyncNew) {
            angle.setFromDevice(render.pannelAlphaSyncValue);
            render.pannelAlphaSyncNew = false;
        }
        angle.active = !render.hasTracker;
    }
}
