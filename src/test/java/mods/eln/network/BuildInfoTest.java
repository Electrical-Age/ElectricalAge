package mods.eln.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The build commit is baked into the jar and versions the network channel (a mismatched pack is refused at login). */
class BuildInfoTest {
    @Test
    void commitIsBakedIn() {
        assertTrue(BuildInfo.COMMIT.matches("[0-9a-f]{7,40}(-dirty)?"), BuildInfo.COMMIT);
        assertEquals("1+" + BuildInfo.COMMIT, BuildInfo.channelVersion("1"));
    }
}
