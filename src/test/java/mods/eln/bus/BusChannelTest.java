package mods.eln.bus;

import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The 16 channels are Minecraft's dyes in DyeColor's order; EA's brushes number them the other way round. */
class BusChannelTest {
    @Test
    void theTableIsDyeColors() {
        assertEquals(DyeColor.values().length, BusChannel.COUNT);
        for (int c = 0; c < BusChannel.COUNT; c++) {
            DyeColor d = DyeColor.byId(c);
            assertEquals(d.getName(), BusChannel.name(c), "channel " + c);
            assertEquals(d.getTextureDiffuseColor() & 0xFFFFFF, BusChannel.rgb(c), "channel " + c + "'s swatch");
            assertEquals(c, BusChannel.byName(d.getName()));
        }
        assertEquals("3 light_blue", BusChannel.label(3));
        assertEquals(BusChannel.UNUSED, BusChannel.byName("teal"));
        assertEquals("", BusChannel.label(BusChannel.UNUSED));
    }

    @Test
    void brushes() {
        // EA's brushes: 0 black, 1 red, 7 silver, 12 light blue, 15 white (Wp12aContent.registerBrush)
        assertEquals(15, BusChannel.fromBrush(0));
        assertEquals(14, BusChannel.fromBrush(1));
        assertEquals(8, BusChannel.fromBrush(7));
        assertEquals(3, BusChannel.fromBrush(12));
        assertEquals(0, BusChannel.fromBrush(15));
    }

    @Test
    void cycleAndOldValues() {
        assertEquals(0, BusChannel.cycle(BusChannel.UNUSED, false));
        assertEquals(BusChannel.UNUSED, BusChannel.cycle(15, false));
        assertEquals(15, BusChannel.cycle(BusChannel.UNUSED, true));
        assertEquals(BusChannel.UNUSED, BusChannel.cycle(0, true));
        assertEquals(BusChannel.UNUSED, BusChannel.orUnused(16));
        assertEquals(BusChannel.UNUSED, BusChannel.orUnused(-7));
        assertEquals(0, BusChannel.cycle(99, false), "a bad value counts as unused");
    }
}
