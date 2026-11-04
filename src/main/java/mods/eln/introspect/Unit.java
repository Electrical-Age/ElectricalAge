package mods.eln.introspect;

/**
 * The units a report's numbers are in. Values are SI as EA computes them; temperatures are degrees Celsius (as EA shows
 * them), K is for temperature differences. The JSON name is the constant's name.
 */
public enum Unit {
    V("V"), A("A"), W("W"), J("J"), OHM("Ω"), C("°C"), K("K"), M("m"), M_PER_S("m/s"), M_PER_S2("m/s\u00B2"), KG("kg"), MB("mB"),
    RPM("rpm"), TICK("t"), S("s"), J_PER_K("J/K"), W_PER_K("W/K"), RATIO("");

    /** What a person reads after the number. */
    public final String symbol;

    Unit(String symbol) {
        this.symbol = symbol;
    }
}
