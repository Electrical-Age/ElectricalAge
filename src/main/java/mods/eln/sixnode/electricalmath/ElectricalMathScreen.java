package mods.eln.sixnode.electricalmath;

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

/** Signal processor (P4b, 1.12 ElectricalMathGui): the output formula and whether it is valid / has its redstone. */
public class ElectricalMathScreen extends ElnScreen {
    final ElectricalMathRender render;

    public ElectricalMathScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalMathRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field(8, 8, 204, () -> render.expression, v -> render.clientSetString(ElectricalMathElement.setExpressionId, v),
            tr("Output voltage formula"), tr("Inputs are") + " A B C");
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int red = 0;
        var stack = menu.deviceInventory.getItem(ElectricalMathContainer.restoneSlotId);
        if (!stack.isEmpty()) red = stack.getCount();
        String t;
        int c;
        if (render.expression == null || render.expression.isEmpty()) {
            t = tr("Equation required!");
            c = 0xFF404040;
        } else if (render.equationIsValid) {
            t = tr("%1$ Redstone(s) required", render.redstoneRequired);
            c = red >= render.redstoneRequired ? 0xFF108F00 : 0xFFC02020;
        } else {
            t = tr("Invalid equation!");
            c = 0xFFC02020;
        }
        g.text(font, t, leftPos + 30, topPos + 31, c, false);
    }
}
