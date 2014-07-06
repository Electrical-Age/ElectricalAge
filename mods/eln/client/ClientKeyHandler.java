package mods.eln.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.PacketHandler;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
//import mods.eln.wiki.Root;

public class ClientKeyHandler {

	static final int idOffset = 3;
	static final String openWiki = "Open Wiki";
	private static final int[] keyValues = {Keyboard.KEY_X};
	private static final String[] desc = {openWiki};
	public static final KeyBinding[] keys = new KeyBinding[desc.length];
	
	boolean[] states = new boolean[desc.length];
	
	Minecraft mc;
	
	public ClientKeyHandler() {
		mc = Minecraft.getMinecraft();
		
		for (int i = 0; i < desc.length; ++i) {
			if(i != 3)
			states[i] = false;
			keys[i] = new KeyBinding(desc[i], keyValues[i], StatCollector.translateToLocal("ElectricalAge"));
			ClientRegistry.registerKeyBinding(keys[i]);
		}
	}
	
	
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {

		for (int i = 0; i < desc.length; ++i) {
			boolean s = keys[i].getIsKeyPressed();
			if(s == false) continue;
			if(states[i])
				setState(i,false);
			setState(i,true);
		}
	}
	
	@SubscribeEvent
	public void tick(ClientTickEvent event) {
		if(event.phase != Phase.START) return;
		for (int i = 0; i < desc.length; ++i) {
			boolean s = keys[i].getIsKeyPressed();
			if(s == false && states[i] == true){
				setState(i,false);
			}
		}		
	}
	
	
	
	void setState(int id,boolean state){
		states[id] = state;
		id+=idOffset;
	    if(id == PacketHandler.openWikiId) {	    	
	    	UtilsClient.clientOpenGui(new Root(null));
	    }	
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream stream = new DataOutputStream(bos);   	
        
        
        try {
        	stream.writeByte(Eln.packetPlayerKey);
			stream.writeByte(id);
	        stream.writeBoolean(state);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
        UtilsClient.sendPacketToServer(bos);		
	}
}
