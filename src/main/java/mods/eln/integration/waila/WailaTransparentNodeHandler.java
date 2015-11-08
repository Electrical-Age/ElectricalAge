package mods.eln.integration.waila;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.SpecialChars;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.nodepackets.NodePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.GameRegistry;

public class WailaTransparentNodeHandler implements IWailaDataProvider {

	private long updateTime = Minecraft.getSystemTime();

	@Override
	public List<String> getWailaBody(ItemStack iStack, List<String> currenttip,
			IWailaDataAccessor accessor, IWailaConfigHandler arg3) {
			Map<String, String> tipMap = new HashMap<String, String>();
			MovingObjectPosition coord = accessor.getPosition();
			Coordonate nCoord = new Coordonate(coord.blockX, coord.blockY, coord.blockZ, accessor.getWorld());
			if (Minecraft.getSystemTime() - updateTime > 2000) {
				Eln.achNetwork.sendToServer(new NodePacket(nCoord));
				updateTime = Minecraft.getSystemTime();
			}
			try {
				tipMap = WailaCache.wailaCache.get(nCoord);
			} catch (Exception e) {
				System.out.println("Caching TransparentNode at: " + nCoord.toString());
			}
			for(Map.Entry<String, String> entry : tipMap.entrySet()) {
	            currenttip.add(entry.getKey() + ": " + /*SpecialChars.ALIGNRIGHT +*/SpecialChars.WHITE + entry.getValue());
	        }
		return currenttip;
	}

	@Override
	public List<String> getWailaHead(ItemStack arg0, List<String> arg1,
			IWailaDataAccessor arg2, IWailaConfigHandler arg3) {
		return arg1;
	}

	@Override
	public ItemStack getWailaStack(IWailaDataAccessor arg0,
			IWailaConfigHandler arg1) {
		return null;
	}

	@Override
	public List<String> getWailaTail(ItemStack arg0, List<String> arg1,
			IWailaDataAccessor arg2, IWailaConfigHandler arg3) {
		return arg1;
	}

	@Override
	public NBTTagCompound getNBTData(EntityPlayerMP arg0, TileEntity arg1,
			NBTTagCompound arg2, World arg3, int arg4, int arg5, int arg6) {
		// TODO Auto-generated method stub
		return null;
	}

}
