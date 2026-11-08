package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.transparentnode.autominer.AutoMinerDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.FunctionTableYProtect;
import mods.eln.misc.series.SerieEE;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sound.SoundCommand;
import mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor;
import mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor;
import mods.eln.transparentnode.electricalmachine.CompressorDescriptor;
import mods.eln.transparentnode.electricalmachine.MaceratorDescriptor;
import mods.eln.transparentnode.electricalmachine.MagnetizerDescriptor;
import mods.eln.transparentnode.electricalmachine.PlateMachineDescriptor;
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor;
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor;
import mods.eln.transparentnode.transformer.TransformerDescriptor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;

import static mods.eln.Eln.*;
import static mods.eln.registry.ElnDeviceRegistry.*;
import static mods.eln.i18n.I18N.*;

/**
 * Device batch Wp10a (1.12 port): transparent-node power and machines: transformer, power capacitor/inductor, electrical machines (macerator, compressor, magnetizer, plate machine), electrical furnace, egg incubator, autominer.
 * Called from ElnContentImpl (one line), after the core slice, in every lifecycle phase.
 * All wp10a devices are ported (registry/pending/Wp10aPending.java is gone). Keep the ids (sub-UIDs) unchanged. Selftest cases: mods.eln.selftest.cases.Wp10aCases.
 */
public class Wp10aContent implements ElnContent {
    @Override
    public void preInit() {
        registerPowerComponent(1);
        registerTransformer(2);
        registerElectricalFurnace(32);
        registerMacerator(33);
        registerCompressor(35);
        registerMagnetizer(36);
        registerPlateMachine(37);
        registerEggIncubator(41);
        registerAutoMiner(42);
        registerOilSeparator(80);
        registerOilPump(81);
        registerSteam(82);
        registerSmelter(82);
        registerColumn(83);
        registerAnvil(85);
        registerWireDrawing(88);
        registerResinCollector(92);
        registerCarbonBlackBurner(93);
        registerDesulfuriser(94);
        registerSteamCracker(95);
        registerPolymeriser(96);
    }

