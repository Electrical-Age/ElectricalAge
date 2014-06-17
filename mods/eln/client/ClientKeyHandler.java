package mods.eln.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.EnumSet;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.IGuiHandler;

import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.PacketHandler;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.StatCollector;

public class ClientKeyHandler {
	static final String stuffInteractA = "Not used";
	static final String stuffInteractB = "Not used 42";
	static final String interact = "Interact";
	static final String openWiki = "Open Wiki";
	private static final int[] keyValues = {Keyboard.KEY_V,Keyboard.KEY_B,Keyboard.KEY_C,Keyboard.KEY_X};
	private static final String[] desc = {stuffInteractA,stuffInteractB,interact,openWiki};
	public static final KeyBinding[] keys = new KeyBinding[desc.length];
	
	boolean[] states = new boolean[desc.length];
	
	Minecraft mc;
	
	public ClientKeyHandler() {
		mc = Minecraft.getMinecraft();
		
		for (int i = 0; i < desc.length; ++i) {
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
