package mods.eln.sixnode.electricaldatalogger;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogAxesTest {
    @Test
    void channelsOnOneScaleShareAnAxis() {
        var volts = new LogChannel(0, 50, DataLogs.voltageType);
        var amps = new LogChannel(0, 10, DataLogs.currentType);
        var axes = LogAxes.layout(List.of(volts, new LogChannel(0, 50, DataLogs.voltageType), amps));
        assertEquals(2, axes.size());
        assertEquals(LogAxes.Side.LEFT, axes.get(0).side());
        assertEquals(List.of(0, 1), axes.get(0).channels());
        assertEquals(LogAxes.Side.RIGHT, axes.get(1).side());
        assertEquals(0, axes.get(1).column());
        assertEquals(List.of(2), axes.get(1).channels());
    }

    @Test
    void aThirdScaleTakesASecondColumnOnTheRight() {
        var axes = LogAxes.layout(List.of(new LogChannel(0, 50, DataLogs.voltageType), new LogChannel(0, 10, DataLogs.currentType),
            new LogChannel(0, 100, DataLogs.percentType), new LogChannel(0, 1000, DataLogs.powerType)));
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
}
