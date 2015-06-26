package mods.eln.misc;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class LiveDataManager {

    public LiveDataManager() {
        FMLCommonHandler.instance().bus().register(this);
    }

    public void start() {
        //elements.clear();
    }

    public void stop() {
        map.clear();
    }

    static class Element {
        public Element(Object data, int timeout) {
            this.data = data;
            this.timeout = timeout;
        }

        Object data;
        int timeout;
    }

    public Object getData(Object key, int timeout) {
        Element e = map.get(key);
        if (e == null) return null;
        e.timeout = timeout;
        return e.data;
    }

    public Object newData(Object key, Object data, int timeout) {
        map.put(key, new Element(data, timeout));
        Utils.println("NewLiveData");
        return data;
    }

    HashMap<Object, Element> map = new HashMap<Object, LiveDataManager.Element>();

    @SubscribeEvent
    public void tick(RenderTickEvent event) {
        if (event.phase != Phase.START) return;
        ArrayList<Object> keyToRemove = new ArrayList<Object>();
        for (Entry<Object, Element> entry : map.entrySet()) {
            Element e = entry.getValue();
            e.timeout--;
            if (e.timeout < 0) {
                keyToRemove.add(entry.getKey());
                Utils.println("LiveDeleted");
            }
        }

        for (Object key : keyToRemove) {
            map.remove(key);
        }
    }
}
