package mods.eln.misc.series;

public class SerieEE implements ISerie{
	
	public SerieEE(double startExp,double[]eValue) {
		this.startExp = startExp;
		this.eValue = eValue;
	}
	
	double startExp = 1;

	double[] eValue;

	public int getSeries() {
		return eValue.length;
	}
	@Override
	public double getValue(int count) {
		int rot = count / getSeries();
		count -= rot * getSeries();
		return Math.pow(10, startExp) * Math.pow(10, rot) * eValue[count];
	}

	
	
	public static SerieEE newE12(double startExp){
		return new SerieEE(startExp,  new double[] { 1, 1.2, 1.5, 1.8, 2.2, 2.7, 3.3, 3.9, 4.7, 5.6, 6.8, 8.2 });
	}
	public static SerieEE newE6(double startExp){
		return new SerieEE(startExp,  new double[] { 1, 1.5, 2.2, 3.3, 4.7, 6.8 });
	}
}
