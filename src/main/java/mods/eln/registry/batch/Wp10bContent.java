package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.IFunction;
import mods.eln.misc.Utils;
import mods.eln.misc.series.SerieEE;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;
import mods.eln.transparentnode.FuelGeneratorDescriptor;
import mods.eln.transparentnode.FuelHeatFurnaceDescriptor;
import mods.eln.transparentnode.LargeRheostatDescriptor;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxDescriptor;
import mods.eln.transparentnode.electricalantennatx.ElectricalAntennaTxDescriptor;
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor;
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterDescriptor;
import mods.eln.transparentnode.teleporter.TeleporterElement;
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor;
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor;
import mods.eln.transparentnode.turbine.TurbineDescriptor;
import mods.eln.transparentnode.turret.TurretDescriptor;
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor;
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;
import static mods.eln.registry.ElnDeviceRegistry.*;

/**
 * Device batch Wp10b (1.12 port): transparent-node generators and thermal: solar, wind, water, turbine, heat furnace, fuel generators, dissipators, large rheostat, antennas, teleporter, turret.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * Sources: m1-exclude-wp10b.txt (delete lines to build them). Not yet ported registrations:
 * registry/pending/Wp10bPending.java (excluded). To port a device: move its register method (and the fields it
 * sets) from Wp10bPending into this class, un-comment its PENDING line below, point device code at
 * Wp10bContent.&lt;field&gt;. Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp10bCases.
 */
public class Wp10bContent implements ElnContent {
    @Override
    public void preInit() {
        registerHeatFurnace(3);
        registerTurbineElectrical(4); // the two TurbineDescriptor sub-UIDs
        registerElectricalAntenna(7);
        registerSolarPanel(48);
        registerWindTurbine(49);
        registerThermalDissipatorPassiveAndActive(64);
        registerTransparentNodeMisc(65); // teleporter
        registerTurret(66);
        registerFuelGenerator(67);
        registerPassiveComponentRheostat(96); // large rheostat (uses a passive dissipator)
    }

    @Override
    public void serverAboutToStart() {
        TeleporterElement.teleporterList.clear();
    }

    @Override
    public void serverStopped() {
        TeleporterElement.teleporterList.clear();
    }

