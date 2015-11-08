package mods.eln.nodepackets;

import java.util.Map;

import mods.eln.integration.waila.WailaCache;
import mods.eln.misc.Coordonate;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class BatteryElementReturnHandler implements IMessageHandler<BatteryElementReturn, IMessage>{
	
	@Override
	public IMessage onMessage(BatteryElementReturn message, MessageContext ctx) {
		Map<String, String> m = message.map;
		Coordonate c = message.coord;
		WailaCache.wailaCache.put(c, m);
		return null;
	}

}
