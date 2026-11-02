package mods.eln.api.docs;

import mods.eln.api.ElnApi;
import mods.eln.api.Machine;
import mods.eln.api.MachineSpec;
import mods.eln.api.MachineView;
import mods.eln.api.Meters;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** docs/api.md's consumer: a mill on a 200 V line. */
public final class Mill {
    private Mill() {
    }

    // doc:begin spec
    public static final MachineSpec SPEC = MachineSpec.consumer(Tier.V200)
        .ratedWatts(2000)                        // the most it draws at 200 V, idle included
        .idleWatts(20)                           // drawn whenever it is powered
        .power(RelativeFace.BACK, RelativeFace.DOWN)
        .build();                                // checks every setting, lists every problem at once
    // doc:end spec

    // doc:begin register
    /** In the mod's constructor: the mill's block entity type becomes a machine at common setup. */
    public static void register(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> type) {
        modBus.addListener(FMLCommonSetupEvent.class, e -> ElnApi.machines().register(type.get(), be -> SPEC));
    }
    // doc:end register

    // doc:begin work
    /** The mill's work, one call per server tick: an item takes 3 kJ, paid tick by tick. */
    public static final class Work {
        public static final double JOULES_PER_ITEM = 3000;
        private double paid;

        /** True when an item is done. */
        public boolean tick(Machine m, boolean hasInput) {
            if (!hasInput) return false;                  // no ask: the mill draws its idle watts only
            paid += m.takeJoules(m.ratedJoulesPerTick()); // ask a tick's rated energy, get what the line gave
            if (paid < JOULES_PER_ITEM) return false;
            paid -= JOULES_PER_ITEM;
            return true;
        }
    }
    // doc:end work

    // doc:begin tooltip
    /** Tooltip lines for any machine's view (MachineView: read-only, also on the client). */
    public static List<String> tooltip(MachineView v) {
        String line = Meters.volts(v.volts()) + "  " + Meters.watts(v.watts());
        String state = v.state().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return v.spec().thermal().isPresent() ? List.of(line, state, Meters.celsius(v.celsius())) : List.of(line, state);
    }
    // doc:end tooltip
}
