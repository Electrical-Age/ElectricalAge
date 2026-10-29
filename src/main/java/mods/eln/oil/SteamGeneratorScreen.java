package mods.eln.oil;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The 50V/200V steam turbine's screen (Baughn 2026-10-26; layout gui/steam_turbine.json): internal or external control
 * (a signal on its left flank), a regulator slot, and either the throttle (no regulator) or the target inlet pressure
 * (with one).
 */
public class SteamGeneratorScreen extends ElnScreen {
    final SteamValveControl.Render render;
    EaSlider throttle, target;

    public SteamGeneratorScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        // (the 50V/200V turbines' render or the shaft Steam Turbine's: both hold their controls' client side)
        this.render = ((SteamValveControl.Rendered) render).controls();
    }

    @Override
    protected void init() {
        super.init();
        button("external", () -> render.controlExternal ? tr("External control (signal, left side)") : tr("Internal control"),
            render::clientToggleControl);
        throttle = slider("throttle", 0, 1, 0.01, Math.max(0, render.throttle),
            v -> Component.literal(tr("Throttle: %1$%", (int) Math.round(v * 100))),
            v -> render.clientSetThrottle((float) v));
        target = slider("target", SteamValveControl.TARGET_MIN_BAR, render.targetMaxBar, 0.5, Math.max(1, render.targetBar),
            v -> Component.literal(tr("Hold the inlet at: %1$ bar", String.format("%.1f", v)) + "  (" + tr("now %1$ bar", String.format("%.1f", render.inletBar)) + ")"),
            v -> render.clientSetTarget((float) v));
        text("valve", () -> tr("Steam valve may open: %1$%", (int) Math.round(render.limit * 100)));
        render.throttleNew = render.targetNew = false;
        update();
    }

    @Override
    protected void update() {
        boolean regulator = !deviceItem(SteamValveControl.REGULATOR_SLOT).isEmpty();
        throttle.active = !regulator && !render.controlExternal;
        target.active = regulator && !render.controlExternal;
        if (render.throttleNew) {
            throttle.setFromDevice(render.throttle);
            render.throttleNew = false;
        }
        if (render.targetNew) {
            target.setFromDevice(render.targetBar);
            render.targetNew = false;
        }
        throttle.refreshLabel();
        target.refreshLabel();
    }
}
