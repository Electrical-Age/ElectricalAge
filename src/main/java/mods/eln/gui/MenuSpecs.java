package mods.eln.gui;

import mods.eln.item.*;
import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor;
import mods.eln.sixnode.thermalcable.ThermalCableDescriptor;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static mods.eln.gui.SlotSpec.*;
import static mods.eln.i18n.I18N.tr;

/**
 * The device slots of every EA GUI with a container: the 1.12 XContainer constructors (legacy/), as data, by
 * descriptor class (P4). A descriptor without an entry has a GUI without a container (1.12 packetOpenLocalGui), if any.
 * Slot ids are the ones the elements use (the XContainer constant classes).
 */
public final class MenuSpecs {
    private MenuSpecs() {
    }

    static final Map<Class<?>, Function<Object, MenuSpec>> BY_DESCRIPTOR = new LinkedHashMap<>();

    static <D> void put(Class<D> descriptor, Function<D, MenuSpec> spec) {
        BY_DESCRIPTOR.put(descriptor, d -> spec.apply(descriptor.cast(d)));
    }

    /** The menu of a device descriptor (the closest registered class), or SCREEN_ONLY. Called on both sides. */
    public static MenuSpec of(Object descriptor) {
        for (Class<?> c = descriptor.getClass(); c != null; c = c.getSuperclass()) {
            Function<Object, MenuSpec> f = BY_DESCRIPTOR.get(c);
            if (f != null) return f.apply(descriptor);
        }
        return MenuSpec.SCREEN_ONLY;
    }

    /** The registered class a descriptor's spec comes from, or null. */
    public static Class<?> registeredClass(Object descriptor) {
        for (Class<?> c = descriptor.getClass(); c != null; c = c.getSuperclass()) if (BY_DESCRIPTOR.containsKey(c)) return c;
        return null;
    }

    public static java.util.Set<Class<?>> registered() {
        return BY_DESCRIPTOR.keySet();
    }

    static final Class<?>[] CABLE = {ElectricalCableDescriptor.class};

    static SlotSpec cableSlot(int id, int x, int y, int limit) {
        return six(id, x, y, limit, CABLE, MEDIUM, tr("Electrical cable slot"));
    }

