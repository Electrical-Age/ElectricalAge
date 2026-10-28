package mods.eln.anvil;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.HashMap;

import static mods.eln.i18n.I18N.tr;

/**
 * The electrical anvil (layout gui/electrical_anvil.json): tool -> progress <- material, the state (waiting for the
 * line's voltage, welding, paused on a sag), the credit kept per material, the tool's anvil penalty, and its name
 * (Enter engraves it: a small pulse).
 */
public class ElectricalAnvilScreen extends ElnScreen {
    final ElectricalAnvilRender render;

    public ElectricalAnvilScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalAnvilRender) render;
    }

    String toolName() {
        if (render.pendingName != null) return render.pendingName;
        Component c = render.tool.get(DataComponents.CUSTOM_NAME);
        return c == null ? "" : c.getString();
    }

    @Override
    protected void init() {
        super.init();
        progress("work", () -> render.progress);
        text("namel", () -> tr("Name"));
        text("status", () -> ElectricalAnvilElement.statusText(render.status, render.voltage)
                + (render.welding ? " " + mods.eln.misc.Utils.plotPower("", render.powerW) : ""),
            () -> switch (render.status) {
                case ElectricalAnvilElement.SAGGING, ElectricalAnvilElement.CANT, ElectricalAnvilElement.NO_MATERIAL -> 0xA02020;
                case ElectricalAnvilElement.WELDING, ElectricalAnvilElement.ENGRAVING -> 0x206020;
                default -> 0x404040;
            });
        text("credit", () -> {
            var m = new HashMap<String, Double>();
            render.credit.forEach((k, v) -> m.put(k, (double) v));
            return render.credit.isEmpty() ? tr("No material credit") : tr("Credit: %1$", ElectricalAnvilElement.creditText(new java.util.TreeMap<>(m)));
        });
        text("penalty", () -> render.tool.isEmpty() ? tr("No XP needed") : tr("Anvil cost %1$ (lowest %2$), no XP needed",
            String.format("%.0f", render.penalty), String.format("%.0f", render.floor)));
        field("name", this::toolName, render::clientSetName, tr("The tool's name: Enter engraves it (a small pulse)"));
    }
}
