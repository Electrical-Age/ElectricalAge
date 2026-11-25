package mods.eln.sixnode.mppt;

import mods.eln.api.Meters;
import mods.eln.client.gui.EaField;
import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** The MPPT charge controller's screen: what comes in and goes out, its state, and the battery's charge voltage. */
public class MpptScreen extends ElnScreen {
    final MpptRender render;
    EaSlider charge;

    public MpptScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (MpptRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("panels", () -> tr("Panels: %1$ at %2$, %3$", Meters.watts(render.uIn * render.iIn),
            Meters.volts(render.uIn), Meters.amps(render.iIn)));
        text("battery", () -> tr("Battery: %1$ at %2$, %3$", Meters.watts(render.uOut * render.iOut),
            Meters.volts(render.uOut), Meters.amps(render.iOut)));
        text("state", () -> tr("State: %1$", MpptElement.stateText(render.state)));
        charge = slider("charge", MpptElement.MIN_CHARGE_VOLTS, render.descriptor.maxInputVolts, 0.1, render.chargeVolts,
            v -> Component.literal(tr("Charge voltage: %1$", Meters.volts(v))),
            v -> render.clientSetFloat(MpptElement.setChargeVoltsId, (float) v));
        // typed, for an exact charge voltage (AdventurAgent: the slider moves ~5 V a pixel on the 700 V one)
        field("chargeField", () -> num(render.chargeVolts), s -> {
            float f = EaField.parse(s);
            if (!Float.isNaN(f)) render.clientSetFloat(MpptElement.setChargeVoltsId, f);
        }, tr("Charge voltage, volts"));
        text("amps", () -> tr("Charge current: up to %1$", Meters.amps(render.chargeAmps)));
        field("ampsField", () -> num(render.chargeAmps), s -> {
            float f = EaField.parse(s);
            if (!Float.isNaN(f)) render.clientSetFloat(MpptElement.setChargeAmpsId, f);
        }, tr("The most charging current, amps (a small battery takes little)"));
    }

    @Override
    protected void update() {
        if (render.chargeVoltsNew) {
            charge.setFromDevice(render.chargeVolts);
            render.chargeVoltsNew = false;
        }
    }
}
