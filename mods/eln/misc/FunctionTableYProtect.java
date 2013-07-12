package mods.eln.misc;

public class FunctionTableYProtect extends FunctionTable {

	double yMin, yMax;
	public FunctionTableYProtect(double[] point, double xMax,double yMin,double yMax) {
		super(point, xMax);
		this.yMax = yMax;
		this.yMin = yMin;
	}


	@Override
	public double getValue(double x) {
		// TODO Auto-generated method stub
		double value = super.getValue(x);
		if(value > yMax) return yMax;
		if(value < yMin) return yMin;
		return value;
	}
	public FunctionTable duplicate(double xFactor,double yFactor)
	{
		double [] pointCpy = new double[point.length];
		for(int idx = 0;idx<point.length;idx++)
		{
			pointCpy[idx] = point[idx] * yFactor;
		}
		FunctionTable  other = new FunctionTableYProtect(pointCpy, xMax*xFactor,yMin*yFactor,yMax*yFactor);
		return other;
	}
}
