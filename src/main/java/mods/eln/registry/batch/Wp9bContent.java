package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.sixnode.ScannerDescriptor;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmDescriptor;
import mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor;
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorDescriptor;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputDescriptor;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputDescriptor;
import mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterDescriptor;
import mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorDescriptor;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp9b (1.12 port): six-node sensors and IO: light/weather/wind/thermal/entity/fire/electrical sensors, scanner, vu meter, watch, alarm, redstone in/out, thermal cable.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * All wp9b registrations are ported (register methods below, verbatim from 1.7.10 Eln.java); sub-UIDs unchanged.
 * Selftest cases: mods.eln.selftest.cases.Wp9bCases.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public class Wp9bContent implements ElnContent {
    @Override
    public void preInit() {
        registerThermalCable(48);
        registerElectricalSensor(100);
        registerThermalSensor(101);
        registerElectricalVuMeter(102);
        registerElectricalAlarm(103);
        registerElectricalEnvironmentalSensor(104); // light, weather, wind, entity, fire, scanner
        registerElectricalRedstone(108);
        registerSixNodeMiscWatch(117); // electrical watches
    }

    public static void registerThermalCable(int id) {
        int subId, completId;
        String name;

        {
            subId = 0;

            name = "Removed from mod Copper Thermal Cable";

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 400, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            desc.addToData(false);
            desc.setDefaultIcon("empty-texture");
            sixNodeItem.addWithoutRegistry(subId + (id << 6), desc);

        }

        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Copper Thermal Cable");

            ThermalCableDescriptor desc = new ThermalCableDescriptor(name,
                1000 - 20, -200, // thermalWarmLimit, thermalCoolLimit,
                500, 2000, // thermalStdT, thermalStdPower,
                2, 10, 0.1,// thermalStdDrop, thermalStdLost, thermalTao,
                new CableRenderDescriptor("eln",
                    "sprites/tex_thermalcablebase.png", 4, 4),
                "Miaou !");// description

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerSixNodeMiscWatch(int id) {

        int subId, completId;
        String name;
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "Analog Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                obj.getObj("WallClock"),
                20000.0 / (3600 * 40)

            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 5;
            name = TR_NAME(Type.NONE, "Digital Watch");

            ElectricalWatchDescriptor desc = new ElectricalWatchDescriptor(
                name,
                obj.getObj("DigitalWallClock"),
                20000.0 / (3600 * 15)

            );

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalSensor(int id) {
        int subId, completId;
        String name;
        ElectricalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Electrical Probe");

            desc = new ElectricalSensorDescriptor(name, "electricalsensor",
                false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Voltage Probe");

            desc = new ElectricalSensorDescriptor(name, "voltagesensor", true);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerThermalSensor(int id) {
        int subId, completId;
        String name;
        ThermalSensorDescriptor desc;

        {
            subId = 0;

            name = TR_NAME(Type.NONE, "Thermal Probe");

            desc = new ThermalSensorDescriptor(name,
                obj.getObj("thermalsensor"), false);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;

            name = TR_NAME(Type.NONE, "Temperature Probe");

            desc = new ThermalSensorDescriptor(name,
                obj.getObj("temperaturesensor"), true);

            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalVuMeter(int id) {
        int subId, completId;
        String name;
        ElectricalVuMeterDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Analog vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Vumeter", false);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "LED vuMeter");
            desc = new ElectricalVuMeterDescriptor(name, "Led", true);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalAlarm(int id) {
        int subId, completId;
        String name;
        ElectricalAlarmDescriptor desc;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Nuclear Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                obj.getObj("alarmmedium"), 7, "eln:alarma", 11, 1f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Standard Alarm");
            desc = new ElectricalAlarmDescriptor(name,
                obj.getObj("alarmmedium"), 7, "eln:smallalarm_critical",
                1.2, 2f);
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalEnvironmentalSensor(int id) {
        int subId, completId;
        String name;
        {
            ElectricalLightSensorDescriptor desc;
            {
                subId = 0;
                name = TR_NAME(Type.NONE, "Electrical Daylight Sensor");
                desc = new ElectricalLightSensorDescriptor(name, obj.getObj("daylightsensor"), true);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
            {
                subId = 1;
                name = TR_NAME(Type.NONE, "Electrical Light Sensor");
                desc = new ElectricalLightSensorDescriptor(name, obj.getObj("lightsensor"), false);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWeatherSensorDescriptor desc;
            {
                subId = 4;
                name = TR_NAME(Type.NONE, "Electrical Weather Sensor");
                desc = new ElectricalWeatherSensorDescriptor(name, obj.getObj("electricalweathersensor"));
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalWindSensorDescriptor desc;
            {
                subId = 8;
                name = TR_NAME(Type.NONE, "Electrical Anemometer Sensor");
                desc = new ElectricalWindSensorDescriptor(name, obj.getObj("Anemometer"), 25);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalEntitySensorDescriptor desc;
            {
                subId = 12;
                name = TR_NAME(Type.NONE, "Electrical Entity Sensor");
                desc = new ElectricalEntitySensorDescriptor(name, obj.getObj("ProximitySensor"), 10);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 13;
                name = TR_NAME(Type.NONE, "Electrical Fire Detector");
                desc = new ElectricalFireDetectorDescriptor(name, obj.getObj("FireDetector"), 15, false);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ElectricalFireDetectorDescriptor desc;
            {
                subId = 14;
                name = TR_NAME(Type.NONE, "Electrical Fire Buzzer");
                desc = new ElectricalFireDetectorDescriptor(name, obj.getObj("FireDetector"), 15, true);
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
        {
            ScannerDescriptor desc;
            {
                subId = 15;
                name = TR_NAME(Type.NONE, "Scanner");
                desc = new ScannerDescriptor(name, obj.getObj("scanner"));
                sixNodeItem.addDescriptor(subId + (id << 6), desc);
            }
        }
    }

    public static void registerElectricalRedstone(int id) {
        int subId, completId;
        String name;
        {
            ElectricalRedstoneInputDescriptor desc;
            subId = 0;
            name = TR_NAME(Type.NONE, "Redstone-to-Voltage Converter");
            desc = new ElectricalRedstoneInputDescriptor(name, obj.getObj("redtoele"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            ElectricalRedstoneOutputDescriptor desc;
            subId = 1;
            name = TR_NAME(Type.NONE, "Voltage-to-Redstone Converter");
            desc = new ElectricalRedstoneOutputDescriptor(name,
                obj.getObj("eletored"));
            sixNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
}
