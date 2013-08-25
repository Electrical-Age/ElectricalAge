package mods.eln.teleporter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class TeleporterGui extends GuiScreenEln{

	public TeleporterGui(EntityPlayer player,TeleporterRender render) {

		this.render = render;
	}

	GuiTextFieldEln name;
	GuiTextFieldEln target;
	GuiVerticalTrackBar chargePower;
	GuiButton start;
	
	
	TeleporterRender render;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		start = newGuiButton(0,0,115, "Start");
		name = newGuiTextField(0, 20, 100);
		target = newGuiTextField(0, 40, 100);
		chargePower = newGuiVerticalTrackBar(0, 60, 20, 60);
		chargePower.setRange(2000, 20000);
		chargePower.setStepIdMax(20/2*10);
		
		
		name.setText(render.name);
		target.setText(render.targetName);
		chargePower.setValue(render.chargePower);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		// TODO Auto-generated method stub
		super.guiObjectEvent(object);
    	if(object == start)
    	{
    		render.clientSendId(TeleporterElement.startId);
    	}
    	if(object == name){
    		render.clientSendString(TeleporterElement.setNameId, name.getText());
    	}
    	if(object == target){
    		render.clientSendString(TeleporterElement.setTargetNameId, target.getText());
    	}
    	if(object == chargePower){
    		render.clientSendFloat(TeleporterElement.setChargePowerId, chargePower.getValue());
    	}
	}
	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);
		
		if(render.chargePowerNew){
			chargePower.setValue(render.chargePower);
			render.chargePowerNew = false;
		}
		
		
	/*	if(render.defaultOutput)
			toogleDefaultOutput.displayString = "default output is high";
		else
			toogleDefaultOutput.displayString = "default output is low";*/
	}
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 128, 64);
	}



	
}
