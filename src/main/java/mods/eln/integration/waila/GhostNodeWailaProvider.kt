package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.packets.GhostNodeWailaRequestPacket
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class GhostNodeWailaProvider(private val transparentNodeHandler: TransparentNodeHandler): IWailaDataProvider {
    companion object {
        var updateTime = Minecraft.getSystemTime()
    }

    private fun getGhostData(coord: Coordonate): GhostNodeWailaData? {
        var ghostData: GhostNodeWailaData? = null

        if (Minecraft.getSystemTime() - GhostNodeWailaProvider.updateTime > 2000) {
            Eln.elnNetwork.sendToServer(GhostNodeWailaRequestPacket(coord))
            GhostNodeWailaProvider.updateTime = Minecraft.getSystemTime()
        }
        try {
            ghostData = WailaCache.ghostNodes.get(coord)
        } catch(e: Exception) {}

        return ghostData
    }

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String>? {
        val pos = accessor!!.position
        val ghostData = getGhostData(Coordonate(pos.blockX, pos.blockY, pos.blockZ, accessor.world))

        if (ghostData != null) {
            val realCoord = ghostData.realCoord
            pos.blockX = realCoord.x
            pos.blockY = realCoord.y
            pos.blockZ = realCoord.z

            return transparentNodeHandler.getWailaBody(itemStack, currenttip, accessor, config)
        } else {
            return currenttip
        }
    }

    override fun getWailaStack(accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): ItemStack? {
        val pos = accessor!!.position
        val ghostData = getGhostData(Coordonate(pos.blockX, pos.blockY, pos.blockZ, accessor.world))

        if (ghostData != null) {
            return ghostData.itemStack
        } else {
            return null
        }
    }

    override fun getWailaTail(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String>? {
        return currenttip
    }

    override fun getNBTData(player: EntityPlayerMP?, te: TileEntity?, tag: NBTTagCompound?,
                            world: World?, x: Int, y: Int, z: Int): NBTTagCompound? = null

    override fun getWailaHead(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?,
                              config: IWailaConfigHandler?): MutableList<String>? {
        val itemStack = getWailaStack(accessor, config)
        if (itemStack != null) {
            return mutableListOf("§f${itemStack.displayName}")
        } else {
            return currenttip
        }
    }
}
