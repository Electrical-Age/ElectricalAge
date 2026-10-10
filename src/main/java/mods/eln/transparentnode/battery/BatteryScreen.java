package mods.eln.transparentnode.battery;

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

/** Battery (P4b, 1.12 BatteryGuiDraw): life, energy, power and time to full/empty; energy as a gauge. */
public class BatteryScreen extends ElnScreen {
    final BatteryRender render;

    public BatteryScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (BatteryRender) render;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new EaGauge(leftPos + 8, topPos + 58, 116, 11, this::fill,
            () -> Component.literal(Utils.plotPercent("", fill()).trim()), 2, 2));
    }

    double full() {
        return render.descriptor.electricalStdEnergy * render.life;
    }

    double fill() {
        return full() <= 0 ? 0 : render.energy / full();
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        double p = render.power, miss = full() - render.energy;
        String s1, s2 = "";
        if (Math.abs(p) < 5) s1 = tr("No charge");
        else if (p > 0) {
            s1 = tr("Discharge");
            s2 = Utils.plotTime("", render.energy / p);
        } else if (miss > 0) {
            s1 = tr("Charge");
            s2 = Utils.plotTime("", -miss / p);
        } else s1 = tr("Charged");
        int x2 = 64, y = 16;
        if (render.descriptor.lifeEnable) {
            label(g, 8, y, tr("Life:"));
            label(g, x2, y, Utils.plotPercent("", render.life));
            y += 9;
        }
        label(g, 8, y, tr("Energy:"));
        label(g, x2, y, Utils.plotValue(render.energy, "J/") + Utils.plotValue(full(), "J"));
        label(g, 8, y + 9, p >= 0 ? tr("Power out:") : tr("Power in:"));
        label(g, x2, y + 9, Utils.plotValue(Math.abs(p), "W/") + Utils.plotValue(render.descriptor.electricalStdP, "W"));
        label(g, 8, y + 18, s1);
        label(g, x2, y + 18, s2);
    }
}
