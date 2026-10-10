package mods.eln.transparentnode.autominer;

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

/** Auto miner (P4b, 1.12 AutoMinerGuiDraw): silk touch, or the missing-chest warning. */
public class AutoMinerScreen extends ElnScreen {
    final AutoMinerRender render;

    public AutoMinerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (AutoMinerRender) render;
    }

    net.minecraft.client.gui.components.Button silk;

    @Override
    protected void init() {
        super.init();
        silk = button(7, 6, 122, () -> tr("Silk Touch") + " " + (render.silkTouch ? tr("On") : tr("Off")),
            () -> render.clientSendId(AutoMinerElement.toggleSilkTouch), tr("Halves speed, triples power draw"));
    }

    @Override
    protected void update() {
        silk.visible = render.job != AutoMinerSlowProcess.jobType.chestFull;
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (render.job == AutoMinerSlowProcess.jobType.chestFull) {
            String[] t = tr("Chest missing on the\nback of the auto miner!").split("\n");
            for (int i = 0; i < t.length; i++) g.text(font, t[i], leftPos + 8, topPos + 7 + 9 * i, 0xFFC02020, false);
        }
    }
}
