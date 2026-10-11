package mods.eln.sixnode.energymeter;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Energy meter (P4b; layout gui/energy_meter.json). As in 1.12, a meter with a password is locked until the right one
 * is tried (checked on the client); unlocked, the password button sets a new one.
 */
public class EnergyMeterScreen extends ElnScreen {
    final EnergyMeterRender render;
    EaField password, energy;
    boolean isLogged;
    final java.util.List<Button> locked = new java.util.ArrayList<>();
    Button eunit, tunit, mode;
    EnergyMeterElement.Mod shownMode;

    public EnergyMeterScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (EnergyMeterRender) render;
    }

    static final String[] ENERGY_UNITS = {"J", "KJ", "MJ", "GJ"}, TIME_UNITS = {"H", "D"};

    boolean hasWheels() {
        return render.descriptor.timeNumberWheel != null && render.descriptor.timeNumberWheel.length != 0;
    }

    @Override
    protected void init() {
        super.init();
        locked.clear();
        shownMode = null;
        isLogged = render.password == null || render.password.isEmpty();
        password = field("password", () -> "", v -> {
        }, tr("Enter password"));
        button("password", () -> isLogged ? tr("Change password") : tr("Try password"), () -> {
            if (isLogged) render.clientSetString(EnergyMeterElement.clientPasswordId, password.getValue());
            else if (password.getValue().equals(render.password)) isLogged = true;
        });
        locked.add(button("state", () -> render.switchState ? tr("is on") : tr("is off"),
            () -> render.clientSend(EnergyMeterElement.clientToggleStateId)));
        mode = button("mode", () -> render.mod == EnergyMeterElement.Mod.ModPrepay ? tr("Prepay Mode") : tr("Counter Mode"),
            () -> render.clientSetString(EnergyMeterElement.clientModId, (render.mod == EnergyMeterElement.Mod.ModPrepay
                ? EnergyMeterElement.Mod.ModCounter : EnergyMeterElement.Mod.ModPrepay).name()));
        locked.add(mode);
        energy = field("energy", () -> "0", v -> {
        }, tr("Enter new energy"), tr("value in kJ"));
        locked.add(button("setenergy", () -> tr("Set energy counter"), () -> {
            float kj = EaField.parse(energy.getValue());
            if (!Float.isNaN(kj)) render.clientSetDouble(EnergyMeterElement.clientEnergyStackId, kj * 1000.0);
        }));
        eunit = button("eunit", () -> render.energyUnit >= 0 && render.energyUnit < 4 ? ENERGY_UNITS[render.energyUnit] : "??",
            () -> render.clientSend(EnergyMeterElement.clientEnergyUnitId), tr("Unit of the energy wheels"));
        tunit = button("tunit", () -> render.timeUnit >= 0 && render.timeUnit < 2 ? TIME_UNITS[render.timeUnit] : "??",
            () -> render.clientSend(EnergyMeterElement.clientTimeUnitId), tr("Unit of the time wheels"));
        locked.add(button("resettime", () -> tr("Reset time counter"), () -> render.clientSend(EnergyMeterElement.clientTimeCounterId)));
        text("energy", () -> tr("Energy counter: %1$J", (long) render.energyStack));
        text("time", () -> tr("Time counter: %1$", Utils.plotTime(render.timerCouter)));
        text("power", () -> Utils.plotPower(tr("Power:"), render.power).trim());
        update();
    }

    @Override
    protected void update() {
        for (Button b : locked) b.active = isLogged;
        eunit.active = tunit.active = isLogged && hasWheels();
        if (render.mod != shownMode) {
            shownMode = render.mod;
            String help = tr("Counts the energy conducted from\n§4red§f to §1blue§f.");
            if (shownMode == EnergyMeterElement.Mod.ModPrepay)
                help += "\n\n" + tr("You can set an initial\namount of available energy.\nWhen the counter arrives at 0\nthe contact will be opened.");
            mode.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(help)));
        }
    }
}
