package mods.eln.transparentnode.teleporter;

import mods.eln.client.gui.EaSlider;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/** Experimental transporter (P4b; layout gui/teleporter.json): names, charge power, the energy gauge and Start. */
public class TeleporterScreen extends ElnScreen {
    final TeleporterRender render;
    EaSlider charge;
    Button start;

    public TeleporterScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (TeleporterRender) render;
    }

    @Override
    protected void init() {
        super.init();
        text("namel", () -> tr("Name"));
        text("targetl", () -> tr("Destination"));
        field("name", () -> render.name == null ? "" : render.name, v -> render.clientSendString(TeleporterElement.setNameId, v),
            tr("Transporter name"));
        field("target", () -> render.targetName == null ? "" : render.targetName,
            v -> render.clientSendString(TeleporterElement.setTargetNameId, v), tr("Destination transporter"));
        charge = slider("charge", 2000, 20000, 200, render.chargePower, v -> Component.literal(tr("Power consumption: %1$W", (int) v)),
            v -> render.clientSendFloat(TeleporterElement.setChargePowerId, (float) v));
        render.chargePowerNew = false;
        gauge("energy", () -> render.energyTarget <= 0 ? 0 : render.energyHit / render.energyTarget,
            () -> Component.literal(tr("Required energy: %1$J", (int) render.energyTarget) + "  " + (int) (render.processRatio * 100) + "%"),
            2, 2);
        start = button("start", () -> tr("Start"), () -> render.clientSendId(TeleporterElement.startId));
        update();
    }

    @Override
    protected void update() {
        start.active = render.state == TeleporterElement.StateIdle;
        if (render.chargePowerNew) {
            charge.setFromDevice(render.chargePower);
            render.chargePowerNew = false;
        }
    }
}
