package mods.eln;

import cpw.mods.fml.common.Loader;

public class Other {
	public static boolean ocLoaded = false;
	public static boolean ccLoaded = false;
	public static boolean teLoaded = false;
	public static boolean buildcraftLoaded = false;

	public static void check(){
		ccLoaded = Loader.isModLoaded(modIdCc);
		teLoaded = Loader.isModLoaded(modIdTe);
		buildcraftLoaded = Loader.isModLoaded(modIdBuildcraft);
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
	
	public static double ElnToTeConversionRatio; 
	public static double ElnToBuildcraftConversionRatio; 
	
	
	public static final String modIdTe = "Eln";
	public static final String modIdCc = "ComputerCraft";
	public static final String modIdBuildcraft = "BuildCraft|Core";


}
