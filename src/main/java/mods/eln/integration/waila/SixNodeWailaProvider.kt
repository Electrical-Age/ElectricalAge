package mods.eln.integration.waila

import com.google.common.cache.CacheLoader
import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.SpecialChars
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.packets.SixNodeWailaRequestPacket
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class SixNodeWailaProvider: IWailaDataProvider {
    companion object {
        var updateTime = Minecraft.getSystemTime()
    }

    private fun getSixData(accessor: IWailaDataAccessor): Map<String, String>? {
        val coord = Coordonate(accessor.position.blockX, accessor.position.blockY, accessor.position.blockZ,
                accessor.world)
        val side = Direction.from(accessor.side)
        var sixData: Map<String, String>? = null

        if (Minecraft.getSystemTime() - SixNodeWailaProvider.updateTime > 2000) {
            Eln.elnNetwork.sendToServer(SixNodeWailaRequestPacket(coord, side))
            SixNodeWailaProvider.updateTime = Minecraft.getSystemTime()
        }
        try {
            sixData = WailaCache.sixNodes.get(SixNodeCoordonate(coord, side))
        } catch(e: CacheLoader.InvalidCacheLoadException) {}

        return sixData
    }

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?): MutableList<String> {
        getSixData(accessor)?.forEach {
            currenttip.add(it.key + ": " + SpecialChars.WHITE + it.value)
        }

        return currenttip
    }

    override fun getWailaStack(accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): ItemStack? = null

    override fun getWailaTail(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String> = currenttip

    override fun getNBTData(player: EntityPlayerMP?, te: TileEntity?, tag: NBTTagCompound?, world: World?,
                            x: Int, y: Int, z: Int): NBTTagCompound?= null

    override fun getWailaHead(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String> = currenttip
}
