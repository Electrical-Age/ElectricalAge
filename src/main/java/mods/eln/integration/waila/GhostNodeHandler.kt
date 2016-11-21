package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mods.eln.misc.Coordonate
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class GhostNodeHandler(private val transparentNodeHandler: TransparentNodeHandler): IWailaDataProvider {
    companion object {
        var updateTime = Minecraft.getSystemTime()
    }

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): MutableList<String>? {
        var pos = accessor!!.position
        val coord = Coordonate(pos.blockX, pos.blockY, pos.blockZ, accessor.world)
        val realCoord = WailaCache.ghostNodes.get(coord)
        pos.blockX = realCoord.x
        pos.blockY = realCoord.y
        pos.blockZ = realCoord.z

        return transparentNodeHandler.getWailaBody(itemStack, currenttip, accessor, config)
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
