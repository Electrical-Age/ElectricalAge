package mods.eln.fluidcore;

import static mods.eln.fluidcore.Network.BAND;
import static mods.eln.fluidcore.Network.G;
import static mods.eln.fluidcore.Network.smoothStep;
import static mods.eln.fluidcore.Network.stepSlope;

/** A node of water and air (WaterAir): one liquid, a gas of vapour and air; its port window and head. */
final class WaterAirFluid implements NodeFluid {
    final double V, H;
    final boolean fixed;
    double mw, ma, U;
    /** The last solved state (outputs and slopes), and its temperature (the next solve's hint). */
    WaterAir.Split split;
    double[] out;
    double[][] slopes;
    double[] tSlope;
    double tHint = 300;

    WaterAirFluid(double V, double H, boolean fixed) {
        this.V = V;
        this.H = H;
        this.fixed = fixed;
    }

    public int species() {
        return 2;
    }

    public void update(double[] m, double U) {
        mw = m[0];
        ma = m[1];
        this.U = U;
        split = split == null ? WaterAir.solve(mw, ma, U, V, tHint) : WaterAir.solve(mw, ma, U, V, split);
        tHint = split.T();
        out = WaterAir.outputs(split, V);
        slopes = null;   // (taken when the Jacobian needs them: slopes())
        tSlope = null;
    }

    public double[] tSlope() {
        if (tSlope == null) tSlope = fixed ? new double[3] : WaterAir.derivatives(split, mw, ma, U, V)[0];
        return tSlope;
    }

    double[][] slopes() {
        if (slopes == null) slopes = fixed ? new double[WaterAir.OUTPUTS][3] : WaterAir.outputSlopes(split, mw, ma, U, V);
        return slopes;
    }

    public double P() {
        return out[WaterAir.P];
    }

    public double T() {
        return split.T();
    }

    public double level() {
        return out[WaterAir.LEVEL];
    }

    double liquid() {
        return out[WaterAir.ML];
    }

    /**
     * The liquid's head over a port at share f, for a level L (shares of H): rho g H (softplus(L - f) - softplus(-f)),
     * smooth at the surface and nothing in an empty node (without the floor's term, an empty node's floor port saw
     * w ln 2 of phantom liquid: 21 Pa in a 1 m node; and a layer's composition would matter while it is empty).
     */
    double head(double L, double f) {
        double w = 0.25 * BAND;
        return G * H / WaterAir.V0 * (WaterAir.softplus(L - f, w) - WaterAir.softplus(-f, w));
    }

    double headSlope(double L, double f) {
        double w = 0.25 * BAND, x = (L - f) / w;
        double sig = x > 30 ? 1 : x < -30 ? Math.exp(x) : 1 / (1 + Math.exp(-x));
        return G * H / WaterAir.V0 * sig;
    }

    public double portP(double f) {
        return P() + head(level(), f);
    }

    public double[] portPSlope(double f) {
        double hs = headSlope(level(), f);
        double[] d = new double[3];
        for (int j = 0; j < 3; j++) d[j] = slopes()[WaterAir.P][j] + hs * slopes()[WaterAir.LEVEL][j];
        return d;
    }

    /**
     * The window's argument: -1 where the liquid's surface is at the port (or lower), 1 a band above it. A port draws
     * liquid only once the surface is over it, so a dry node's floor port draws no water it hasn't got (centred on the
     * port, it drew half "liquid" from a dry node and drove its water negative); a port at the top counts as a band
     * lower, so a full node can push liquid out of it.
     */
    double windowArg(double f) {
        return 2 * (level() - Math.min(f, 1 - BAND)) / BAND - 1;
    }

    public double[] draw(double f) {
        double xl = smoothStep(windowArg(f));
        double yv = out[WaterAir.YV], ya = out[WaterAir.YA];
        return new double[]{xl + (1 - xl) * yv, (1 - xl) * ya, xl * out[WaterAir.HL] + (1 - xl) * out[WaterAir.HG]};
    }

    public double[][] drawSlope(double f) {
        double x = windowArg(f);
        double xl = smoothStep(x), dxl = 2 * stepSlope(x) / BAND;
        double yv = out[WaterAir.YV], ya = out[WaterAir.YA], hl = out[WaterAir.HL], hg = out[WaterAir.HG];
        double[][] d = new double[3][3];
        for (int j = 0; j < 3; j++) {
            double dL = dxl * slopes()[WaterAir.LEVEL][j], dy = slopes()[WaterAir.YV][j], da = slopes()[WaterAir.YA][j];
            d[0][j] = dL * (1 - yv) + (1 - xl) * dy;
            d[1][j] = -dL * ya + (1 - xl) * da;
            d[2][j] = dL * (hl - hg) + xl * slopes()[WaterAir.HL][j] + (1 - xl) * slopes()[WaterAir.HG][j];
        }
        return d;
    }

    public double liquidWindow(double f) {
        return smoothStep(windowArg(f));
    }

    public double rhoLiquid(double f) {
        return 1 / split.v();
    }

    public double rhoGas() {
        return (out[WaterAir.MV] + out[WaterAir.MA]) / split.vg();
    }

    public double[] liquidDraw(double f) {
        return new double[]{1, 0, out[WaterAir.HL]};
    }

    /** (Its real contents' share: the cushion stays.) */
    public double[] gasDraw() {
        return new double[]{out[WaterAir.YV], out[WaterAir.YA], out[WaterAir.HG]};
    }

    public double liquidMass(int i) {
        return i == 0 ? out[WaterAir.ML] : 0;
    }

    public double vapourMass(int i) {
        return i == 0 ? out[WaterAir.MV] : out[WaterAir.MA];
    }

    public double partialP(int i) {
        return i == 0 ? split.pv() : Math.max(0, out[WaterAir.P] - split.pv());
    }

    public double energyAt(double[] m, double T) {
        return WaterAir.energyAt(m[0], m[1], V, T);
    }

    public Object saveState() {
        return new Object[]{split, out, slopes, tSlope};
    }

    public void restoreState(Object s) {
        Object[] a = (Object[]) s;
        split = (WaterAir.Split) a[0];
        out = (double[]) a[1];
        slopes = (double[][]) a[2];
        tSlope = (double[]) a[3];
        tHint = split.T();
    }
}
