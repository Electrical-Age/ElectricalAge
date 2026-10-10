package mods.eln.transparentnode.electricalmachine;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electrical machines: macerator, compressor, plate machine, magnetizer (P4b; layout gui/electrical_machine.json). */
public class ElectricalMachineScreen extends ElnScreen {
    final ElectricalMachineRender render;

    public ElectricalMachineScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalMachineRender) render;
    }

    @Override
    protected void init() {
        super.init();
        progress("process", () -> render.processState);
        var g = gauge("supply", () -> render.UFactor, () -> Component.literal(Utils.plotVolt("", render.UFactor * render.descriptor.nominalU)
            + " / " + Utils.plotVolt("", render.descriptor.nominalU) + "  " + Utils.plotPower("", render.powerFactor * render.descriptor.nominalP)), 1.05, 1.2);
    }
}
