package mods.eln.cad.models;

import java.util.List;
import java.util.function.Supplier;

/** The catalog's entries: each model class's models. */
final class Models {
    private Models() {
    }

    static final List<Supplier<List<NodeModel>>> NODES = List.of(Boiler::models, CompactTurbine::models, Condenser::models, ElectricHeater::models,
        InlinePump::models, OilPump::models, SubmersiblePump::models, Tank::models, TarSandSeparator::models, ShaftGas::models, ShaftMotor::models,
        ShaftParts::models, ShaftTurbines::models, PlantCondenser::models, HeatExchanger::models, SteamOven::models, SteamTurbine::models,
        Smelter::models, ElectricalAnvil::models, CapacitorBank::models, Column::models, HandDrill::models, SignalProcessor::models, FieldLogger::models);
    static final List<Supplier<List<BlockModel>>> BLOCKS = List.of(Converter::models, Headlamp::models);
}
