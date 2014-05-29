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
	static final String stuffInteractA = "stuffInteractA";
	static final String stuffInteractB = "stuffInteractB++";
	static final String interact = "ElnInteract";
	static final String openWiki = "Open Wiki";
	private static final int[] keyValues = {Keyboard.KEY_V,Keyboard.KEY_B,Keyboard.KEY_X,Keyboard.KEY_C};
	private static final String[] desc = {stuffInteractA,stuffInteractB,interact,openWiki};
	public static final KeyBinding[] keys = new KeyBinding[desc.length];
	
	boolean[] states = new boolean[desc.length];
	
	Minecraft mc;
	
	public ClientKeyHandler() {
		mc = Minecraft.getMinecraft();
		
		for (int i = 0; i < desc.length; ++i) {
			states[i] = false;
			keys[i] = new KeyBinding(desc[i], keyValues[i], StatCollector.translateToLocal("MiaouuuXXX"));
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

        
    	Utils.sendPacketToServer(bos);		
	}
	/*static final String stuffInteractA = "stuffInteractA";
	static final String stuffInteractB = "stuffInteractB++";
	static final String interact = "ElnInteract";
	static final String openWiki = "Open Wiki";*/
	//1.7.2
	/*public ClientKeyHandler() {
		
		Minecraft mc = Minecraft.getMinecraft();
		for (int i = 0; i < desc.length; ++i) {
			keys[i] = new KeyBinding(desc[i], keyValues[i], StatCollector.translateToLocal("key.tutorial.label"));
			ClientRegistry.registerKeyBinding(keys[i]);
		}*/
	    //the first value is an array of KeyBindings, the second is whether or not the call 
		//keyDown should repeat as long as the key is down
	   /* super(new KeyBinding[]{	new KeyBinding(stuffInteractA, Keyboard.KEY_V),
	    						new KeyBinding(stuffInteractB, Keyboard.KEY_B),
	    						new KeyBinding(openWiki, Keyboard.KEY_X),
	        					new KeyBinding(interact, Keyboard.KEY_C)},
	        					new boolean[]{false, false, false, false});
	    KeyBindingRegistry.registerKeyBinding(this);*/
		
		
		
		
		/*public static final KeyBinding[] keys = new KeyBinding[desc.length];

		public KeyHandler() {
			mc = Minecraft.getMinecraft();
			for (int i = 0; i < desc.length; ++i) {
				keys[i] = new KeyBinding(desc[i], keyValues[i], StatCollector.translateToLocal("key.tutorial.label"));
				ClientRegistry.registerKeyBinding(keys[i]);
			}
		}*/
	//	ClientRegistry.registerKeyBinding(keys[i]);
//	}
	
	/*@Override
	public String getLabel() {
	    return "mykeybindings";
	}
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		
	
	}
	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb,
	                boolean tickEnd, boolean isRepeat) {
		int i = 0;
		i++;
	    // Utils.println("keyDown " + kb + "   " +  tickEnd + "   " + isRepeat);
	    
	    if(! tickEnd) return;
	    if(Minecraft.getMinecraft().currentScreen != null) return;
	    		
	    if(kb.keyDescription.equals(openWiki)) {	    	
	    	UtilsClient.clientOpenGui(new Root(null));
	    	return;
	    }
	    
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = Eln.channelName;
        packet.data = new byte[2];
        packet.length = 2;
        boolean job = false;
        packet.data[0] = Eln.packetPlayerKey;
        
        if(kb.keyDescription.equals(stuffInteractA)) {
        	packet.data[1] = PacketHandler.stuffInteractAId;
        	job = true;
        }
        if(kb.keyDescription.equals(stuffInteractB)) {
        	packet.data[1] = PacketHandler.stuffInteractBId;
        	job = true;
        }
        if(kb.keyDescription.equals(interact)) {
        	packet.data[1] = PacketHandler.interactEnableId;
        	job = true;
        }
        
    	if(!job)return;
    	PacketDispatcher.sendPacketToServer(packet);
	}
	
	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
	    if(! tickEnd) return;
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = Eln.channelName;
        packet.data = new byte[2];
        packet.length = 2;
        
        packet.data[0] = Eln.packetPlayerKey;

        boolean job = false;
        
        if(kb.keyDescription.equals(interact)) {
        	packet.data[1] = PacketHandler.interactDisableId; 
        	job = true;
        }
        
        if(!job)return;
    	PacketDispatcher.sendPacketToServer(packet);
	}
	
	@Override
	public EnumSet<TickType> ticks() {
	        return EnumSet.of(TickType.CLIENT);
	}*/
}
