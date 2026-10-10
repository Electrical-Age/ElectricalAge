package mods.eln.client;

import mods.eln.node.ElnRenders;

/**
 * Descriptor class -> render object (1.12: the RenderClass each descriptor passed to its super constructor). Filled
 * as the renders are ported (P3 R2/R3); unregistered devices draw nothing yet.
 */
public final class ClientRenders {
    private ClientRenders() {
    }

    public static void registerAll() {
        // generated from the 1.12 descriptors' RenderClass (eln/port: super(name, XElement.class, XRender.class))
        ElnRenders.six(mods.eln.signalinductor.SignalInductorDescriptor.class, mods.eln.signalinductor.SignalInductorRender::new);
        ElnRenders.six(mods.eln.sixnode.TreeResinCollector.TreeResinCollectorDescriptor.class, mods.eln.sixnode.TreeResinCollector.TreeResinCollectorRender::new);
        ElnRenders.six(mods.eln.sixnode.batterycharger.BatteryChargerDescriptor.class, mods.eln.sixnode.batterycharger.BatteryChargerRender::new);
        ElnRenders.six(mods.eln.sixnode.diode.DiodeDescriptor.class, mods.eln.sixnode.diode.DiodeRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor.class, mods.eln.sixnode.electricalalarm.ElectricalAlarmRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor.class, mods.eln.sixnode.electricalbreaker.ElectricalBreakerRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalcable.ElectricalCableDescriptor.class, mods.eln.sixnode.electricalcable.ElectricalCableRender::new);
        ElnRenders.six(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor.class, mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor.class, mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor.class, mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRender::new);
        ElnRenders.six(mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor.class, mods.eln.sixnode.electricallightsensor.ElectricalLightSensorRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalmath.ElectricalMathDescriptor.class, mods.eln.sixnode.electricalmath.ElectricalMathRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor.class, mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor.class, mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalrelay.ElectricalRelayDescriptor.class, mods.eln.sixnode.electricalrelay.ElectricalRelayRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor.class, mods.eln.sixnode.electricalsensor.ElectricalSensorRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalsource.ElectricalSourceDescriptor.class, mods.eln.sixnode.electricalsource.ElectricalSourceRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalswitch.ElectricalSwitchDescriptor.class, mods.eln.sixnode.electricalswitch.ElectricalSwitchRender::new);
        ElnRenders.six(mods.eln.sixnode.electricaltimeout.ElectricalTimeoutDescriptor.class, mods.eln.sixnode.electricaltimeout.ElectricalTimeoutRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor.class, mods.eln.sixnode.electricalvumeter.ElectricalVuMeterRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor.class, mods.eln.sixnode.electricalwatch.ElectricalWatchRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor.class, mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorRender::new);
        ElnRenders.six(mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor.class, mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorRender::new);
        ElnRenders.six(mods.eln.sixnode.energymeter.EnergyMeterDescriptor.class, mods.eln.sixnode.energymeter.EnergyMeterRender::new);
        ElnRenders.six(mods.eln.sixnode.groundcable.GroundCableDescriptor.class, mods.eln.sixnode.groundcable.GroundCableRender::new);
        ElnRenders.six(mods.eln.sixnode.hub.HubDescriptor.class, mods.eln.sixnode.hub.HubRender::new);
        ElnRenders.six(mods.eln.sixnode.lampsocket.LampSocketDescriptor.class, mods.eln.sixnode.lampsocket.LampSocketRender::new);
        ElnRenders.six(mods.eln.sixnode.lampsupply.LampSupplyDescriptor.class, mods.eln.sixnode.lampsupply.LampSupplyRender::new);
        ElnRenders.six(mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor.class, mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixRender::new);
        ElnRenders.six(mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor.class, mods.eln.sixnode.powerinductorsix.PowerInductorSixRender::new);
        ElnRenders.six(mods.eln.sixnode.powersocket.PowerSocketDescriptor.class, mods.eln.sixnode.powersocket.PowerSocketRender::new);
        ElnRenders.six(mods.eln.sixnode.resistor.ResistorDescriptor.class, mods.eln.sixnode.resistor.ResistorRender::new);
        ElnRenders.six(mods.eln.sixnode.thermalcable.ThermalCableDescriptor.class, mods.eln.sixnode.thermalcable.ThermalCableRender::new);
        ElnRenders.six(mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor.class, mods.eln.sixnode.thermalsensor.ThermalSensorRender::new);
        ElnRenders.six(mods.eln.sixnode.tutorialsign.TutorialSignDescriptor.class, mods.eln.sixnode.tutorialsign.TutorialSignRender::new);
        ElnRenders.six(mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterDescriptor.class, mods.eln.sixnode.wirelesssignal.repeater.WirelessSignalRepeaterRender::new);
        ElnRenders.six(mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxDescriptor.class, mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxRender::new);
        ElnRenders.six(mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceDescriptor.class, mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceRender::new);
        ElnRenders.six(mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxDescriptor.class, mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxRender::new);
        ElnRenders.transparent(mods.eln.gridnode.downlink.DownlinkDescriptor.class, mods.eln.gridnode.downlink.DownlinkRender::new);
        ElnRenders.transparent(mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor.class, mods.eln.gridnode.electricalpole.ElectricalPoleRender::new);
        ElnRenders.transparent(mods.eln.gridnode.transformer.GridTransformerDescriptor.class, mods.eln.gridnode.transformer.GridTransformerRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.autominer.AutoMinerDescriptor.class, mods.eln.transparentnode.autominer.AutoMinerRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.battery.BatteryDescriptor.class, mods.eln.transparentnode.battery.BatteryRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor.class, mods.eln.transparentnode.eggincubator.EggIncubatorRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor.class, mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor.class, mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor.class, mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor.class, mods.eln.transparentnode.electricalmachine.ElectricalMachineRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor.class, mods.eln.transparentnode.heatfurnace.HeatFurnaceRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor.class, mods.eln.transparentnode.powercapacitor.PowerCapacitorRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.powerinductor.PowerInductorDescriptor.class, mods.eln.transparentnode.powerinductor.PowerInductorRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.solarpanel.SolarPanelDescriptor.class, mods.eln.transparentnode.solarpanel.SolarPanelRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.teleporter.TeleporterDescriptor.class, mods.eln.transparentnode.teleporter.TeleporterRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor.class, mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor.class, mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.transformer.TransformerDescriptor.class, mods.eln.transparentnode.transformer.TransformerRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.turbine.TurbineDescriptor.class, mods.eln.transparentnode.turbine.TurbineRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.turret.TurretDescriptor.class, mods.eln.transparentnode.turret.TurretRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor.class, mods.eln.transparentnode.waterturbine.WaterTurbineRender::new);
        ElnRenders.transparent(mods.eln.transparentnode.windturbine.WindTurbineDescriptor.class, mods.eln.transparentnode.windturbine.WindTurbineRender::new);
    }
}
