package mods.eln.transparentnode.battery;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Battery (P4b; layout gui/battery.json): life, energy, power and time to full/empty; energy as a gauge. */
public class BatteryScreen extends ElnScreen {
    final BatteryRender render;

    public BatteryScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (BatteryRender) render;
    }

    @Override
    protected void init() {
        super.init();
        gauge("energy", this::fill, () -> Component.literal(Utils.plotPercent("", fill()).trim()), 2, 2);
        text("l1", () -> render.descriptor.lifeEnable ? tr("Life:") + " " + Utils.plotPercent("", render.life) : "");
        text("l2", () -> tr("Energy:") + " " + Utils.plotValue(render.energy, "J/") + Utils.plotValue(full(), "J"));
        text("l3", () -> (render.power >= 0 ? tr("Power out:") : tr("Power in:")) + " "
            + Utils.plotValue(Math.abs(render.power), "W/") + Utils.plotValue(render.descriptor.electricalStdP, "W"));
        text("l4", this::state);
    }

    double full() {
        return render.descriptor.electricalStdEnergy * render.life;
    }

    double fill() {
        return full() <= 0 ? 0 : render.energy / full();
    }

    String state() {
        double p = render.power, miss = full() - render.energy;
        if (Math.abs(p) < 5) return tr("No charge");
        if (p > 0) return tr("Discharge") + " " + Utils.plotTime("", render.energy / p);
        if (miss > 0) return tr("Charge") + " " + Utils.plotTime("", -miss / p);
        return tr("Charged");
    }
}
