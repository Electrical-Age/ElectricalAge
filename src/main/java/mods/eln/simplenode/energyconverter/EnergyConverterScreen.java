package mods.eln.simplenode.energyconverter;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** EA -> Forge Energy converter (P4b; layout gui/energy_converter.json): the input power limit. */
public class EnergyConverterScreen extends ElnScreen {
    final EnergyConverterElnToOtherEntity render;
    EaSlider limit;

    public EnergyConverterScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (EnergyConverterElnToOtherEntity) render;
    }

    @Override
    protected void init() {
        super.init();
        limit = slider("limit", 0, 1, 0.01, render.inPowerFactor,
            v -> Component.literal(tr("Input power is limited to %1$W", (int) (v * render.inPowerMax))),
            v -> render.clientSendFloat(EnergyConverterElnToOtherNode.setInPowerFactor, (float) v));
        render.hasChanges = false;
    }

    @Override
    protected void update() {
        if (render.hasChanges) {
            limit.setFromDevice(render.inPowerFactor);
            render.hasChanges = false;
        }
        limit.refreshLabel();
    }
}
