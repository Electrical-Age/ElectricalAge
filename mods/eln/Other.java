package mods.eln;

import cpw.mods.fml.common.Loader;

public class Other {
	public static boolean ic2Loaded = false;
	public static boolean ocLoaded = false;
	public static boolean ccLoaded = false;
	public static boolean teLoaded = false;
	public static boolean buildcraftLoaded = false;

	public static void check(){
		ic2Loaded = Loader.isModLoaded(modIdIc2);
		ocLoaded = Loader.isModLoaded(modIdOc);
		ccLoaded = Loader.isModLoaded(modIdCc);
		teLoaded = Loader.isModLoaded(modIdTe);
		buildcraftLoaded = Loader.isModLoaded(modIdBuildcraft);
	}
	public static double getElnToIc2ConversionRatio() {
		// TODO Auto-generated method stub
		//return 1.0 / 3;
		return ElnToIc2ConversionRatio;
	}
	public static double getElnToOcConversionRatio() {
		// TODO Auto-generated method stub
		//return getElnToIc2ConversionRatio()/2.5;
		return ElnToOcConversionRatio;
	}
	public static double getElnToTeConversionRatio() {
		// TODO Auto-generated method stub
		//return getElnToIc2ConversionRatio()*4;
		return ElnToTeConversionRatio;
	}
	public static double getElnToBuildcraftConversionRatio() {
		// TODO Auto-generated method stub
		return ElnToBuildcraftConversionRatio;
		//return getElnToIc2ConversionRatio()/5*2;
	}	
	
	public static double ElnToIc2ConversionRatio; 
	public static double ElnToOcConversionRatio; 
	public static double ElnToTeConversionRatio; 
	public static double ElnToBuildcraftConversionRatio; 
	
	
	public static final String modIdIc2 = "IC2";
	public static final String modIdOc ="OpenComputers";
	public static final String modIdTe = "Eln";
	//public static final String modIdTe = "Eln";
	public static final String modIdCc = "ComputerCraft";
	public static final String modIdBuildcraft = "BuildCraft|Core";


}
