package mods.eln.modbusrtu;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.electricalmath.ElectricalMathElement;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.wiki.GuiVerticalExtender;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ModbusRtuGui extends GuiScreenEln{

	public ModbusRtuGui(EntityPlayer player,ModbusRtuRender render) {

		this.render = render;
	}

	ModbusRtuRender render;

	
	GuiTextFieldEln station,name;
	GuiVerticalExtender extender;
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		
		extender = new GuiVerticalExtender(0, 0, 128, 128, helper);
		add(extender);
		
		int y = 0;
		station = new GuiTextFieldEln(fontRenderer, 0, y, 50, helper);  y+= station.getHeight();
		if(render.station != -1)
			station.setText(render.station);
		station.setObserver(this);
		extender.add(station);
		name = new GuiTextFieldEln(fontRenderer, 0, y, 50, helper);  y+= name.getHeight();
		name.setText(render.name);
		name.setObserver(this);
		extender.add(name);
		//add(name);
		
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
    	if(object == station){
    		render.clientSendInt(ModbusRtuElement.setStation, Integer.parseInt(station.getText()));
    	}else if(object == name){
    		render.clientSendString(ModbusRtuElement.setName, name.getText());
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);

	}
	GuiHelper helper;
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return helper = new GuiHelper(this, 128, 64);
	}



	
}
