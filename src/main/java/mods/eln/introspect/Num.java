package mods.eln.introspect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A number with its unit, and optionally the tolerance two halves may differ by (sync), its valid range and its
 * nominal value. Set the extras right after adding it: {@code s.num("position", p, Unit.M).tol(0.05)}.
 */
public final class Num implements Value {
    public final double v;
    public final Unit unit;
    private double tol = Double.NaN, min = Double.NaN, max = Double.NaN, nominal = Double.NaN;

    Num(double v, Unit unit) {
        if (unit == null) throw new IllegalArgumentException("a number needs a unit (Unit.RATIO for a plain number)");
        this.v = v;
        this.unit = unit;
    }

    /** How far the client's value may be from the server's (sync), in the same unit. */
    public Num tol(double tol) {
        if (!(tol >= 0)) throw new IllegalArgumentException("tolerance " + tol);
        this.tol = tol;
        return this;
    }

    public Num range(double min, double max) {
        if (!(min <= max)) throw new IllegalArgumentException("range " + min + ".." + max);
        this.min = min;
        this.max = max;
        return this;
    }

    public Num nominal(double nominal) {
        this.nominal = nominal;
        return this;
    }

    /** NaN when not set. */
    public double tol() {
        return tol;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double nominal() {
        return nominal;
    }

    public JsonElement json() {
        JsonObject o = new JsonObject();
        // JSON has no NaN or infinity: such a value goes out as a string, and validation reports it
        if (Double.isFinite(v)) o.addProperty("num", v);
        else o.addProperty("num", Double.toString(v));
        o.addProperty("unit", unit.name());
        if (!Double.isNaN(tol)) o.addProperty("tol", tol);
        if (!Double.isNaN(min)) o.addProperty("min", min);
        if (!Double.isNaN(max)) o.addProperty("max", max);
        if (!Double.isNaN(nominal)) o.addProperty("nominal", nominal);
        return o;
    }
}
