package mods.eln.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import mods.eln.misc.Utils;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

public class UuidManager {
	LinkedList<Pair> eList = new LinkedList<Pair>();
	
	public static class Pair{
		Pair(ArrayList<Integer> uuid,IUuidEntity e){
			this.uuid = uuid;
			this.e = e;
		}
		ArrayList<Integer> uuid;
		IUuidEntity e;
	}
	
	public UuidManager() {
		FMLCommonHandler.instance().bus().register(this);
	}
	
	public void add(ArrayList<Integer> uuid,IUuidEntity e){
		eList.add(new Pair(uuid, e));
	}
	
	
	@SubscribeEvent
	public void tick(TickEvent.ClientTickEvent event) {
		if(event.phase == Phase.END) return;
		Iterator<Pair> i = eList.iterator();
		while(i.hasNext()){
			Pair p = i.next();
			if(!p.e.isAlive()){
				i.remove();
			}
		}
		//Utils.println(eList.size());
	}
	
	public void kill(int uuid){
		Iterator<Pair> i = eList.iterator();
		while(i.hasNext()){
			Pair p = i.next();
			if(p.uuid == null) continue;
			for(Integer pUuid : p.uuid){
				if(pUuid == uuid){
					p.e.kill();
					i.remove();
				}
			}
		}		
	}
}