    /** The Polymeriser (the rubber chain's oil route, 2026-11-07): butadiene and Soul Dust to Raw Rubber, 230 V, 1 kW. Model
     *  mods.eln.cad.models.Polymeriser. */
    public static void registerPolymeriser(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.polymeriser.PolymeriserDescriptor(
            TR_NAME(Type.NONE, "Polymeriser"), "polymeriser", MVU, 1000, meduimVoltageCableDescriptor));
    }

    /** The Steam Cracker (the rubber chain's oil route, 2026-11-07): gasoline and steam to butadiene and natural gas, 400 V,
     *  4 kW. Model mods.eln.cad.models.SteamCracker. */
    public static void registerSteamCracker(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.steamcracker.SteamCrackerDescriptor(
            TR_NAME(Type.NONE, "Steam Cracker"), "steamcracker", HVU, 4000, highVoltageCableDescriptor));
    }

    /** The Desulfuriser (the rubber chain's oil route, 2026-11-07): in line on a cut's pipe, 230 V, 1 kW. Model
     *  mods.eln.cad.models.Desulfuriser. */
    public static void registerDesulfuriser(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.desulfuriser.DesulfuriserDescriptor(
            TR_NAME(Type.NONE, "Desulfuriser"), "desulfuriser", MVU, 1000, meduimVoltageCableDescriptor));
    }

    /** The Carbon Black Burner (the rubber chain's oil route, 2026-11-07). Model mods.eln.cad.models.CarbonBlackBurner. */
    public static void registerCarbonBlackBurner(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.carbonblack.CarbonBlackBurnerDescriptor(
            TR_NAME(Type.NONE, "Carbon Black Burner"), "carbonblackburner"));
    }

    /** The Industrial Resin Collector (Baughn 2026-11-07, the rubber chain): taps a trunk for latex on 230 V. Model
     *  mods.eln.cad.models.ResinCollector. */
    public static void registerResinCollector(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.resincollector.IndustrialResinCollectorDescriptor(
            TR_NAME(Type.NONE, "Industrial Resin Collector"), "resincollector230", MVU, 200, meduimVoltageCableDescriptor));
    }

    /**
     * The wire-drawing machines (Baughn 2026-11-06, Wires stage 4): cable kits and windings to any spec; the 48 V one up
     * to 6 mm² and 230 V insulation, the 230 V one everything. Model mods.eln.cad.models.WireDrawer.
     */
    public static void registerWireDrawing(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.transparentnode.wiredrawing.WireDrawingDescriptor(
            TR_NAME(Type.NONE, "48V Wire Drawing Machine"), "wiredrawer48", LVU, 400, lowVoltageCableDescriptor, 6, 2,
            mods.eln.node.voxel.CableSpec.BROWN));
        transparentNodeItem.addDescriptor(4 + (id << 6), new mods.eln.transparentnode.wiredrawing.WireDrawingDescriptor(
            TR_NAME(Type.NONE, "230V Wire Drawing Machine"), "wiredrawer230", MVU, 1600, meduimVoltageCableDescriptor,
            mods.eln.node.voxel.CableSpec.MM2.length - 1, 4, mods.eln.node.voxel.CableSpec.BLACK));
    }

    /** The electrical anvil (Baughn 2026-10-28): a workshop desk on 800 V; model mods.eln.cad.models.ElectricalAnvil. */
    public static void registerAnvil(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.anvil.ElectricalAnvilDescriptor(TR_NAME(Type.NONE, "Electrical Anvil"), "electricalanvil"));
        // the capacitor bank (Baughn 2026-10-28): one carries one of the anvil's bursts; model mods.eln.cad.models.CapacitorBank
        transparentNodeItem.addDescriptor(4 + (id << 6), new mods.eln.transparentnode.capacitorbank.CapacitorBankDescriptor(TR_NAME(Type.NONE, "Capacitor Bank"), "capacitorbank"));
    }

    /** 26.x oil: the fractionating column (base + segments) and the flare stack (placeholder models). */
    public static void registerColumn(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.oil.ColumnDescriptor(TR_NAME(Type.NONE, "Fractionating Column Base"), true, "columnbase"));
        transparentNodeItem.addDescriptor(4 + (id << 6), new mods.eln.oil.ColumnDescriptor(TR_NAME(Type.NONE, "Fractionating Column Segment"), false, "columnsegment"));
        transparentNodeItem.addDescriptor(8 + (id << 6), new mods.eln.oil.FlareStackDescriptor(TR_NAME(Type.NONE, "Flare Stack"), "flarestack"));
    }

    /** 26.x oil & steam: electric heaters and the boiler (placeholder models). */
    public static void registerSteam(int id) {
        // (a burnt heater gets a new heating corp, overvoltage or overheat)
        var heater50 = new mods.eln.oil.ElectricHeaterDescriptor(TR_NAME(Type.NONE, "48V Electric Heater"),
            LVU, 1000, lowVoltageCableDescriptor, "electricheater50v");
        heater50.repairPart = "48V Copper Heating Corp";
        transparentNodeItem.addDescriptor(0 + (id << 6), heater50);
        var heater200 = new mods.eln.oil.ElectricHeaterDescriptor(TR_NAME(Type.NONE, "230V Electric Heater"),
            MVU, 4000, meduimVoltageCableDescriptor, "electricheater200v");
        heater200.repairPart = "230V Copper Heating Corp";
        transparentNodeItem.addDescriptor(4 + (id << 6), heater200);
        transparentNodeItem.addDescriptor(8 + (id << 6), new mods.eln.oil.BoilerDescriptor(TR_NAME(Type.NONE, "Boiler"), "boiler"));
        // Baughn: small steam turbines with the generator built in; they replace the heat turbines (now uncraftable)
        transparentNodeItem.addDescriptor(12 + (id << 6), new mods.eln.oil.SteamGeneratorDescriptor(TR_NAME(Type.NONE, "48V Steam Turbine"),
            LVU, 1000, lowVoltageCableDescriptor, "steamturbine50v"));
        transparentNodeItem.addDescriptor(16 + (id << 6), new mods.eln.oil.SteamGeneratorDescriptor(TR_NAME(Type.NONE, "230V Steam Turbine"),
            MVU, 4000, meduimVoltageCableDescriptor, "steamturbine200v"));
        // the steam oven (Baughn 2026-10-23): food cooked with a pipe's steam; model mods.eln.cad.models.SteamOven
        transparentNodeItem.addDescriptor(20 + (id << 6), new mods.eln.oil.SteamOvenDescriptor(TR_NAME(Type.NONE, "Steam Oven"), "steamoven"));
        // the turbines' condensers (Baughn 2026-10-26: optional, beside the turbine, the models join); model mods.eln.cad.models.Condenser
        transparentNodeItem.addDescriptor(28 + (id << 6), new mods.eln.oil.CondenserDescriptor(TR_NAME(Type.NONE, "Small Steam Condenser"),
            1000, 0.005, "condenser50v"));   // (0.005, 0.02: the vessels before 12 buckets, for old saves)
        transparentNodeItem.addDescriptor(32 + (id << 6), new mods.eln.oil.CondenserDescriptor(TR_NAME(Type.NONE, "Steam Condenser"),
            4000, 0.02, "condenser200v"));
        // the bootstrap turbine (Baughn 2026-10-27): heat and water in, 50 V out, its control and condenser built in
        transparentNodeItem.addDescriptor(36 + (id << 6), new mods.eln.oil.CompactTurbineDescriptor(TR_NAME(Type.NONE, "48V Compact Steam Turbine"),
            LVU, 300, lowVoltageCableDescriptor, "compactturbine50v"));
        // the shaft redo (Baughn 2026-10-29): tanks (steam drum, separator, hotwell) and an inline feed pump
        transparentNodeItem.addDescriptor(40 + (id << 6), new mods.eln.oil.TankDescriptor(TR_NAME(Type.NONE, "Iron Tank"),
            mods.eln.oil.TankDescriptor.bucketsVolume(12), 10e5, 40e5, "tankiron"));   // (12 buckets a block: Baughn 2026-10-29)
        transparentNodeItem.addDescriptor(44 + (id << 6), new mods.eln.oil.TankDescriptor(TR_NAME(Type.NONE, "Alloy Tank"),
            mods.eln.oil.TankDescriptor.bucketsVolume(12), 100e5, 300e5, "tankalloy"));
        transparentNodeItem.addDescriptor(48 + (id << 6), new mods.eln.oil.InlinePumpDescriptor(TR_NAME(Type.NONE, "230V Inline Pump"),
            MVU, meduimVoltageCableDescriptor, "inlinepump200v"));
        // ... and the plant condenser for the shaft turbines (the add-on condensers above stay as they are)
        transparentNodeItem.addDescriptor(52 + (id << 6), new mods.eln.oil.PlantCondenserDescriptor(TR_NAME(Type.NONE, "Plant Steam Condenser"),
            0.06, 40000, mods.eln.mechanical.SteamTurbineSpec.HP, "plantcondenser"));
        // the counterflow heat exchanger (economizer, reheater, steam raiser: Baughn 2026-10-29): 12.5 W/K a block, 50 a 2 x 2 slice; 8 L a side a block
        transparentNodeItem.addDescriptor(56 + (id << 6), new mods.eln.oil.HeatExchangerDescriptor(TR_NAME(Type.NONE, "Counterflow Heat Exchanger"),
            mods.eln.oil.HeatExchangerDescriptor.SLICE_UA / 4, 0.008, "heatexchanger"));
    }

    /** 26.x oil: the pump (on well casing; oil lakes and water). Model: mods.eln.cad.models.OilPump. */
    public static void registerOilPump(int id) {
        transparentNodeItem.addDescriptor(0 + (id << 6), new mods.eln.oil.OilPumpDescriptor(TR_NAME(Type.NONE, "48V Oil Pump"),
            LVU, 1000, lowVoltageCableDescriptor, "oilpump50v"));
        transparentNodeItem.addDescriptor(4 + (id << 6), new mods.eln.oil.OilPumpDescriptor(TR_NAME(Type.NONE, "230V Oil Pump"),
            MVU, 4000, meduimVoltageCableDescriptor, "oilpump200v"));
        // the steam injection wellhead (todo #138; Baughn 2026-10-30: enhanced recovery by steam, not water)
        transparentNodeItem.addDescriptor(16 + (id << 6), new mods.eln.oil.SteamInjectionDescriptor(TR_NAME(Type.NONE, "Steam Injection Wellhead"),
            "oilpump50v"));
        // Baughn 2026-10-25: small waterlogged pumps for a boiler's feed, a few turbines' worth of water each
        transparentNodeItem.addDescriptor(8 + (id << 6), new mods.eln.oil.SubmersiblePumpDescriptor(TR_NAME(Type.NONE, "48V Submersible Pump"),
            LVU, 100, 1000, 6e5, lowVoltageCableDescriptor, "submersiblepump50v"));
        transparentNodeItem.addDescriptor(12 + (id << 6), new mods.eln.oil.SubmersiblePumpDescriptor(TR_NAME(Type.NONE, "230V Submersible Pump"),
            MVU, 400, 4000, 15e5, meduimVoltageCableDescriptor, "submersiblepump200v"));
    }

    /** 26.x oil: the tar sand separator (notes/oil; model mods.eln.cad.models.TarSandSeparator, drawn as a macerator). */
    public static void registerOilSeparator(int id) {
        {
            String name = TR_NAME(Type.NONE, "48V Tar Sand Separator");
            var desc = new mods.eln.transparentnode.electricalmachine.OilSeparatorDescriptor(name, "tarsandseparator50v", LVU, 200, LVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0), lowVoltageCableDescriptor, mods.eln.Eln.separatorRecipes);
            transparentNodeItem.addDescriptor(0 + (id << 6), desc);
            desc.setRunningSound("eln:separator");   // (Baughn's recording, 2026-11-03: squishier than the macerator)
        }
        {
            String name = TR_NAME(Type.NONE, "230V Tar Sand Separator");
            var desc = new mods.eln.transparentnode.electricalmachine.OilSeparatorDescriptor(name, "tarsandseparator200v", MVU, 2000, MVU * 1.25,
                new ThermalLoadInitializer(80, -100, 10, 100000.0), meduimVoltageCableDescriptor, mods.eln.Eln.separatorRecipes);
            transparentNodeItem.addDescriptor(4 + (id << 6), desc);
            desc.setRunningSound("eln:separator");   // (Baughn's recording, 2026-11-03: squishier than the macerator)
        }
    }

    public static ElectricalFurnaceDescriptor electricalFurnace;

    @Override
    public void serverAboutToStart() {
        furnaceList.clear();
        furnaceList.add(new ItemStack(Blocks.FURNACE));
        furnaceList.add(electricalFurnace.newItemStack());
    }

    public static void registerPowerComponent(int id) {
        int subId, completId;
        String name;

        {
            subId = 16;

            name = TR_NAME(Type.NONE, "Power inductor");

            PowerInductorDescriptor desc = new PowerInductorDescriptor(
                name, null, SerieEE.newE12(-1)
            );

            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }

        {
            subId = 20;

            name = TR_NAME(Type.NONE, "Power capacitor");

            PowerCapacitorDescriptor desc = new PowerCapacitorDescriptor(
                name, null, SerieEE.newE6(-2), 300
            );

            transparentNodeItem.addWithoutRegistry(subId + (id << 6), desc);
        }
    }

    public static void registerTransformer(int id) {
        int subId;
        String name;

        {
            subId = 0;
            name = TR_NAME(Type.NONE, "DC-DC Converter");

            TransformerDescriptor desc = new TransformerDescriptor(name, obj.getObj("transformator"),
                obj.getObj("feromagneticcorea"), obj.getObj("transformatorCase"), 0.5f);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
        {
            // Baughn 2026-11-06: a signal on its front taps the secondary
            subId = 1;
            name = TR_NAME(Type.NONE, "Variable DC-DC Converter");
            TransformerDescriptor desc = new mods.eln.transparentnode.transformer.VariableTransformerDescriptor(name,
                obj.getObj("transformator"), obj.getObj("feromagneticcorea"), obj.getObj("transformatorCase"), 0.5f);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerElectricalFurnace(int id) {
        int subId, completId;
        String name;
        // 26.x port: furnaceList is filled in serverAboutToStart (no ItemStacks before components are bound)
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Electrical Furnace");
            double[] PfTTable = new double[]{0, 20, 40, 80, 160, 240, 360,
                540, 756, 1058.4, 1481.76};

            double[] thermalPlostfTTable = new double[PfTTable.length];
            for (int idx = 0; idx < thermalPlostfTTable.length; idx++) {
                thermalPlostfTTable[idx] = PfTTable[idx]
                    * Math.pow((idx + 1.0) / thermalPlostfTTable.length, 2)
                    * 2;
            }

            FunctionTableYProtect PfT = new FunctionTableYProtect(PfTTable,
                800.0, 0, 100000.0);

            FunctionTableYProtect thermalPlostfT = new FunctionTableYProtect(
                thermalPlostfTTable, 800.0, 0.001, 10000000.0);

            ElectricalFurnaceDescriptor desc = new ElectricalFurnaceDescriptor(
                name, PfT, thermalPlostfT,// thermalPlostfT;
                40// thermalC;
            );
            electricalFurnace = desc;
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            // Utils.smeltRecipeList.addMachine(desc.newItemStack());
        }
        // Utils.smeltRecipeList.addMachine(new ItemStack(Blocks.FURNACE));
    }

    /** 26.x (Baughn 2026-10-23): the smelter, heated through a thermal cable; it smelts at the electrical oven's rate for its
     *  temperature (the same PfT table). Model: mods.eln.cad.models.Smelter. */
    public static void registerSmelter(int id) {
        double[] PfTTable = {0, 20, 40, 80, 160, 240, 360, 540, 756, 1058.4, 1481.76};
        double[] lost = new double[PfTTable.length];
        for (int idx = 0; idx < lost.length; idx++) lost[idx] = PfTTable[idx] * Math.pow((idx + 1.0) / lost.length, 2) * 2;
        transparentNodeItem.addDescriptor(24 + (id << 6), new mods.eln.transparentnode.smelter.SmelterDescriptor(TR_NAME(Type.NONE, "Smelter"),
            new FunctionTableYProtect(PfTTable, 800.0, 0, 100000.0), new FunctionTableYProtect(lost, 800.0, 0.001, 10000000.0), 40, "smelter"));
    }

    public static void registerMacerator(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "48V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratora", LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                maceratorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "230V Macerator");

            MaceratorDescriptor desc = new MaceratorDescriptor(name,
                "maceratorb", MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                maceratorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:macerator");
        }
    }

    public static void registerPlateMachine(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "48V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                obj.getObj("platemachinea"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                plateMachineRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "230V Plate Machine");

            PlateMachineDescriptor desc = new PlateMachineDescriptor(
                name,// String name,
                obj.getObj("platemachineb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                plateMachineRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:plate_machine");

        }
    }

    public static void registerEggIncubator(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "48V Egg Incubator");

            EggIncubatorDescriptor desc = new EggIncubatorDescriptor(
                name, obj.getObj("eggincubator"),
                lowVoltageCableDescriptor,
                LVU, 50);
            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }

    }

    public static void registerCompressor(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "48V Compressor");

            CompressorDescriptor desc = new CompressorDescriptor(
                name,// String name,
                obj.getObj("compressora"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                compressorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "230V Compressor");

            // (the rubber chain, 2026-11-07: latex ports on its back and the dandelion recipe)
            CompressorDescriptor desc = new mods.eln.transparentnode.electricalmachine.CompressorPortsDescriptor(
                name,// String name,
                obj.getObj("compressorb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                compressorRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
            desc.setRunningSound("eln:compressor_run");
            desc.setEndSound(new SoundCommand("eln:compressor_end"));
        }
    }

    public static void registerMagnetizer(int id) {

        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "48V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                obj.getObj("magnetizera"),
                LVU, 200,// double nominalU,double nominalP,
                LVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                lowVoltageCableDescriptor,// ElectricalCableDescriptor cable
                magnetiserRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:motor");
        }

        {
            subId = 4;
            name = TR_NAME(Type.NONE, "230V Magnetizer");

            MagnetizerDescriptor desc = new MagnetizerDescriptor(
                name,// String name,
                obj.getObj("magnetizerb"),
                MVU, 2000,// double nominalU,double nominalP,
                MVU * 1.25,// double maximalU,
                new ThermalLoadInitializer(80, -100, 10, 100000.0),// thermal,
                meduimVoltageCableDescriptor,// ElectricalCableDescriptor
                // cable
                magnetiserRecipes);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);

            desc.setRunningSound("eln:motor");
        }
    }

    public static void registerAutoMiner(int id) {
        int subId, completId;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Auto Miner");

            Coordonate[] powerLoad = new Coordonate[2];
            powerLoad[0] = new Coordonate(-2, -1, 1, 0);
            powerLoad[1] = new Coordonate(-2, -1, -1, 0);

            Coordonate lightCoord = new Coordonate(-3, 0, 0, 0);

            Coordonate miningCoord = new Coordonate(-1, 0, 1, 0);

            AutoMinerDescriptor desc = new AutoMinerDescriptor(name,
                obj.getObj("AutoMiner"),
                powerLoad, lightCoord, miningCoord,
                2, 1, 0,
                highVoltageCableDescriptor,
                1, 50// double pipeRemoveTime,double pipeRemoveEnergy
            );

            GhostGroup ghostGroup = new GhostGroup();

            ghostGroup.addRectangle(-2, -1, -1, 0, -1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, 1, 1);
            ghostGroup.addRectangle(1, 1, -1, 0, -1, -1);
            ghostGroup.addElement(1, 0, 0);
            ghostGroup.addElement(0, 0, 1);
            ghostGroup.addElement(0, 1, 0);
            ghostGroup.addElement(0, 0, -1);
            ghostGroup.removeElement(-1, -1, 0);

            desc.setGhostGroup(ghostGroup);

            transparentNodeItem.addDescriptor(subId + (id << 6), desc);
        }
    }
}
