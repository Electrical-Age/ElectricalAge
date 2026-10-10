package mods.eln.misc;

public class LinearFunction implements IFunction {

    private final float x0;
    private final float y0;
    private final float x1;
    private final float y1;

    public LinearFunction(float x0, float y0, float x1, float y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }

    @Override
    public double getValue(double x) {
        return (x - x0) / (x1 - x0) * (y1 - y0) + y0;
    }
}