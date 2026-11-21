package mods.eln.misc;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** plotValue's SI prefixes: the µ range was scaled by 1e4 in 1.12 (0.5 mA read "5.00µ"); M above 1e6. */
public class PlotValueTest {
    @Test
    void prefixes() {
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        try {
            assertEquals("0", Utils.plotValue(5e-7));
            assertEquals("2.50µ", Utils.plotValue(2.5e-6));
            assertEquals("50.0µ", Utils.plotValue(5e-5));
            assertEquals("500µ", Utils.plotValue(5e-4));
            assertEquals("5.00m", Utils.plotValue(5e-3));
            assertEquals("1.00", Utils.plotValue(1));
            assertEquals("2.50k", Utils.plotValue(2500));
            assertEquals("250k", Utils.plotValue(250000));
            assertEquals("2.50M", Utils.plotValue(2.5e6));
            assertEquals("-500µ", Utils.plotValue(-5e-4));
        } finally {
            Locale.setDefault(old);
        }
    }

    /** Temperatures: the Tc scale is kelvin above 20 °C; the padded meter form is the bare one with its header. */
    @Test
    void celsiusFromTheTcScale() {
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        try {
            assertEquals("20.0°C", Utils.plotCelsius(0));
            assertEquals("85.0°C", Utils.plotCelsius(65));
            assertEquals("1.00k°C", Utils.plotCelsius(980));
            assertEquals("T: 85.0°C ", Utils.plotCelsius("T:", 65));
        } finally {
            Locale.setDefault(old);
        }
    }

    /** Percent: whole numbers, 1 is 100 %; the padded form adds the header and the meter's spacing. */
    @Test
    void wholePercent() {
        Locale old = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        try {
            assertEquals("50%", Utils.plotPercent(0.5));
            assertEquals("0%", Utils.plotPercent(0.004));
            assertEquals("100%", Utils.plotPercent(0.996));
            assertEquals("120%", Utils.plotPercent(1.2));
            assertEquals("Eff: 50%   ", Utils.plotPercent("Eff:", 0.5));
        } finally {
            Locale.setDefault(old);
        }
    }
}
