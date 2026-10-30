package mods.eln.sixnode.electricaldatalogger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogAxesTest {
    @Test
    void channelsOnOneScaleShareAnAxis() {
        var volts = new LogChannel(0, 50, "V");
        var amps = new LogChannel(0, 10, "A");
        var axes = LogAxes.layout(List.of(volts, new LogChannel(0, 50, "V"), amps));
        assertEquals(2, axes.size());
        assertEquals(LogAxes.Side.LEFT, axes.get(0).side());
        assertEquals(List.of(0, 1), axes.get(0).channels());
        assertEquals(LogAxes.Side.RIGHT, axes.get(1).side());
        assertEquals(0, axes.get(1).column());
        assertEquals(List.of(2), axes.get(1).channels());
    }

    @Test
    void aThirdScaleTakesASecondColumnOnTheRight() {
        var axes = LogAxes.layout(List.of(new LogChannel(0, 50, "V"), new LogChannel(0, 10, "A"),
            new LogChannel(0, 100, "%"), new LogChannel(0, 1000, "W")));
        assertEquals(4, axes.size());
        assertEquals(LogAxes.Side.RIGHT, axes.get(2).side());
        assertEquals(1, axes.get(2).column());
        assertEquals(2, axes.get(3).column());
    }

    @Test
    void ticksAreNice() {
        assertArrayEquals(new double[]{0, 10, 20, 30, 40, 50}, LogAxes.ticks(0, 50, 6), 1e-12);
        assertArrayEquals(new double[]{0, 20, 40}, LogAxes.ticks(0, 50, 4), 1e-12);
        assertArrayEquals(new double[]{-1, -0.5, 0, 0.5, 1}, LogAxes.ticks(1, -1, 5), 1e-12);
        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, LogAxes.ticks(0.05, 0.31, 5), 1e-12);
        assertArrayEquals(new double[]{3, 3}, LogAxes.ticks(3, 3, 5), 1e-12);
    }

    @Test
    void unitsAreFreeTextMadeSafe() {
        assertEquals("rpm", LogChannel.sanitize("  rpm "));
        assertEquals("acbc", LogChannel.sanitize("a\u00A7cb\nc"));
        assertEquals("0123456789", LogChannel.sanitize("0123456789XYZ"));
        assertEquals("\u00B0C", LogChannel.oldUnit(DataLogs.celsiusType));
        assertEquals("12.5 rpm", DataLogs.format(12.5, "rpm"));
        assertEquals("50%", DataLogs.format(50, "%"));
        assertEquals("1.20kW", DataLogs.format(1200, "W"));
    }
}
