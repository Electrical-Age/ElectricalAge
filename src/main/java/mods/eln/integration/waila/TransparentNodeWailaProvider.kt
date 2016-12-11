package mods.eln.integration.waila

import com.google.common.cache.CacheLoader
import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.SpecialChars
import mods.eln.misc.Coordonate
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class TransparentNodeWailaProvider : IWailaDataProvider{
    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>,
                              accessor: IWailaDataAccessor, config: IWailaConfigHandler?): MutableList<String>? {
        val coord = Coordonate(accessor.position.blockX, accessor.position.blockY, accessor.position.blockZ,
                accessor.world)
        try {
            WailaCache.nodes.get(coord)?.forEach { currenttip.add("${it.key}: ${SpecialChars.WHITE}${it.value}") }
        } catch(e: CacheLoader.InvalidCacheLoadException){
            //This is probably just it complaining about the cache returning null. Should be safe to ignore.
        }

        return currenttip
    }

    override fun getWailaStack(accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): ItemStack? {
        return null
    }

    override fun getWailaTail(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): MutableList<String>? {
        return currenttip
    }

    override fun getNBTData(player: EntityPlayerMP?, te: TileEntity?, tag: NBTTagCompound?, world: World?, x: Int, y: Int, z: Int): NBTTagCompound? {
        return null
    }

    override fun getWailaHead(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): MutableList<String>? {
        return currenttip
    }


}
