package mods.eln.introspect;

/**
 * The units a report's numbers are in. Values are SI as EA computes them; temperatures are degrees Celsius (as EA shows
 * them), K is for temperature differences. The JSON name is the constant's name.
 */
public enum Unit {
    V("V"), A("A"), W("W"), J("J"), OHM("Ω"), C("°C"), K("K"), M("m"), M_PER_S("m/s"), KG("kg"), MB("mB"),
    RPM("rpm"), TICK("t"), S("s"), RATIO("");

    /** What a person reads after the number. */
    public final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }
}