    static {
        // ---- six-node devices
        // Baughn 2026-10-17: no more "a piece of cable in the GUI before it works" (notes/port26/cable-slots.md). The
        // ground cable, energy meters, electrical/voltage probes, thermal probe and lamp sockets lost their cable slot
        // (built-in cable); the ground cable lost its GUI. Kept: breaker (trip rating), hub (which sides), transformer
        // (turns); the lamp supply's and temperature probe's slots are optional (range, mode).
        put(mods.eln.sixnode.electricalbreaker.ElectricalBreakerDescriptor.class, d -> MenuSpec.of(
            cableSlot(0, 150, 21, 1)).panel(176, 140, 8, 58));
        put(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerContainer.paperSlotId, 176 / 2 - 44, 125, 64,
                item(Items.PAPER), MEDIUM, "Paper Slot"),
            descriptor(mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerContainer.printSlotId, 176 / 2 + 45 - 17, 125, 1,
                DataLogsPrintDescriptor.class, MEDIUM)).panel(176, 230, 8, 148));
        put(mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.sixnode.electricalentitysensor.ElectricalEntitySensorContainer.filterId, 184 / 2 - 12, 8, 1,
                EntitySensorFilterDescriptor.class, MEDIUM, tr("Entity filter slot"))).panel(176, 114, 8, 32));
        put(mods.eln.sixnode.electricalmath.ElectricalMathDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.sixnode.electricalmath.ElectricalMathContainer.restoneSlotId, 125 + 27 + 44 / 2, 25, 64,
                mods.eln.sixnode.electricalmath.ElectricalMathElement::isChip, MEDIUM, tr("Cheap chip slot"))).panel(220, 128, 30, 46));
        put(mods.eln.sixnode.electricalsensor.ElectricalSensorDescriptor.class, d -> MenuSpec.of()
            .panel(176, d.voltageOnly ? 121 : 166, 8, d.voltageOnly ? 39 : 84));
        put(mods.eln.sixnode.electricalwatch.ElectricalWatchDescriptor.class, d -> MenuSpec.of(
            descriptor(0, 184 / 2 - 12, 8, 1, BatteryItem.class, MEDIUM, tr("Portable battery slot"))).panel(176, 114, 8, 32));
        put(mods.eln.sixnode.energymeter.EnergyMeterDescriptor.class, d -> MenuSpec.of().panel(192, 208, 16, 126));
        put(mods.eln.sixnode.hub.HubDescriptor.class, d -> MenuSpec.of(
            cableSlot(0, 176 / 2 + 80 / 2 + 2, 134 / 2 - 9, 1),
            cableSlot(1, 176 / 2 - 80 / 2 - 20, 134 / 2 - 9, 1),
            cableSlot(2, 176 / 2 - 9, 134 / 2 + 80 / 2 + 2, 1),
            cableSlot(3, 176 / 2 - 9, 134 / 2 - 80 / 2 - 20, 1)).panel(176, 216, 8, 134));
        put(mods.eln.sixnode.lampsocket.LampSocketDescriptor.class, d -> MenuSpec.of(
            lamp(mods.eln.sixnode.lampsocket.LampSocketContainer.lampSlotId, 70, 57, 1, d.socketType)));
        put(mods.eln.sixnode.lampsupply.LampSupplyDescriptor.class, d -> MenuSpec.of(
            six(0, 184, 144, 64, CABLE, MEDIUM,
                tr("Electrical cable slot (optional)\nBase range is 32 blocks.\nEach cable\nincreases range by one.").split("\n"))).panel(220, 205, 8, 125));
        put(mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixContainer.redId, 132, 8, 13, item(Items.REDSTONE), MEDIUM,
                tr("Redstone slot"), tr("(Increases capacity)")),
            descriptor(mods.eln.sixnode.powercapacitorsix.PowerCapacitorSixContainer.dielectricId, 132 + 20, 8, 20, DielectricItem.class,
                MEDIUM, tr("Dielectric slot"), tr("(Increases maximum voltage)"))).panel(176, 112, 8, 30));
        put(mods.eln.sixnode.powerinductorsix.PowerInductorSixDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.sixnode.powerinductorsix.PowerInductorSixContainer.cableId, 132, 8, 19, CopperCableDescriptor.class,
                MEDIUM, tr("Copper cable slot"), tr("(Increases inductance)")),
            descriptor(mods.eln.sixnode.powerinductorsix.PowerInductorSixContainer.coreId, 132 + 20, 8, 1,
                FerromagneticCoreDescriptor.class, MEDIUM, tr("Ferromagnetic core slot"))).panel(176, 112, 8, 30));
        put(mods.eln.sixnode.powersocket.PowerSocketDescriptor.class, d -> MenuSpec.of().panel(176, 144, 8, 60));
        put(mods.eln.sixnode.resistor.ResistorDescriptor.class, d -> resistor());
        // the thermal probe: built-in Copper Thermal Cable; the temperature probe: an optional mode slot
        put(mods.eln.sixnode.thermalsensor.ThermalSensorDescriptor.class, d -> d.temperatureOnly ? MenuSpec.of(
            six(0, 152, 62, 1, new Class<?>[]{ThermalCableDescriptor.class, ElectricalCableDescriptor.class}, MEDIUM,
                tr("Cable slot (optional)"), tr("Empty: on electrical cables"), tr("Thermal cable: on thermal cables")))
            : MenuSpec.of());
        put(mods.eln.sixnode.batterycharger.BatteryChargerDescriptor.class, d -> MenuSpec.of(
            battery(0, 26 - 18 + 0, 6 + 0),
            battery(1, 26 - 18 + 18, 6 + 0),
            battery(2, 26 - 18 + 0, 6 + 18),
            battery(3, 26 - 18 + 18, 6 + 18),
            descriptor(mods.eln.sixnode.batterycharger.BatteryChargerContainer.boosterSlotId, 80 - 18, 6 + 18 / 2, 5,
                MachineBoosterDescriptor.class, MEDIUM, tr("Booster slot"))).panel(176, 126, 8, 44));
        put(mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorDescriptor.class, d -> d.batteryPowered ? MenuSpec.of(
            descriptor(0, 184 / 2 - 12, 8, 1, BatteryItem.class, MEDIUM, tr("Portable battery slot"))) : MenuSpec.SCREEN_ONLY);

        // ---- transparent-node devices
        put(mods.eln.transparentnode.autominer.AutoMinerDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.transparentnode.autominer.AutoMinerContainer.electricalDrillSlotId, 134, 8, 1,
                ElectricalDrillDescriptor.class, MEDIUM, tr("Drill slot")),
            descriptor(1, 3000, 3000, 1, OreScanner.class, MEDIUM, tr("Ore scanner slot")),
            descriptor(mods.eln.transparentnode.autominer.AutoMinerContainer.MiningPipeSlotId, 134 + 18, 8, 64,
                MiningPipeDescriptor.class, MEDIUM, tr("Mining pipe slot"))).panel(176, 112, 8, 30));
        put(mods.eln.transparentnode.battery.BatteryDescriptor.class, d -> MenuSpec.of(
            descriptor(0, 130, 40, 1, OverVoltageProtectionDescriptor.class, MEDIUM, tr("Overvoltage protection")),
            descriptor(1, 130, 60, 1, OverHeatingProtectionDescriptor.class, MEDIUM, tr("Overheating protection"))).publishing(1));
        put(mods.eln.transparentnode.eggincubator.EggIncubatorDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.transparentnode.eggincubator.EggIncubatorContainer.EggSlotId, 176 / 2 - 8, 7, 64, mods.eln.transparentnode.eggincubator.EggIncubatorElement::incubates,
                MEDIUM, tr("Egg slot"))).publishing(1).panel(202, 111, 8, 29));
        // P4b layout (ElectricalFurnaceScreen): input -> arrow -> output, heater and regulator to the right
        put(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceDescriptor.class, d -> MenuSpec.of(
            SlotSpec.output(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.outSlotId, 86, 25, BIG),
            SlotSpec.smeltable(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.inSlotId, 28, 25, MEDIUM, tr("Input")),
            descriptor(2, 130, 25, 1, HeatingCorpElement.class, MEDIUM, tr("Heating element slot")),
            descriptor(3, 80 + 18, -2000, 1, ThermalIsolatorElement.class, MEDIUM, tr("Thermal isolator slot")),
            regulator(4, 150, 25, 1, new RegulatorType[]{RegulatorType.OnOff, RegulatorType.Analog}, MEDIUM,
                tr("Regulator slot"))).publishing(1).inventoryAt(122));
        // the electrical anvil (Baughn 2026-10-28): the tool (one, mended in place) and its repair material
        put(mods.eln.anvil.ElectricalAnvilDescriptor.class, d -> MenuSpec.of(
            SlotSpec.filter(mods.eln.anvil.ElectricalAnvilElement.TOOL, 28, 25, 1, s -> s.isDamageableItem() || s.getMaxStackSize() == 1, BIG, tr("Tool")),
            SlotSpec.filter(mods.eln.anvil.ElectricalAnvilElement.MATERIAL, 86, 25, 64, mods.eln.anvil.ElectricalAnvilElement::materialMayGo, MEDIUM,
                tr("Repair material"))).publishing(1).inventoryAt(150));
        put(mods.eln.oil.SteamOvenDescriptor.class, d -> MenuSpec.of(
            SlotSpec.output(mods.eln.oil.SteamOvenElement.outSlotId, 86, 25, BIG),
            SlotSpec.smeltable(mods.eln.oil.SteamOvenElement.inSlotId, 28, 25, MEDIUM, tr("Input"))).publishing(1).inventoryAt(122));
        put(mods.eln.transparentnode.smelter.SmelterDescriptor.class, d -> {
            int out = mods.eln.transparentnode.smelter.SmelterElement.outSlotId, n = mods.eln.transparentnode.smelter.SmelterElement.OUT_SLOTS;
            SlotSpec[] s = new SlotSpec[1 + n];
            s[0] = SlotSpec.smeltable(mods.eln.transparentnode.smelter.SmelterElement.inSlotId, 28, 25, MEDIUM, tr("Input"));
            for (int i = 0; i < n; i++) s[1 + i] = SlotSpec.output(out + i, 86 + i * 18, 25, MEDIUM);
            return MenuSpec.of(s).publishing(1).inventoryAt(122);
        });
        put(mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor.class, d -> {
            SlotSpec[] s = new SlotSpec[2 + d.outStackCount];
            for (int idx = 0; idx < d.outStackCount; idx++) s[idx] = SlotSpec.output(idx, 130 - 32 + idx * 18, 12, MEDIUM);
            // the input: only what this machine has a recipe for (Baughn: shift-click ignored which slot made sense)
            s[d.outStackCount] = filter(d.outStackCount, 8 + 36, 12, 64, st -> d.recipe.accepts(st.copyWithCount(st.getMaxStackSize())), MEDIUM, tr("Input"));
            s[d.outStackCount + 1] = descriptor(d.outStackCount + 1, 8, 12, 5, MachineBoosterDescriptor.class, MEDIUM, tr("Booster slot"));
            return MenuSpec.of(s).publishing(1).panel(204, 122, 8, 40);
        });
        // the steam turbines (Baughn 2026-10-26): a regulator holds a target inlet pressure
        put(mods.eln.oil.SteamGeneratorDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.oil.SteamGeneratorElement.REGULATOR_SLOT, 8, 58, 1, IRegulatorDescriptor.class, MEDIUM, tr("Regulator slot"),
                tr("Holds a target inlet pressure"))).publishing(0));
        put(mods.eln.mechanical.SteamTurbineDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.oil.SteamValveControl.REGULATOR_SLOT, 8, 58, 1, IRegulatorDescriptor.class, MEDIUM, tr("Regulator slot"),
                tr("Holds a target inlet pressure"))).publishing(0));
        put(mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer.combustibleId, 70, 58, 64,
                s -> mods.eln.misc.Utils.getItemEnergie(s) > 0, MEDIUM, tr("Fuel slot")),
            descriptor(mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer.regulatorId, 8, 58, 1, IRegulatorDescriptor.class,
                MEDIUM, tr("Regulator slot")),
            descriptor(mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer.combustrionChamberId, 8 + 18, 58,
                d.combustionChamberMax, CombustionChamber.class, MEDIUM, tr("Combustion chamber slot")),
            descriptor(mods.eln.transparentnode.heatfurnace.HeatFurnaceContainer.isolatorId, 8 + 36, 58, 1,
                ThermalIsolatorElement.class, MEDIUM, tr("Thermal isolator slot"))).publishing(0));
        put(mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor.class, d -> MenuSpec.of(
            filter(mods.eln.transparentnode.powercapacitor.PowerCapacitorContainer.redId, 132, 8, 13, item(Items.REDSTONE), MEDIUM,
                tr("Redstone slot"), tr("(Increases capacity)")),
            descriptor(mods.eln.transparentnode.powercapacitor.PowerCapacitorContainer.dielectricId, 132 + 20, 8, 20,
                DielectricItem.class, MEDIUM, tr("Dielectric slot"), tr("(Increases maximal voltage)"))).panel(176, 112, 8, 30));
        put(mods.eln.transparentnode.powerinductor.PowerInductorDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.transparentnode.powerinductor.PowerInductorContainer.cableId, 132, 8, 19, CopperCableDescriptor.class,
                MEDIUM, tr("Copper cable slot"), tr("(Increases inductance)")),
            descriptor(mods.eln.transparentnode.powerinductor.PowerInductorContainer.coreId, 132 + 20, 8, 1,
                FerromagneticCoreDescriptor.class, MEDIUM, tr("Ferromagnetic core slot"))).panel(176, 112, 8, 30));
        put(mods.eln.transparentnode.solarpanel.SolarPanelDescriptor.class, d -> !d.canRotate ? MenuSpec.SCREEN_ONLY : MenuSpec.of(
            descriptor(mods.eln.transparentnode.solarpanel.SolarPanelContainer.trackerSlotId, 176 / 2 - 20, 35, 1,
                SolarTrackerDescriptor.class, MEDIUM, tr("Solar tracker slot"))).publishing(0));
        put(mods.eln.transparentnode.transformer.TransformerDescriptor.class, d -> MenuSpec.of(
            six(mods.eln.transparentnode.transformer.TransformerContainer.primaryCableSlotId, 58, 30, 4, CABLE, MEDIUM,
                tr("Electrical cable slot")),
            six(mods.eln.transparentnode.transformer.TransformerContainer.secondaryCableSlotId, 100, 30, 4, CABLE, MEDIUM,
                tr("Electrical cable slot")),
            descriptor(mods.eln.transparentnode.transformer.TransformerContainer.ferromagneticSlotId, 58 + (100 - 58) / 2, 30, 1,
                FerromagneticCoreDescriptor.class, MEDIUM, tr("Ferromagnetic core slot")),
            descriptor(mods.eln.transparentnode.transformer.TransformerContainer.CasingSlotId, 130, 74, 1, CaseItemDescriptor.class,
                MEDIUM, tr("Casing slot"))).panel(176, 181, 8, 99));
        put(mods.eln.transparentnode.turret.TurretDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.transparentnode.turret.TurretContainer.filterId, 120, 8, 1, EntitySensorFilterDescriptor.class,
                MEDIUM, tr("Entity filter slot"))).panel(176, 134, 8, 52));
        put(mods.eln.transparentnode.FuelHeatFurnaceDescriptor.class, d -> MenuSpec.of(
            descriptor(mods.eln.transparentnode.FuelHeatFurnaceContainer.Companion.getFuelBurnerSlot(), 26, 58, 1,
                FuelBurnerDescriptor.class, MEDIUM, tr("Fuel burner slot")),
            regulator(mods.eln.transparentnode.FuelHeatFurnaceContainer.Companion.getRegulatorSlot(), 8, 58, 1,
                new RegulatorType[]{RegulatorType.Analog}, MEDIUM, tr("Analog regulator slot"))).publishing(1));
        put(mods.eln.oil.ElectricHeaterDescriptor.class, d -> MenuSpec.of(
            regulator(mods.eln.oil.ElectricHeaterElement.REGULATOR_SLOT, 8, 24, 1,
                new RegulatorType[]{RegulatorType.OnOff, RegulatorType.Analog}, MEDIUM, tr("Regulator slot"))).publishing(1));
        put(mods.eln.transparentnode.LargeRheostatDescriptor.class, d -> resistor());
    }

    /** 1.12 ResistorContainer (resistor and large rheostat): coal dust sets the resistance. */
    static MenuSpec resistor() {
        return MenuSpec.of(filter(mods.eln.sixnode.resistor.ResistorContainer.coreId, 132, 8, 64, ore("dustCoal"), MEDIUM,
            tr("Coal dust slot"), tr("(Sets resistance)"))).panel(176, 112, 8, 30);
    }
}
