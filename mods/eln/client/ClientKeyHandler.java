package mods.eln.client;

import java.io.IOException;
import java.util.EnumSet;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

import mods.eln.Eln;
import mods.eln.GuiHandler;
import mods.eln.PacketHandler;
import mods.eln.misc.Utils;
import mods.eln.wiki.Root;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.packet.Packet250CustomPayload;


public class ClientKeyHandler extends KeyHandler {

	static final String stuffInteractA = "stuffInteractA";
	static final String stuffInteractB = "stuffInteractB++";
	static final String interact = "ElnInteract";
	static final String openWiki = "Open Wiki";
	

	
	
	public ClientKeyHandler() {
	        //the first value is an array of KeyBindings, the second is whether or not the call 
	//keyDown should repeat as long as the key is down
    	
	        super(new KeyBinding[]{	new KeyBinding(stuffInteractA, Keyboard.KEY_V),
	    							new KeyBinding(stuffInteractB, Keyboard.KEY_B),
	    							new KeyBinding(openWiki, Keyboard.KEY_X),
	        						new KeyBinding(interact, Keyboard.KEY_C)},
	        						new boolean[]{false,false,false,false});
	        KeyBindingRegistry.registerKeyBinding(this);
	}
	
	@Override
	public String getLabel() {
	        return "mykeybindings";
	}
	
	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb,
	                boolean tickEnd, boolean isRepeat) {
		int i = 0;
		i++;
	  //  System.out.println("keyDown " + kb + "   " +  tickEnd + "   " + isRepeat);
	    
	    if(! tickEnd) return;
	    if(Minecraft.getMinecraft().currentScreen != null) return;
	    		
	    if(kb.keyDescription.equals(openWiki)){	    	
	    	Utils.clientOpenGui(new Root(null));
	    	return;
	    }
	    
        Packet250CustomPayload packet = new Packet250CustomPayload();
        packet.channel = Eln.channelName;
        packet.data = new byte[2];
        packet.length = 2;
        boolean job = false;
        packet.data[0] = Eln.packetPlayerKey;
        if(kb.keyDescription.equals(stuffInteractA))
        {
        	packet.data[1] = PacketHandler.stuffInteractAId; job = true;
        }
        if(kb.keyDescription.equals(stuffInteractB))
        {
        	packet.data[1] = PacketHandler.stuffInteractBId; job = true;
        }
        if(kb.keyDescription.equals(interact))
        {
        	packet.data[1] = PacketHandler.interactEnableId; job = true;
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
        
        if(kb.keyDescription.equals(interact))
        {
        	packet.data[1] = PacketHandler.interactDisableId; job = true;
        }
        
        
           
        if(!job)return;
    	PacketDispatcher.sendPacketToServer(packet);
	}
	
	@Override
	public EnumSet<TickType> ticks() {
	        return EnumSet.of(TickType.CLIENT);
	       
	}
}