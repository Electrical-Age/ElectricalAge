package mods.eln.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeManager;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.event.world.WorldEvent.Unload;

import java.util.LinkedList;

public class ServerEventListener {

    LinkedList<EntityLightningBolt> lightningListNext = new LinkedList<EntityLightningBolt>();
    LinkedList<EntityLightningBolt> lightningList = new LinkedList<EntityLightningBolt>();

	public ServerEventListener(){
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if (event.phase != Phase.END) return;

		lightningList = lightningListNext;
		lightningListNext = new LinkedList<EntityLightningBolt>();
	}

	@SubscribeEvent
	public void onNewEntity(EntityConstructing event) {
		if (event.entity instanceof EntityLightningBolt) {
			lightningListNext.add((EntityLightningBolt)event.entity);
		}
	}

	public void clear() {
		lightningList.clear();
	}

	public double getLightningClosestTo(Coordonate c) {
		double best = 10000000;
		for (EntityLightningBolt l : lightningList) {
			if (c.world() != l.worldObj) continue;
			double d = l.getDistance(c.x, c.y, c.z);
			if (d < best) best = d;
		}
		return best;
	}

	@SubscribeEvent	
	public void onWorldLoad(Load e) {
		if (e.world.isRemote) return;
		ElnWorldStorage storage = ElnWorldStorage.forWorld(e.world);
	}

	@SubscribeEvent	
	public void onWorldUnload(Unload e) {
		if (e.world.isRemote) return;
		NodeManager.instance.unload(e.world.provider.dimensionId);
		Eln.ghostManager.unload(e.world.provider.dimensionId);
	}
		
	@SubscribeEvent	
	public void onWorldSave(Save e) {
		if (e.world.isRemote) return;
		//ElnWorldStorage storage = ElnWorldStorage.forWorld(e.world);
		int idx = 0;
		idx++;
	}
}
