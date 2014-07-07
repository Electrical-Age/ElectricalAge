package mods.eln.sixnode.wirelesssignal;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;

public class WirelessSignalAnalyserItemDescriptor extends GenericItemUsingDamageDescriptor {

	public WirelessSignalAnalyserItemDescriptor(String name) {
		super(name);
		
	}
	
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
			int x, int y, int z, int side, float vx, float vy, float vz) {
		if(world.isRemote) return true;
		
		Direction dir = Direction.fromIntMinecraftSide(side);
		Coordonate c = new Coordonate(x,y,z,world);
		c.move(dir);

		ArrayList<WirelessSignalInfo> list = WirelessSignalRxProcess.getTxList(c);
		int idx = 0;
		for(WirelessSignalInfo e : list)
		{
			Utils.addChatMessage(player,e.tx.getChannel() + " Strength=" + String.format("%2.1f",e.power) +" Value=" +String.format("%2.1fV",e.tx.getValue() * Eln.instance.SVU));
			idx++;
		}
		if(list.size() == 0)
		{
			Utils.addChatMessage(player,"No wireless signal in area!");
		}
		return true;
	}
	
	

}
