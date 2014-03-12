package mods.eln.misc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class LiveDataManager implements ITickHandler{
	
	public LiveDataManager() {
		TickRegistry.registerTickHandler(this, Side.CLIENT);
	}
	
	public void start(){
		//elements.clear();
	}
	public void stop(){
		map.clear();
	}

	static class Element{
		public Element(Object data,int timeout) {
			this.data = data;
			this.timeout = timeout;
		}
		
		Object data;
		int timeout;
	}
	
	public Object getData(Object key,int timeout){
		Element e = map.get(key);
		if(e == null) return null;
		e.timeout = timeout;
		return e.data;
	}
	
	public Object newData(Object key,Object data,int timeout){
		map.put(key, new Element(data,timeout));
		System.out.println("NewLiveData");
		return data;
	}
	
	HashMap<Object,Element> map = new HashMap<Object, LiveDataManager.Element>();
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		ArrayList<Object> keyToRemove = new ArrayList<Object>();
		for(Entry<Object, Element> entry : map.entrySet()){
			Element e = entry.getValue();
			e.timeout--;
			if(e.timeout < 0){
				keyToRemove.add(entry.getKey());
				System.out.println("LiveDeleted");
			}
		}
		
		for (Object key : keyToRemove) {
			map.remove(key);
		}
		
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public EnumSet<TickType> ticks() {
		// TODO Auto-generated method stub
		return EnumSet.of(TickType.RENDER);
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "miaou2";
	}
	
	
}
