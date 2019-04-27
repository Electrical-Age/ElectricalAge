package mods.eln.integration.waila

import com.google.common.cache.CacheLoader
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import net.minecraftforge.fml.common.Optional

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class SixNodeWailaProvider : IWailaDataProvider {
    private fun getSixData(accessor: IWailaDataAccessor): SixNodeWailaData? {
        val coord = Coordinate(accessor.position.x, accessor.position.y, accessor.position.z,
            accessor.world)
        val side = Direction.fromFacing(accessor.side)
        var sixData: SixNodeWailaData? = null
        try {
            sixData = WailaCache.sixNodes.get(SixNodeCoordinate(coord, side))
        } catch(e: CacheLoader.InvalidCacheLoadException) {
        }

        return sixData
    }

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?): MutableList<String> {
        getSixData(accessor)?.data?.forEach {
            currenttip.add("${it.key}: ${TextFormatting.WHITE}${it.value}")
        }

        return currenttip
    }

    override fun getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler?): ItemStack
        = getSixData(accessor)?.itemStack ?: ItemStack.EMPTY

    override fun getWailaTail(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String> = currenttip

    override fun getNBTData(player: EntityPlayerMP?, te: TileEntity?, tag: NBTTagCompound?, world: World?, pos: BlockPos?): NBTTagCompound {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWailaHead(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?): MutableList<String> = if (itemStack != null) {
        mutableListOf("${TextFormatting.WHITE}${itemStack.displayName}")
    } else {
        currenttip
    }
}
