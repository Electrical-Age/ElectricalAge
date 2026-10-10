package mods.eln.transparentnode.transformer;

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

/** DC-DC converter / transformer (P4b, 1.12 TransformerGuiDraw): windings, core, casing; network isolation. */
public class TransformerScreen extends ElnScreen {
    final TransformerRender render;

    public TransformerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TransformerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button(52, 64, 72, () -> render.isIsolator ? tr("Isolated") : tr("Not isolated"),
            () -> render.clientSendId(TransformerElement.toogleIsIsolator),
            tr("Isolated: the networks on both sides are simulated apart. Faster for circuits that switch very often, at the cost of a little capacitance."));
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // slots: primary (58,30), core (79,30), secondary (100,30), casing (130,74)
        g.text(font, tr("Primary"), leftPos + 66 - font.width(tr("Primary")), topPos + 50, LABEL, false);
        g.text(font, tr("Secondary"), leftPos + 100, topPos + 50, LABEL, false);
        label(g, 130 - 4 - font.width(tr("Casing")), 79, tr("Casing"));
    }
}
