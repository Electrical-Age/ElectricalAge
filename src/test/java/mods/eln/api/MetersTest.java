package mods.eln.api;

import mods.eln.misc.Utils;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Meters reads as EA's own meters (Utils.plot*), without their padding. */
public class MetersTest {
    @Test
    void readingsMatchEasMeters() {
        Locale was = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        try {
            assertEquals("230V", Meters.volts(230));
            assertEquals("1.50kW", Meters.watts(1500));
            assertEquals("-3.20mA", Meters.amps(-0.0032));
            assertEquals("0V", Meters.volts(1e-9));
            assertEquals("12.0kJ", Meters.joules(12000));
            assertEquals("10.0Ω", Meters.ohms(10));
            assertEquals("30.0K", Meters.kelvin(30));
            assertEquals("85.0°C", Meters.celsius(85));
            for (double v : new double[]{0, 2e-6, 4.5e-4, 0.0123, 0.5, 7.77, 42, 230, 3200, 51200, 2.5e5, 1.2e6, 3e7, 9e8, -800}) {
                assertEquals(Utils.plotVolt(v).trim(), Meters.volts(v));
                assertEquals(Utils.plotAmpere(v).trim(), Meters.amps(v));
                assertEquals(Utils.plotPower(v).trim(), Meters.watts(v));
                assertEquals(Utils.plotEnergy(v).trim(), Meters.joules(v));
                assertEquals(Utils.plotOhm(v).trim(), Meters.ohms(v));
                // EA's thermal loads hold degrees over the 20 C reference; the API's celsius() is absolute
                assertEquals(Utils.plotCelsius("", v - 20).trim(), Meters.celsius(v));
            }
        } finally {
            Locale.setDefault(was);
        }
    }
}
