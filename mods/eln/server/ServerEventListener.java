package mods.eln.server;

import java.util.LinkedList;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

public class ServerEventListener {
	public ServerEventListener(){
		MinecraftForge.EVENT_BUS.register(this);
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if(event.phase != Phase.END) return;
		
		lightningList = lightningListNext;
		lightningListNext = new LinkedList<EntityLightningBolt>();
	}
	
	LinkedList<EntityLightningBolt> lightningListNext = new LinkedList<EntityLightningBolt>();;
	LinkedList<EntityLightningBolt> lightningList = new LinkedList<EntityLightningBolt>();
	@SubscribeEvent
	public void onNewEntity(EntityConstructing event){
		if(event.entity instanceof EntityLightningBolt){
			lightningListNext.add((EntityLightningBolt)event.entity);
		}
	}

	public void clear() {
		lightningList.clear();
	}
	
	
	public double getLightningClosestTo(Coordonate c){
		double best = 10000000;
		for(EntityLightningBolt l : lightningList){
			if(c.world() != l.worldObj) continue;
			double d = l.getDistance(c.x,c.y,c.z);
			if(d < best) best = d;
		}
		return best;
	}
	
}
