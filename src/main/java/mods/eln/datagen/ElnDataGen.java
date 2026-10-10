package mods.eln.datagen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * NeoForge datagen (P5): `gradlew runClientData` writes src/generated/resources (committed, a resources dir).
 * Datagen owns lang, item tags, recipes and their advancements; script/gen_ore_data.py owns the ore loot tables,
 * worldgen and the mineable block tag (one owner per file).
 */
public final class ElnDataGen {
    private ElnDataGen() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GatherDataEvent.Client.class, ElnDataGen::gather);
    }

    static void gather(GatherDataEvent.Client event) {
        event.createProvider(ElnLang::new);
    }
}
