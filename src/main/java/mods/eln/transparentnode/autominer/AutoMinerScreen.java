package mods.eln.transparentnode.autominer;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Auto miner (P4b; layout gui/auto_miner.json): silk touch, or the missing-chest warning. */
public class AutoMinerScreen extends ElnScreen {
    final AutoMinerRender render;

    public AutoMinerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (AutoMinerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        silk = button("silk", () -> tr("Silk Touch") + " " + (render.silkTouch ? tr("On") : tr("Off")),
            () -> render.clientSendId(AutoMinerElement.toggleSilkTouch), tr("Halves speed, triples power draw"));
        String[] t = tr("Chest missing on the\nback of the auto miner!").split("\n");
        text("chest1", () -> chestFull() ? t[0] : "", () -> 0xFFC02020);
        text("chest2", () -> chestFull() && t.length > 1 ? t[1] : "", () -> 0xFFC02020);
    }

    net.minecraft.client.gui.components.Button silk;

    boolean chestFull() {
        return render.job == AutoMinerSlowProcess.jobType.chestFull;
    }

    @Override
    protected void update() {
        silk.visible = !chestFull();
    }
}
