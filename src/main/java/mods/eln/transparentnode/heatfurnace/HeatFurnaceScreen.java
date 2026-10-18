package mods.eln.transparentnode.heatfurnace;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Stone heat furnace (P4b; layout gui/heat_furnace.json): control mode, fuel intake, and either the control gauge (no
 * regulator) or the temperature set point (with one).
 */
public class HeatFurnaceScreen extends ElnScreen {
    final HeatFurnaceRender render;
    EaSlider gain, heat;
    net.minecraft.client.gui.components.Button fuel;

    public HeatFurnaceScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (HeatFurnaceRender) render;
    }

    @Override
    protected void init() {
        super.init();
        button("external", () -> render.controleExternal ? tr("External control") : tr("Internal control"), render::clientToogleControl);
        fuel = button("fuel", () -> render.takeFuel ? tr("Take fuel") : tr("Decline fuel"), render::clientToogleTakeFuel);
        gain = slider("gain", 0.1, 1.0, 0.01, render.gainSyncValue,
            v -> Component.literal(tr("Control gauge at %1$%", (int) Math.round(v * 100)) + "  (" + tr("Power: %1$W", render.power) + ")"),
            v -> render.clientSetGain((float) v));
        text("fire", () -> !render.takeFuel && render.fire <= 0 ? tr("Fire: out (fuel declined: press \"Decline fuel\" to take fuel)")
            : fireLine(render.fire, render.gainSyncValue, render.lighting, mods.eln.sim.Fire.COAL));
        heat = slider("heat", 0, 980, 10, render.temperatureTargetSyncValue,
            v -> Component.literal(tr("Set point: %1$°C", (int) v) + "  (" + tr("Actual: %1$°C", Utils.plotValue(render.temperature)) + ")"),
            v -> render.clientSetTemperatureTarget((float) v));
        render.gainSyncNew = render.temperatureTargetSyncNew = false;
        update();
    }

    /** Baughn: the burner UI shows how the true power setting lags the player's. */
    public static String fireLine(double fire, double setting, boolean lighting, mods.eln.sim.Fire.Kind kind) {
        if (lighting) return tr("Fire: lighting...");
        if (fire <= 0) return tr("Fire: out");
        String trend = Math.abs(fire - setting) < 0.01 ? "" : fire < setting ? "  " + tr("rising") : "  " + tr("falling");
        return tr("Fire: burning at %1$%", (int) Math.round(fire * 100)) + trend;
    }

    @Override
    protected void update() {
        boolean regulator = !deviceItem(HeatFurnaceContainer.regulatorId).isEmpty();
        fuel.active = !render.controleExternal;
        gain.active = !regulator && !render.controleExternal;
        heat.active = regulator && !render.controleExternal;
        if (render.gainSyncNew) {
            gain.setFromDevice(render.gainSyncValue);
            render.gainSyncNew = false;
        }
        if (render.temperatureTargetSyncNew) {
            heat.setFromDevice(render.temperatureTargetSyncValue);
            render.temperatureTargetSyncNew = false;
        }
        gain.refreshLabel();
        heat.refreshLabel();
    }
}
