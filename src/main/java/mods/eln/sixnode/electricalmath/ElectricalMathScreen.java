package mods.eln.sixnode.electricalmath;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Signal processor (P4b; layout gui/electrical_math.json): the output formula and whether it is valid / has its redstone. */
public class ElectricalMathScreen extends ElnScreen {
    final ElectricalMathRender render;

    public ElectricalMathScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalMathRender) render;
    }

    @Override
    protected void init() {
        super.init();
        field("expression", () -> render.expression, v -> render.clientSetString(ElectricalMathElement.setExpressionId, v),
            tr("Output voltage formula"), tr("Inputs are") + " A B C", tr("Name parts with name = expression; separate them with ;"))
            .setMaxLength(mods.eln.solver.Program.MAX_CHARS);
        text("status", this::status, this::statusColor);
    }

    int chips() {
        var stack = menu.deviceInventory.getItem(ElectricalMathContainer.restoneSlotId);
        return ElectricalMathElement.isChip(stack) ? stack.getCount() : 0;
    }

    String status() {
        if (render.expression == null || render.expression.isEmpty()) return tr("Equation required!");
        if (render.equationIsValid) return tr("%1$ Cheap Chip(s) required", render.chipsRequired);
        return render.error == null || render.error.isEmpty() ? tr("Invalid equation!") : render.error;
    }

    int statusColor() {
        if (render.expression == null || render.expression.isEmpty()) return LABEL;
        if (render.equationIsValid) return chips() >= render.chipsRequired ? 0xFF108F00 : 0xFFC02020;
        return 0xFFC02020;
    }
}
