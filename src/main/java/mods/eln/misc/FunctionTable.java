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
