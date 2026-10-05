package mods.eln;

import net.minecraftforge.fml.common.Loader;

public class Other {

    public static boolean ic2Loaded = false;
    public static boolean ocLoaded = false;
    public static boolean ccLoaded = false;
    public static boolean teLoaded = false;

    public static double ElnToIc2ConversionRatio;
    public static double ElnToOcConversionRatio;
    public static double ElnToTeConversionRatio;

    public static final String modIdIc2 = "IC2";
    public static final String modIdOc = "OpenComputers";
    public static final String modIdTe = "Eln";
    public static final String modIdCc = "ComputerCraft";

    public static void check() {
        ic2Loaded = Loader.isModLoaded(modIdIc2);
        ocLoaded = Loader.isModLoaded(modIdOc);
        ccLoaded = Loader.isModLoaded(modIdCc);
        teLoaded = Loader.isModLoaded(modIdTe);
    }

    public static double getElnToIc2ConversionRatio() {
        return ElnToIc2ConversionRatio;
    }

    public static double getElnToOcConversionRatio() {
        return ElnToOcConversionRatio;
    }

    public static double getElnToTeConversionRatio() {
        return ElnToTeConversionRatio;
    }
}
