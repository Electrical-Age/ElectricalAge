package mods.eln.api;

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
        return number(volts) + "V";
    }

    /** "1.50A". */
    public static String amps(double amps) {
        return number(amps) + "A";
    }

    /** "1.50kW". */
    public static String watts(double watts) {
        return number(watts) + "W";
    }

    /** "12.0kJ". */
    public static String joules(double joules) {
        return number(joules) + "J";
    }

    /** "10.0Ω". */
    public static String ohms(double ohms) {
        return number(ohms) + "Ω";
    }

    /** A temperature, or a difference of temperatures, in kelvin: "30.0K". */
    public static String kelvin(double kelvin) {
        return number(kelvin) + "K";
    }

    /**
     * A temperature in degrees Celsius (as MachineView.celsius()): "85.0°C". Under a degree it has two decimals and no
     * prefix ("0.12°C", not "122m°C": nobody reads milli-degrees Celsius), and "0°C" under 0.005.
     */
    public static String celsius(double celsius) {
        double abs = Math.abs(celsius);
        if (abs < 0.005) return "0°C";
        return (abs < 0.999 ? String.format("%1.2f", celsius) : number(celsius)) + "°C";
    }

    /**
     * The number alone, as EA's meters show it, for a unit with no method here: three significant digits with an SI
     * prefix (µ to M), "0" under a microunit: "1.50k", "-3.20m". EA's own text uses this.
     */
    public static String number(double value) {
        double abs = Math.abs(value);
        if (abs < 0.000001) return "0";
        if (abs < 0.00000999) return String.format("%1.2fµ", value * 1e6);
        if (abs < 0.0000999) return String.format("%2.1fµ", value * 1e6);
        if (abs < 0.000999) return String.format("%3.0fµ", value * 1e6);
        if (abs < 0.00999) return String.format("%1.2fm", value * 1000);
        if (abs < 0.0999) return String.format("%2.1fm", value * 1000);
        if (abs < 0.999) return String.format("%3.0fm", value * 1000);
        if (abs < 9.99) return String.format("%1.2f", value);
        if (abs < 99.9) return String.format("%2.1f", value);
        if (abs < 999) return String.format("%3.0f", value);
        if (abs < 9999) return String.format("%1.2fk", value / 1000.0);
        if (abs < 99999) return String.format("%2.1fk", value / 1000.0);
        if (abs < 999999) return String.format("%3.0fk", value / 1000.0);
        if (abs < 9999999) return String.format("%1.2fM", value / 1e6);
        if (abs < 99999999) return String.format("%2.1fM", value / 1e6);
        return String.format("%3.0fM", value / 1e6);
    }
}
