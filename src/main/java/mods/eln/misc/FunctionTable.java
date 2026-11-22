package mods.eln.misc;

public class FunctionTable implements IFunction {

    double[] point;
    double xMax, xMaxInv;
    double xDelta;

    public FunctionTable(double[] point, double xMax) {
        this.point = point;
        this.xMax = xMax;
        this.xMaxInv = 1.0 / xMax;

        this.xDelta = 1.0 / (point.length - 1) * xMax;
    }

    /**
     * A table of n points over 0..xMax, linear between the knots ({x, y} pairs, x rising, the first at 0 and the
     * last at xMax).
     */
    public static FunctionTable fromKnots(double[][] knots, double xMax, int n) {
        double[] p = new double[n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            double x = xMax * i / (n - 1);
            while (k + 2 < knots.length && x > knots[k + 1][0]) k++;
            double[] a = knots[k], b = knots[k + 1];
            double t = b[0] == a[0] ? 1 : Math.max(0, Math.min(1, (x - a[0]) / (b[0] - a[0])));
            p[i] = a[1] + (b[1] - a[1]) * t;
        }
        return new FunctionTable(p, xMax);
    }

    public double getValue(double x) {
        x *= xMaxInv;
        if (x < 0f) return point[0] + (point[1] - point[0]) * (point.length - 1) * x;

        if (x >= 1.0f)
            return point[point.length - 1] + (point[point.length - 1] - point[point.length - 2]) * (point.length - 1) * (x - 1.0);

        x *= point.length - 1;
        int idx = (int) x;
        x -= idx;
        return point[idx + 1] * x + point[idx] * (1.0f - x);
    }

	/*public double getValue(double x) {
        double a = getValueLin(-xDelta);
		double b = getValueLin(xDelta);
		double firFactorA = 0.5, firFactorB = (1 - firFactorA) / 2;
		return getValueLin(x) * firFactorA + getValueLin(x - xDelta) * firFactorB + getValueLin(x + xDelta) * firFactorB;
	}
	*/

    public FunctionTable duplicate(double xFactor, double yFactor) {
        double[] pointCpy = new double[point.length];
        for (int idx = 0; idx < point.length; idx++) {
            pointCpy[idx] = point[idx] * yFactor;
        }
        FunctionTable other = new FunctionTable(pointCpy, xMax * xFactor);
        return other;
    }
}
