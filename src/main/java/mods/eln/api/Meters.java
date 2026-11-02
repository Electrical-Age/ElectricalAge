package mods.eln.api;

import mods.eln.misc.Utils;

/**
 * Readings formatted as EA's own meters, Jade lines and tooltips show them, for other mods' GUIs and tooltips: three
 * significant digits with an SI prefix (µ to M) and the unit right after, no padding: "230V", "1.50kW", "-3.20mA",
 * "0V" under a microunit. The number is EA's (the multimeter's), in the default locale as EA's own text is.
 */
public final class Meters {
    private Meters() {
    }

    /** "230V". */
    public static String volts(double volts) {
        return Utils.plotValue(volts) + "V";
    }

    /** "1.50A". */
    public static String amps(double amps) {
        return Utils.plotValue(amps) + "A";
    }

    /** "1.50kW". */
    public static String watts(double watts) {
        return Utils.plotValue(watts) + "W";
    }

    /** "12.0kJ". */
    public static String joules(double joules) {
        return Utils.plotValue(joules) + "J";
    }

    /** "10.0Ω". */
    public static String ohms(double ohms) {
        return Utils.plotValue(ohms) + "Ω";
    }

    /** A temperature, or a difference of temperatures, in kelvin: "30.0K". */
    public static String kelvin(double kelvin) {
        return Utils.plotValue(kelvin) + "K";
    }

    /** A temperature in degrees Celsius (as MachineView.celsius()): "85.0°C". */
    public static String celsius(double celsius) {
        return Utils.plotValue(celsius) + "°C";
    }
}
