package mods.eln;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerManager implements ITickHandler {
	private Hashtable<EntityPlayerMP, PlayerMetadata> metadataHash = new Hashtable<EntityPlayerMP, PlayerMetadata>();
	
	public class PlayerMetadata {
		private int timeout;
		public boolean interactEnable = false;
		public boolean interactRise = false,interactRiseBuffer = false;

		public PlayerMetadata() {
			timeoutReset();
		}

		public boolean needDelete() {
			return timeout == 0;
		}

		public void timeoutReset() {
			timeout = 20 * 120;
		}

		public void timeoutDec() {
			timeout--;
			if (timeout < 0)
				timeout = 0;
		}

		public void setInteractEnable(boolean interactEnable) {
			if(this.interactEnable == false && interactEnable == true){
				interactRiseBuffer = true;
				System.out.println("interactRiseBuffer");
			}
			this.interactEnable = interactEnable;

			timeoutReset();
			System.out.println("interactEnable : " + interactEnable);
		}

		public boolean getInteractEnable() {
			timeoutReset();
			return interactEnable;
		}
		public boolean getInteractRise() {
			timeoutReset();
			return interactRise;
		}


	}

	public PlayerManager() {
		TickRegistry.registerTickHandler(this, Side.SERVER);
	}
	public void clear()
	{
		metadataHash.clear();
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		for (Entry<EntityPlayerMP, PlayerMetadata> entry : metadataHash
				.entrySet()) {
			PlayerMetadata p = entry.getValue();
			
			p.interactRise = p.interactRiseBuffer;
			p.interactRiseBuffer = false;

			if (p.needDelete()) {
				metadataHash.remove(entry.getKey());
			}
		}
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub

	}

	@Override
	public EnumSet<TickType> ticks() {
		// TODO Auto-generated method stub
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "rondoudou";
	}

	public PlayerMetadata get(EntityPlayerMP player) {
		PlayerMetadata metadata = metadataHash.get(player);
		if (metadata != null)
			return metadata;
		metadataHash.put(player, new PlayerMetadata());
		return metadataHash.get(player);

	}

	public PlayerMetadata get(EntityPlayer player) {
		return get((EntityPlayerMP) player);
	}
}
