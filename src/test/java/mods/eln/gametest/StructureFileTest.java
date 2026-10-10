package mods.eln.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/** The generated GameTest structures parse as intended (script/gen_test_structures.py): a 3x3 stone floor. */
public class StructureFileTest {
    @Test
    public void floorStructureHasItsBlocks() throws Exception {
        try (InputStream in = StructureFileTest.class.getResourceAsStream("/data/eln/structure/floor_3x4x3.nbt")) {
            CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            StructureTemplate t = new StructureTemplate();
            t.load(BuiltInRegistries.BLOCK, tag);
            assertEquals("size x", 3, t.getSize().getX());
            System.out.println("eln structure test: tag keys " + tag.keySet() + ", blocks list " + tag.getListOrEmpty("blocks").size()
                + ", palette " + tag.getListOrEmpty("palette"));
            assertEquals("floor blocks", 9, tag.getListOrEmpty("blocks").size());
        }
    }
}
