package mods.eln.gridnode.electricalpole;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.node.voxel.CableSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * A utility pole's screen (layout gui/utility_pole.json): its equipment slots (transformer, stasis anchor, the
 * streetlights facing north, east, south and west), to take things out or put them in, and the line, transformer,
 * lights and anchor at a glance.
 */
public class PoleScreen extends ElnScreen {
    final ElectricalPoleRender render;

    public PoleScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalPoleRender) render;
    }

    @Override
    protected void init() {
        super.init();
        ElectricalPoleRender r = render;
        text("gearl", () -> tr("Transformer, anchor"));
        text("lampsl", () -> tr("Lights: N E S W"));
        text("line", () -> tr("Line: %1$ kits, %2$, %3$", CableSpec.voltsLabel(r.lineTier), mods.eln.api.Meters.volts(Math.abs(r.lineU)),
            mods.eln.api.Meters.watts(Math.abs(r.lineP))));
        text("xfmr", () -> r.transformerTier == 0 ? tr("No transformer: the foot's socket is dead")
            : tr("Transformer: line to %1$", CableSpec.voltsLabel(r.transformerTier)));
        text("lights", () -> {
            int n = Integer.bitCount(r.lampMask);
            if (n == 0) return tr("No streetlights");
            return r.lit ? tr("Streetlights: %1$, lit (%2$)", n, mods.eln.api.Meters.watts(n * ElectricalPoleElement.LAMP_WATTS))
                : tr("Streetlights: %1$, off (daylight or no power)", n);
        });
        text("anchor", () -> r.anchor ? tr("Stasis anchor: keeps running in stopped time") : tr("No stasis anchor"));
    }
}
