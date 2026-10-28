package mods.eln.client;

import mods.eln.client.gui.ElnScreens;
import mods.eln.misc.Utils;

import static mods.eln.i18n.I18N.tr;

/** Descriptor class -> screen (P4b). Devices without one open the generic slot screen. */
final class ClientScreens {
    private ClientScreens() {
    }

    static void registerAll() {
        // screen layouts (assets/eln/gui/<name>.json)
        layout("transparentnode.electricalfurnace.ElectricalFurnaceDescriptor", "electrical_furnace");
        layout("transparentnode.smelter.SmelterDescriptor", "smelter");
        layout("oil.SteamOvenDescriptor", "steam_oven");
        layout("anvil.ElectricalAnvilDescriptor", "electrical_anvil");
        layout("sixnode.electricalsource.ElectricalSourceDescriptor", "electrical_source");
        layout("sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor", "wireless_tx");
        layout("sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor", "wireless_source");
        layout("sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor", "wireless_rx");
        layout("sixnode.electricalalarm.ElectricalAlarmDescriptor", "electrical_alarm");
        layout("sixnode.electricalrelay.ElectricalRelayDescriptor", "electrical_relay");
        layout("sixnode.electricalgatesource.ElectricalGateSourceDescriptor", "gate_source");
        layout("sixnode.electricaltimeout.ElectricalTimeoutDescriptor", "electrical_timeout");
        layout("sixnode.electricalmath.ElectricalMathDescriptor", "electrical_math");
        layout("sixnode.electricalbreaker.ElectricalBreakerDescriptor", "electrical_breaker");
        layout("sixnode.batterycharger.BatteryChargerDescriptor", "battery_charger");
        layout("sixnode.powersocket.PowerSocketDescriptor", "power_socket");
        layout("transparentnode.battery.BatteryDescriptor", "battery");
        layout("transparentnode.transformer.TransformerDescriptor", "transformer");
        layout("transparentnode.autominer.AutoMinerDescriptor", "auto_miner");
        layout("transparentnode.solarpanel.SolarPanelDescriptor", "solar_panel");
        layout("transparentnode.turret.TurretDescriptor", "turret");
        layout("transparentnode.eggincubator.EggIncubatorDescriptor", "egg_incubator");
        layout("transparentnode.electricalmachine.ElectricalMachineDescriptor", "electrical_machine");
        layout("sixnode.resistor.ResistorDescriptor", "resistor");
        layout("transparentnode.LargeRheostatDescriptor", "resistor");
        layout("sixnode.powercapacitorsix.PowerCapacitorSixDescriptor", "power_capacitor");
        layout("transparentnode.powercapacitor.PowerCapacitorDescriptor", "power_capacitor");
        layout("sixnode.powerinductorsix.PowerInductorSixDescriptor", "power_inductor");
        layout("transparentnode.powerinductor.PowerInductorDescriptor", "power_inductor");
        layout("sixnode.electricalwatch.ElectricalWatchDescriptor", "electrical_watch");
        layout("sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor", "entity_sensor");
        layout("sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor", "fire_detector");
        layout("sixnode.lampsupply.LampSupplyDescriptor", "lamp_supply");
        layout("sixnode.energymeter.EnergyMeterDescriptor", "energy_meter");
        layout("sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor", "data_logger");
        layout("transparentnode.teleporter.TeleporterDescriptor", "teleporter");
        layout("sixnode.tutorialsign.TutorialSignDescriptor", "tutorial_sign");
        layout("transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor", "energy_converter");
        layout("sixnode.hub.HubDescriptor", "hub");
        layout("transparentnode.heatfurnace.HeatFurnaceDescriptor", "heat_furnace");
        layout("oil.SteamGeneratorDescriptor", "steam_turbine");
        layout("transparentnode.FuelHeatFurnaceDescriptor", "fuel_heat_furnace");
        layout("oil.ElectricHeaterDescriptor", "electric_heater");
        mods.eln.client.gui.ElnLayouts.register(mods.eln.sixnode.lampsocket.LampSocketDescriptor.class,
            d -> mods.eln.sixnode.lampsocket.LampSocketScreen.fixedLayout(d) ? "lamp_socket_fixed" : "lamp_socket");
        mods.eln.client.gui.ElnLayouts.register(mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor.class,
            d -> ((mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor) d).voltageOnly ? "voltage_probe" : "electrical_sensor");
        mods.eln.client.gui.ElnLayouts.register(mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor.class,
            d -> ((mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor) d).temperatureOnly ? "temperature_probe" : "thermal_sensor");
        ElnScreens.register(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor.class,
            mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceScreen::new);
        ElnScreens.register(mods.eln.transparentnode.smelter.SmelterDescriptor.class, mods.eln.transparentnode.smelter.SmelterScreen::new);
        ElnScreens.register(mods.eln.oil.SteamOvenDescriptor.class, mods.eln.oil.SteamOvenScreen::new);
        ElnScreens.register(mods.eln.anvil.ElectricalAnvilDescriptor.class, mods.eln.anvil.ElectricalAnvilScreen::new);
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
        // S1d
        reg(mods.eln.sixnode.lampsocket.LampSocketDescriptor.class, mods.eln.sixnode.lampsocket.LampSocketScreen::new);
        reg(mods.eln.sixnode.lampsupply.LampSupplyDescriptor.class, mods.eln.sixnode.lampsupply.LampSupplyScreen::new);
        reg(mods.eln.sixnode.hub.HubDescriptor.class, mods.eln.sixnode.hub.HubScreen::new);
        reg(mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor.class, mods.eln.sixnode.electricalsensor.ElectricalSensorScreen::new);
        reg(mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor.class, mods.eln.sixnode.thermalsensor.ThermalSensorScreen::new);
        reg(mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor.class, mods.eln.transparentnode.heatfurnace.HeatFurnaceScreen::new);
        reg(mods.eln.oil.SteamGeneratorDescriptor.class, mods.eln.oil.SteamGeneratorScreen::new);
        reg(mods.eln.transparentnode.FuelHeatFurnaceDescriptor.class, mods.eln.transparentnode.FuelHeatFurnaceScreen::new);
        reg(mods.eln.oil.ElectricHeaterDescriptor.class, mods.eln.oil.ElectricHeaterScreen::new);
        // S1e
        reg(mods.eln.sixnode.energymeter.EnergyMeterDescriptor.class, mods.eln.sixnode.energymeter.EnergyMeterScreen::new);
        reg(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor.class,
            mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerScreen::new);
        reg(mods.eln.transparentnode.teleporter.TeleporterDescriptor.class, mods.eln.transparentnode.teleporter.TeleporterScreen::new);
        reg(mods.eln.sixnode.tutorialsign.TutorialSignDescriptor.class, mods.eln.sixnode.tutorialsign.TutorialSignScreen::new);
        reg(mods.eln.transparentnode.energyconverter.EnergyConverterElnToOtherDescriptor.class,
            mods.eln.transparentnode.energyconverter.EnergyConverterScreen::new);
        // S1f: the Kotlin devices (chips share a descriptor class: by element class)
        mods.eln.client.gui.ElnLayouts.register(mods.eln.sixnode.AnalogChipDescriptor.class, d -> {
            Class<?> e = ((mods.eln.node.six.SixNodeDescriptor) d).ElementClass;
            if (e == mods.eln.sixnode.PIDRegulatorElement.class) return "pid_regulator";
            if (e == mods.eln.sixnode.AmplifierElement.class) return "amplifier";
            if (e == mods.eln.sixnode.SummingUnitElement.class) return "summing_unit";
            if (e == mods.eln.sixnode.FilterElement.class) return "filter";
            return null;
        });
        layout("sixnode.logicgate.LogicGateDescriptor", "pal");
        layout("sixnode.EmergencyLampDescriptor", "emergency_lamp");
        layout("mechanical.TachometerDescriptor", "tachometer");
        reg(mods.eln.sixnode.AnalogChipDescriptor.class, (m, i, t, r) -> {
            if (r instanceof mods.eln.sixnode.PIDRegulatorRender) return new mods.eln.client.gui.PIDRegulatorScreen(m, i, t, r);
            if (r instanceof mods.eln.sixnode.AmplifierRender) return new mods.eln.client.gui.AmplifierScreen(m, i, t, r);
            if (r instanceof mods.eln.sixnode.SummingUnitRender) return new mods.eln.client.gui.SummingUnitScreen(m, i, t, r);
            if (r instanceof mods.eln.sixnode.FilterRender) return new mods.eln.client.gui.FilterScreen(m, i, t, r);
            return new mods.eln.client.gui.ElnScreen(m, i, t);
        });
        reg(mods.eln.sixnode.logicgate.LogicGateDescriptor.class, (m, i, t, r) -> r instanceof mods.eln.sixnode.logicgate.PalRender
            ? new mods.eln.client.gui.PalScreen(m, i, t, r) : new mods.eln.client.gui.ElnScreen(m, i, t));
        reg(mods.eln.sixnode.EmergencyLampDescriptor.class, mods.eln.client.gui.EmergencyLampScreen::new);
        reg(mods.eln.mechanical.TachometerDescriptor.class, mods.eln.client.gui.TachometerScreen::new);
        // value lines from the slots (1.12 drew them over the panel)
        info(mods.eln.sixnode.resistor.ResistorDescriptor.class, (d, inv) -> java.util.List.of(
            tr("Resistance: %1$\u2126", Utils.plotValue(((mods.eln.sixnode.resistor.ResistorDescriptor) d).getRsValue(inv)))));
        info(mods.eln.transparentnode.LargeRheostatDescriptor.class, (d, inv) -> java.util.List.of(
            tr("Nom. Resistance: %1$", Utils.plotValue(((mods.eln.transparentnode.LargeRheostatDescriptor) d).getRsValue(inv), "Ohm"))));
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

    static void layout(String descriptorClass, String name) {
        try {
            mods.eln.client.gui.ElnLayouts.register(Class.forName("mods.eln." + descriptorClass), name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    static void reg(Class<?> descriptor, ElnScreens.Factory f) {
        ElnScreens.register(descriptor, f);
    }
}
