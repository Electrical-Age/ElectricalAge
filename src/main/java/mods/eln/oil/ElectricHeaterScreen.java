package mods.eln.oil;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Electric heater (layout gui/electric_heater.json): the regulator slot and its set point; without one, full power. */
public class ElectricHeaterScreen extends ElnScreen {
    final ElectricHeaterRender render;
    EaSlider target;

    public ElectricHeaterScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricHeaterRender) render;
    }

    static String celsius(double relative) {
        return Utils.plotValue(relative + PhysicalConstant.Tamb) + "°C";
    }

    @Override
    protected void init() {
        super.init();
        target = slider("target", 0, 780, 10, render.target,
            v -> Component.literal(deviceItem(ElectricHeaterElement.REGULATOR_SLOT).isEmpty()
                ? tr("No regulator: full power  (now %1$, %2$)", celsius(render.temperature), Utils.plotPower("", render.power).trim())
                : tr("Set point %1$  (now %2$, %3$)", celsius(v), celsius(render.temperature), Utils.plotPower("", render.power).trim())),
            v -> render.clientSendFloat(ElectricHeaterElement.SET_TARGET_EVENT, (float) v));
        update();
    }

    @Override
    protected void update() {
        if (render.targetNew) {
            target.setFromDevice(render.target);
            render.targetNew = false;
        }
        target.active = !deviceItem(ElectricHeaterElement.REGULATOR_SLOT).isEmpty();
        target.refreshLabel();
    }
}