    public static void registerPassiveComponentRheostat(int id) {
        int subId, completId;
        String name;
        IFunction function;
        FunctionTableYProtect baseFunction = new FunctionTableYProtect(
            new double[]{0.0, 0.01, 0.03, 0.1, 0.2, 0.4, 0.8, 1.2}, 1.0,
            0, 5);

        {
            subId = 39;

            name = TR_NAME(Type.NONE, "Large Rheostat");

            ThermalDissipatorPassiveDescriptor dissipator = new ThermalDissipatorPassiveDescriptor(
                name,
                obj.getObj("LargeRheostat"),
                1000, -100,// double warmLimit,double coolLimit,
                4000, 800,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop
            );
            LargeRheostatDescriptor desc = new LargeRheostatDescriptor(
                name, dissipator, veryHighVoltageCableDescriptor, SerieEE.newE12(0)
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerHeatFurnace(int id) {
        int subId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Stone Heat Furnace");

            HeatFurnaceDescriptor desc = new HeatFurnaceDescriptor(name,
                "stonefurnace", 1000,
                Utils.getCoalEnergyReference() * 2 / 3,// double
                // nominalPower,
                // double
                // nominalCombustibleEnergy,
                2, 500,// int combustionChamberMax,double
                // combustionChamberPower,
                new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2) // thermal
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Fuel Heat Furnace");

            FuelHeatFurnaceDescriptor desc = new FuelHeatFurnaceDescriptor(name,
                obj.getObj("FuelHeater"), new ThermalLoadInitializerByPowerDrop(780, -100, 10, 2));
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerTurbineElectrical(int id) {
        int subId, completId;
        String name;

        FunctionTable TtoU = new FunctionTable(new double[]{0, 0.1, 0.85,
            1.0, 1.1, 1.15, 1.18, 1.19, 1.25}, 8.0 / 5.0);
        FunctionTable PoutToPin = new FunctionTable(new double[]{0.0, 0.2,
            0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7}, 8.0 / 5.0);

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "50V Turbine");
            double RsFactor = 0.1;
            double nominalU = LVU;
            double nominalP = 300 * heatTurbinePowerFactor;
            double nominalDeltaT = 250;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbineb", lowVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 8;
            name = TR_NAME(Type.NONE, "200V Turbine");
            double RsFactor = 0.10;
            double nominalU = MVU;
            double nominalP = 500 * heatTurbinePowerFactor;
            double nominalDeltaT = 350;
            TurbineDescriptor desc = new TurbineDescriptor(name, "turbinebblue", meduimVoltageCableDescriptor.render,
                TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                nominalU, nominalP, nominalP / 40, meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v");
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerSolarPanel(int id) {
        int subId, completId;
        GhostGroup ghostGroup;
        String name;

        FunctionTable diodeIfUBase;
        diodeIfUBase = new FunctionTableYProtect(new double[]{0.0, 0.002,
            0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.035, 0.04, 0.045,
            0.05, 0.06, 0.07, 0.08, 0.09, 0.10, 0.11, 0.12, 0.13, 1.0},
            1.0, 0, 1.0);

        FunctionTable solarIfSBase;
        solarIfSBase = new FunctionTable(new double[]{0.0, 0.1, 0.4, 0.6,
            0.8, 1.0}, 1);

        double LVSolarU = 59;

        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Small Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannel"), null,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, 65.0 * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 2, Math.PI / 2 // alphaMin alphaMax
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 2;
            name = TR_NAME(Type.NONE, "Small Rotating Solar Panel");

            ghostGroup = new GhostGroup();

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,// String
                // name,
                obj.getObj("smallsolarpannelrot"), lowVoltageCableDescriptor.render,
                ghostGroup, 0, 1, 0,// GhostGroup ghostGroup, int
                // solarOffsetX,int solarOffsetY,int
                // solarOffsetZ,
                // FunctionTable solarIfSBase,
                null, LVSolarU / 4, solarPanelBasePower * solarPanelPowerFactor,// double electricalUmax,double
                // electricalPmax,
                0.01,// ,double electricalDropFactor
                Math.PI / 4, Math.PI / 4 * 3 // alphaMin alphaMax
            );
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 3;
            name = TR_NAME(Type.NONE, "2x3 Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanel"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 0,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 2, Math.PI / 2
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            subId = 4;
            name = TR_NAME(Type.NONE, "2x3 Rotating Solar Panel");

            Coordonate groundCoordinate = new Coordonate(1, 0, 0, 0);

            ghostGroup = new GhostGroup();
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1);
            ghostGroup.removeElement(0, 0, 0);

            SolarPanelDescriptor desc = new SolarPanelDescriptor(name,
                obj.getObj("bigSolarPanelrot"), meduimVoltageCableDescriptor.render,
                ghostGroup, 1, 1, 1,
                groundCoordinate,
                LVSolarU * 2, solarPanelBasePower * solarPanelPowerFactor * 8,
                0.01,
                Math.PI / 8 * 3, Math.PI / 8 * 5
            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerWindTurbine(int id) {
        int subId, completId;
        String name;

        FunctionTable PfW = new FunctionTable(
            new double[]{0.0, 0.1, 0.3, 0.5, 0.8, 1.0, 1.1, 1.15, 1.2},
            8.0 / 5.0);
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Wind Turbine");

            WindTurbineDescriptor desc = new WindTurbineDescriptor(
                name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable,
                PfW,// PfW
                160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
                LVU * 1.18, 22,// double maxVoltage, double maxWind,
                3,// int offY,
                7, 2, 2,// int rayX,int rayY,int rayZ,
                2, 0.07,// int blockMalusMinCount,double blockMalus
                "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
            );

            GhostGroup g = new GhostGroup();
            g.addElement(0, 1, 0);
            g.addElement(0, 2, -1);
            g.addElement(0, 2, 1);
            g.addElement(0, 3, -1);
            g.addElement(0, 3, 1);
            g.addRectangle(0, 0, 1, 3, 0, 0);
            desc.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 16;
            name = TR_NAME(Type.NONE, "Water Turbine");

            Coordonate waterCoord = new Coordonate(1, -1, 0, 0);

            WaterTurbineDescriptor desc = new WaterTurbineDescriptor(
                name, obj.getObj("SmallWaterWheel"), // name,Obj3D obj,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                30 * waterTurbinePowerFactor,
                LVU * 1.18,
                waterCoord,
                "eln:water_turbine", 1f
            );

            GhostGroup g = new GhostGroup();

            g.addRectangle(1, 1, 0, 1, -1, 1);
            desc.setGhostGroup(g);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerFuelGenerator(int id) {
        int subId;
        {
            subId = 1;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(Type.NONE, "50V Fuel Generator"), obj.getObj("FuelGenerator50V"),
                    lowVoltageCableDescriptor, fuelGeneratorPowerFactor * 300, LVU * 1.05, fuelGeneratorTankCapacity);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
        {
            subId = 2;
            FuelGeneratorDescriptor descriptor =
                new FuelGeneratorDescriptor(TR_NAME(Type.NONE, "200V Fuel Generator"), obj.getObj("FuelGenerator200V"),
                    meduimVoltageCableDescriptor, fuelGeneratorPowerFactor * 1500, MVU * 1.05,
                    fuelGeneratorTankCapacity);
            transparentNodeItem.addDescriptor(subId + (id << 6), descriptor);
        }
    }

    public static void registerThermalDissipatorPassiveAndActive(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Small Passive Thermal Dissipator");

            ThermalDissipatorPassiveDescriptor desc = new ThermalDissipatorPassiveDescriptor(
                name,
                obj.getObj("passivethermaldissipatora"),
                200, -100,// double warmLimit,double coolLimit,
                250, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 32;
            name = TR_NAME(Type.NONE, "Small Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                obj.getObj("activethermaldissipatora"),
                LVU, 50,// double nominalElectricalU,double
                // electricalNominalP,
                800,// double nominalElectricalCoolingPower,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {
            subId = 34;
            name = TR_NAME(Type.NONE, "200V Active Thermal Dissipator");

            ThermalDissipatorActiveDescriptor desc = new ThermalDissipatorActiveDescriptor(
                name,
                obj.getObj("200vactivethermaldissipatora"),
                MVU, 60,// double nominalElectricalU,double
                // electricalNominalP,
                1200,// double nominalElectricalCoolingPower,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cableDescriptor,
                130, -100,// double warmLimit,double coolLimit,
                200, 30,// double nominalP,double nominalT,
                10, 1// double nominalTao,double nominalConnectionDrop

            );

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerTransparentNodeMisc(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Experimental Transporter");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-1, 0, 1, 0);
            powerLoad[1] = new Coordonate(-1, 0, -1, 0);

            GhostGroup doorOpen = new GhostGroup();
            doorOpen.addRectangle(-4, -3, 2, 2, 0, 0);

            GhostGroup doorClose = new GhostGroup();
            doorClose.addRectangle(-2, -2, 0, 1, 0, 0);

            TeleporterDescriptor desc = new TeleporterDescriptor(
                name, obj.getObj("Transporter"),
                highVoltageCableDescriptor,
                new Coordonate(-1, 0, 0, 0), new Coordonate(-1, 1, 0, 0),
                2,// int areaH
                powerLoad,
                doorOpen, doorClose

            );
            desc.setChargeSound("eln:transporter", 0.5f);
            GhostGroup g = new GhostGroup();
            g.addRectangle(-2, 0, 0, 1, -1, -1);
            g.addRectangle(-2, 0, 0, 1, 1, 1);
            g.addRectangle(-4, -1, 2, 2, 0, 0);
            g.addElement(0, 1, 0);
            //g.addElement(0, 2, 0);
            g.addElement(-1, 0, 0, ghostBlock, ghostBlock.tFloor);
		/*	g.addElement(1, 0, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 1, 0,ghostBlock,ghostBlock.tLadder);
			g.addElement(1, 2, 0,ghostBlock,ghostBlock.tLadder);*/
            g.addRectangle(-3, -3, 0, 1, -1, -1);
            g.addRectangle(-3, -3, 0, 1, 1, 1);
            // g.addElement(-4, 0, -1);
            // g.addElement(-4, 0, 1);

            desc.setGhostGroup(g);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

		/*if (Other.ccLoaded && ComputerProbeEnable) {
			subId = 4;
			name = "ComputerCraft Probe";

			ComputerCraftIoDescriptor desc = new ComputerCraftIoDescriptor(
					name,
					obj.getObj("passivethermaldissipatora")

					);

			transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
		}*/

    }

    public static void registerTurret(int id) {
        {
            int subId = 0;
            String name = TR_NAME(Type.NONE, "800V Defence Turret");

            TurretDescriptor desc = new TurretDescriptor(name, "Turret");

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }

    public static void registerElectricalAntenna(int id) {
        int subId, completId;
        String name;
        {

            subId = 0;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "Low Power Transmitter Antenna");
            double P = 250;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 200,// int
                // rangeMax,
                0.9, 0.7,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                LVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                lowVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 1;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "Low Power Receiver Antenna");
            double P = 250;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), LVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                LVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                lowVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 2;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "Medium Power Transmitter Antenna");
            double P = 1000;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 250,// int
                // rangeMax,
                0.9, 0.75,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                MVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                meduimVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 3;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "Medium Power Receiver Antenna");
            double P = 1000;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), MVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                MVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                meduimVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

        {

            subId = 4;
            ElectricalAntennaTxDescriptor desc;
            name = TR_NAME(Type.NONE, "High Power Transmitter Antenna");
            double P = 2000;
            desc = new ElectricalAntennaTxDescriptor(name,
                obj.getObj("lowpowertransmitterantenna"), 300,// int
                // rangeMax,
                0.95, 0.8,// double electricalPowerRatioEffStart,double
                // electricalPowerRatioEffEnd,
                HVU, P,// double electricalNominalVoltage,double
                // electricalNominalPower,
                HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                highVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {

            subId = 5;
            ElectricalAntennaRxDescriptor desc;
            name = TR_NAME(Type.NONE, "High Power Receiver Antenna");
            double P = 2000;
            desc = new ElectricalAntennaRxDescriptor(name,
                obj.getObj("lowpowerreceiverantenna"), HVU, P,// double
                // electricalNominalVoltage,double
                // electricalNominalPower,
                HVU * 1.3, P * 1.3,// electricalMaximalVoltage,double
                // electricalMaximalPower,
                highVoltageCableDescriptor);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
}
