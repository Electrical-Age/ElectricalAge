package mods.eln;

import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Map.Entry;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.relauncher.Side;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PlayerManager {
	private Hashtable<EntityPlayerMP, PlayerMetadata> metadataHash = new Hashtable<EntityPlayerMP, PlayerMetadata>();
	
	public class PlayerMetadata {
		private int timeout;
		public boolean interactEnable = false;
		public boolean interactRise = false,interactRiseBuffer = false;
		EntityPlayer player;
		public PlayerMetadata(EntityPlayer p) {
			timeoutReset();
			this.player = p;
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
				Utils.println("interactRiseBuffer");
			}
			this.interactEnable = interactEnable;

			timeoutReset();
			Utils.println("interactEnable : " + interactEnable);
		}

		/*public boolean getInteractEnable() {
			timeoutReset();
			//return interactEnable;
			return player.isSneaking();
		}*/
		/*public boolean getInteractRise() {
			timeoutReset();
			return interactRise;
		}*/


	}

	public PlayerManager() {
		FMLCommonHandler.instance().bus().register(this);
	}
	public void clear()
	{
		metadataHash.clear();
	}
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if(event.phase != Phase.START) return;
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



	public PlayerMetadata get(EntityPlayerMP player) {
		PlayerMetadata metadata = metadataHash.get(player);
		if (metadata != null)
			return metadata;
		metadataHash.put(player, new PlayerMetadata(player));
		return metadataHash.get(player);

	}

	public PlayerMetadata get(EntityPlayer player) {
		return get((EntityPlayerMP) player);
	}
}
