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
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class GhostNodeWailaProvider(private val transparentNodeHandler: TransparentNodeHandler): IWailaDataProvider {
    companion object {
        var updateTime = Minecraft.getSystemTime()
    }

    private class WailaDataAccessorProxy(val accessor: IWailaDataAccessor, val coord: Coordonate): IWailaDataAccessor {
        override fun getPlayer() = accessor.player
        override fun getStack() = accessor.stack
        override fun getPosition() = MovingObjectPosition(coord.x, coord.y, coord.z, accessor.position.sideHit,
                accessor.position.hitVec)
        override fun getSide() = accessor.side
        override fun getBlockID() = accessor.blockID
        override fun getPartialFrame() = accessor.partialFrame
        override fun getMetadata() = accessor.metadata
        override fun getBlockQualifiedName() = accessor.blockQualifiedName
        override fun getRenderingPosition() = accessor.renderingPosition
        override fun getNBTData() = accessor.nbtData
        override fun getTileEntity() = accessor.tileEntity
        override fun getWorld() = coord.world()
        override fun getBlock() = accessor.block
        override fun getNBTInteger(tag: NBTTagCompound?, keyname: String?) = accessor.getNBTInteger(tag, keyname)
    }

    private fun getGhostData(accessor: IWailaDataAccessor): GhostNodeWailaData? {
        val coord = Coordonate(accessor.position.blockX, accessor.position.blockY, accessor.position.blockZ,
                accessor.world)
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

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?): MutableList<String>? {
        val realCoord = getGhostData(accessor)?.realCoord
        return if (realCoord != null) {
            transparentNodeHandler.getWailaBody(itemStack, currenttip, WailaDataAccessorProxy(accessor, realCoord), config)
        } else {
            currenttip
        }
    }

    override fun getWailaStack(accessor: IWailaDataAccessor, config: IWailaConfigHandler?): ItemStack? =
            getGhostData(accessor)?.itemStack

    override fun getWailaTail(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?) = currenttip

    override fun getNBTData(player: EntityPlayerMP?, te: TileEntity?, tag: NBTTagCompound?,
                            world: World?, x: Int, y: Int, z: Int): NBTTagCompound? = null

    override fun getWailaHead(itemStack: ItemStack?, currenttip: MutableList<String>, accessor: IWailaDataAccessor,
                              config: IWailaConfigHandler?): MutableList<String> = if (itemStack != null) {
            mutableListOf("§f${itemStack.displayName}")
    } else {
        currenttip
    }
}
