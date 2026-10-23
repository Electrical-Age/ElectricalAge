package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.node.DescriptorRegistry;
import mods.eln.ore.OreDescriptor;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;

/**
 * Device batch Wp12c (26.x port, split from 1.12 Wp12Content): the ores (1.12 ids = eln:ore metadata: 1 copper,
 * 4 lead, 5 tungsten, 6 cinnabar). World generation is data (data/eln/worldgen + a NeoForge biome modifier, made by
 * script/gen_ore_data.py): with the 1.12 defaults (also production's eln.cfg) only tungsten generates; copper and lead
 * are off, and 1.12 forced cinnabar off in code. Ore dictionary names become item tags in P5.
 */
public class Wp12cContent implements ElnContent {
    /** The ores by 1.12 id (registered as blocks + block items by ElnRegistry). */
    public static final DescriptorRegistry<OreDescriptor> ores = new DescriptorRegistry<>(OreDescriptor.class);

    /** 1.12 also had Copper Ore (id 1); dropped, vanilla has copper (Baughn, 2026-10-11). */
    public static OreDescriptor oreLead;

    @Override
    public void preInit() {
        registerOre();
    }

    public static void registerOre() {
        int id;
        String name;

        {
            id = 4;
            name = TR_NAME(Type.NONE, "Lead Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                8 * (genLead ? 1 : 0), 3, 9, 0, 24 // spawnRate, spawnSizeMin, spawnSizeMax, spawnHeightMin, spawnHeightMax
            );
            oreLead = desc;
            ores.addDescriptor(id, desc);
            mods.eln.misc.OreDictTags.addToOre("oreLead", desc);
        }

        {
            // 26.x: lead lives in the middle of the deepslate range (Baughn 2026-10-20: y < 24 was 1.7.10's world), so it
            // needs a deepslate ore too; world generation is data (script/gen_ore_data.py)
            id = 7;
            name = TR_NAME(Type.NONE, "Deepslate Lead Ore");
            OreDescriptor desc = new OreDescriptor(name, id, 0, 3, 9, -64, 0);
            ores.addDescriptor(id, desc);
            mods.eln.misc.OreDictTags.addToOre("oreLead", desc);
        }

        {
            id = 5;
            name = TR_NAME(Type.NONE, "Tungsten Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                6 * (genTungsten ? 1 : 0), 3, 9, 0, 32
            );
            ores.addDescriptor(id, desc);
            mods.eln.misc.OreDictTags.addToOre(dictTungstenOre, desc);
        }

        {
            id = 6;
            name = TR_NAME(Type.NONE, "Cinnabar Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                3 * (genCinnabar ? 1 : 0), 3, 9, 0, 32
            );
            ores.addDescriptor(id, desc);
            mods.eln.misc.OreDictTags.addToOre("oreCinnabar", desc);
        }
    }
}
