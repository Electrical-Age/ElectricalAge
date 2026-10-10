package mods.eln.client;

import mods.eln.client.gui.ElnScreens;
import mods.eln.misc.Utils;

import static mods.eln.i18n.I18N.tr;

/** Descriptor class -> screen (P4b). Devices without one open the generic slot screen. */
final class ClientScreens {
    private ClientScreens() {
    }

    static void registerAll() {
        ElnScreens.register(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor.class,
            mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceScreen::new);
        ElnScreens.register(mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor.class,
            mods.eln.sixnode.electricalsource.ElectricalSourceScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor.class, mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor.class, mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceScreen::new);
        reg(mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor.class, mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxScreen::new);
        reg(mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor.class, mods.eln.sixnode.electricalalarm.ElectricalAlarmScreen::new);
        reg(mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor.class, mods.eln.sixnode.electricalrelay.ElectricalRelayScreen::new);
        reg(mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor.class, mods.eln.sixnode.electricalgatesource.ElectricalGateSourceScreen::new);
        reg(mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor.class, mods.eln.sixnode.electricaltimeout.ElectricalTimeoutScreen::new);
        // S1c
        reg(mods.eln.sixnode.electricalmath.ElectricalMathDescriptor.class, mods.eln.sixnode.electricalmath.ElectricalMathScreen::new);
        reg(mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor.class, mods.eln.sixnode.electricalbreaker.ElectricalBreakerScreen::new);
        reg(mods.eln.sixnode.batterycharger.BatteryChargerDescriptor.class, mods.eln.sixnode.batterycharger.BatteryChargerScreen::new);
        reg(mods.eln.sixnode.powersocket.PowerSocketDescriptor.class, mods.eln.sixnode.powersocket.PowerSocketScreen::new);
        reg(mods.eln.transparentnode.battery.BatteryDescriptor.class, mods.eln.transparentnode.battery.BatteryScreen::new);
        reg(mods.eln.transparentnode.transformer.TransformerDescriptor.class, mods.eln.transparentnode.transformer.TransformerScreen::new);
        reg(mods.eln.transparentnode.autominer.AutoMinerDescriptor.class, mods.eln.transparentnode.autominer.AutoMinerScreen::new);
        reg(mods.eln.transparentnode.solarpanel.SolarPanelDescriptor.class, mods.eln.transparentnode.solarpanel.SolarPanelScreen::new);
        reg(mods.eln.transparentnode.turret.TurretDescriptor.class, mods.eln.transparentnode.turret.TurretScreen::new);
        reg(mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor.class, mods.eln.transparentnode.eggincubator.EggIncubatorScreen::new);
        reg(mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor.class, mods.eln.transparentnode.electricalmachine.ElectricalMachineScreen::new);
        // value lines from the slots (1.12 drew them over the panel)
        info(mods.eln.sixnode.resistor.ResistorDescriptor.class, (d, inv) -> java.util.List.of(
            tr("Resistance: %1$\u2126", Utils.plotValue(((mods.eln.sixnode.resistor.ResistorDescriptor) d).getRsValue(inv)))));
        info(mods.eln.transparentnode.LargeRheostatDescriptor.class, (d, inv) -> java.util.List.of());
        info(mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor.class, (d, inv) -> {
            var c = (mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor) d;
            return java.util.List.of(tr("Capacity: %1$F", Utils.plotValue(c.getCValue(inv))),
                tr("Nominal voltage: %1$V", Utils.plotValue(c.getUNominalValue(inv))));
        });
        info(mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor.class, (d, inv) -> {
            var c = (mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor) d;
            return java.util.List.of(tr("Capacity: %1$F", Utils.plotValue(c.getCValue(inv))),
                tr("Nominal voltage: %1$V", Utils.plotValue(c.getUNominalValue(inv))));
        });
        info(mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor.class, (d, inv) -> java.util.List.of(
            tr("Inductance: %1$H", Utils.plotValue(((mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor) d).getlValue(inv)))));
        info(mods.eln.transparentnode.powerinductor.PowerInductorDescriptor.class, (d, inv) -> java.util.List.of(
            tr("Inductance: %1$H", Utils.plotValue(((mods.eln.transparentnode.powerinductor.PowerInductorDescriptor) d).getlValue(inv)))));
    }

    static void info(Class<?> descriptor, java.util.function.BiFunction<Object, net.minecraft.world.Container, java.util.List<String>> lines) {
        ElnScreens.register(descriptor, (m, i, t, r) -> new mods.eln.client.gui.InfoScreen(m, i, t,
            inv -> lines.apply(ElnScreens.descriptor(m), inv)));
    }

    static void reg(Class<?> descriptor, ElnScreens.Factory f) {
        ElnScreens.register(descriptor, f);
    }
}
