package mods.eln.simplenode.energyconverter;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

//public class EnergyConverterElnToOtherFireWallIc2 {
//
//    /**
//     * Forward for the base TileEntity's updateEntity(), used for creating the energy net link.
//     * Either updateEntity or onLoaded have to be used.
//     */
//    public static void updateEntity(EnergyConverterElnToOtherEntity e) {
//        if (!e.addedToEnet) onLoaded(e);
//    }
//
//    /**
//     * Notification that the base TileEntity finished loading, for advanced uses.
//     * Either updateEntity or onLoaded have to be used.
//     */
//    public static void onLoaded(EnergyConverterElnToOtherEntity e) {
//        if (!e.addedToEnet &&
//            !FMLCommonHandler.instance().getEffectiveSide().isClient() &&
//            Info.isIc2Available()) {
//
//            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(e));
//
//            e.addedToEnet = true;
//        }
//    }
//
//    /**
//     * Forward for the base TileEntity's invalidate(), used for destroying the energy net link.
//     * Both invalidate and onChunkUnload have to be used.
//     */
//    public static void invalidate(EnergyConverterElnToOtherEntity e) {
//        e.onChunkUnload();
//    }
//
//    /**
//     * Forward for the base TileEntity's onChunkUnload(), used for destroying the energy net link.
//     * Both invalidate and onChunkUnload have to be used.
//     */
//    public static void onChunkUnload(EnergyConverterElnToOtherEntity e) {
//        if (e.addedToEnet && Info.isIc2Available()) {
//            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(e));
//
//            e.addedToEnet = false;
//        }
//    }
//}
