package mods.eln.api.impl.lowlevel;

import com.mojang.serialization.JsonOps;
import mods.eln.api.lowlevel.PortKind;
import mods.eln.api.lowlevel.PortPosition;
import mods.eln.api.lowlevel.PortRef;
import mods.eln.api.lowlevel.PortStatus;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A port link's status from what it finds, the EA mask and position of a PortRef, and PortRef's codec. */
public class PortLinkTest {
    @Test
    void eachFailureHasItsOwnStatus() {
        assertEquals(PortStatus.NOT_LOADED, PortLinkImpl.classify(false, false, false, false, false));
        assertEquals(PortStatus.NO_NODE, PortLinkImpl.classify(false, true, false, false, false));
        assertEquals(PortStatus.NOT_SIMULATED, PortLinkImpl.classify(true, true, false, false, false));
        assertEquals(PortStatus.NO_PORT, PortLinkImpl.classify(true, true, true, false, false));
        assertEquals(PortStatus.WRONG_KIND, PortLinkImpl.classify(true, true, true, false, true));
        assertEquals(PortStatus.CONNECTED, PortLinkImpl.classify(true, true, true, true, false));
    }

    @Test
    void kindsAndPositionsAreEasOwn() {
        assertEquals(NodeBase.maskElectricalPower, PortLinkImpl.mask(PortKind.POWER));
        assertEquals(NodeBase.maskElectricalGate, PortLinkImpl.mask(PortKind.SIGNAL));
        assertEquals(null, PortLinkImpl.lrdu(PortPosition.ANY));
        assertEquals(LRDU.Down, PortLinkImpl.lrdu(PortPosition.DOWN));
        assertEquals(LRDU.Left, PortLinkImpl.lrdu(PortPosition.LEFT));
    }

    @Test
    void aPortRefSavesUnderFixedKeysAndRefusesUnknownNames() {
        PortRef ref = new PortRef(new BlockPos(3, -60, 7), Direction.EAST, PortPosition.DOWN, PortKind.SIGNAL);
        Tag tag = PortRef.CODEC.encodeStart(NbtOps.INSTANCE, ref).getOrThrow();
        assertEquals(ref, PortRef.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow());
        String json = PortRef.CODEC.encodeStart(JsonOps.INSTANCE, ref).getOrThrow().toString();
        assertEquals("{\"pos\":[3,-60,7],\"side\":\"east\",\"position\":\"DOWN\",\"kind\":\"SIGNAL\"}", json);
        var bad = com.google.gson.JsonParser.parseString(json.replace("SIGNAL", "STEAM"));
        var r = PortRef.CODEC.parse(JsonOps.INSTANCE, bad);
        assertTrue(r.error().isPresent() && r.error().get().message().contains("no PortKind named STEAM"), r.toString());
        assertEquals(PortPosition.ANY, PortRef.of(BlockPos.ZERO, Direction.UP, PortKind.POWER).position());
    }
}
