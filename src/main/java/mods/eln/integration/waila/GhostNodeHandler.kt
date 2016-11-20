package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaConfigHandler
import mcp.mobius.waila.api.IWailaDataAccessor
import mcp.mobius.waila.api.IWailaDataProvider
import mcp.mobius.waila.api.SpecialChars
import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.packets.GhostNodeRequestPacket
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaDataProvider", modid = "Waila")
class GhostNodeHandler : IWailaDataProvider {
    companion object {
        var updateTime = Minecraft.getSystemTime()
    }

    override fun getWailaBody(itemStack: ItemStack?, currenttip: MutableList<String>?, accessor: IWailaDataAccessor?, config: IWailaConfigHandler?): MutableList<String>? {
        var tipMap: Map<String, String>? = mapOf()
        val coord = accessor!!.position
        val nodeCoord = Coordonate(coord.blockX, coord.blockY, coord.blockZ, accessor.world)
        if(Minecraft.getSystemTime() - GhostNodeHandler.updateTime > 2000){
            Eln.elnNetwork.sendToServer(GhostNodeRequestPacket(Coordonate(nodeCoord)))
            GhostNodeHandler.updateTime = Minecraft.getSystemTime()
        }
        try {
            tipMap = WailaCache.ghostNodes.get(nodeCoord)
        } catch(e: Exception){
            //This is probably just it complaining about the cache returning null. Should be safe to ignore.
        }
        for((key, value) in tipMap!!.asSequence()){
            currenttip!! += (key + ": " + SpecialChars.WHITE + value)
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
