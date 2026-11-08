package mods.eln.cad.models;

import java.util.List;
import java.util.function.Supplier;

/** The catalog's entries: each model class's models. */
final class Models {
    private Models() {
    }

    static final List<Supplier<List<NodeModel>>> NODES = List.of(Boiler::models, PoleGear::models, CompactTurbine::models, Condenser::models, ElectricHeater::models,
        InlinePump::models, OilPump::models, SubmersiblePump::models, Tank::models, TarSandSeparator::models, ShaftGas::models, ShaftMotor::models,
        ShaftParts::models, ShaftTurbines::models, PlantCondenser::models, HeatExchanger::models, SteamOven::models, SteamTurbine::models,
        Smelter::models, HeatFurnace::models, FuelHeater::models, ElectricalAnvil::models, CapacitorBank::models, Column::models, HandDrill::models, SignalProcessor::models, FieldLogger::models, TiltedTerminal::models,
        ServoParts::models, ComputerParts::models, WireDrawer::models, Macerator::models, Compressor::models, Magnetizer::models, PlateMachine::models, ResinCollector::models, CarbonBlackBurner::models, Desulfuriser::models, SteamCracker::models, Polymeriser::models);
    static final List<Supplier<List<BlockModel>>> BLOCKS = List.of(Converter::models, Headlamp::models, ServoParts::blocks);
}
