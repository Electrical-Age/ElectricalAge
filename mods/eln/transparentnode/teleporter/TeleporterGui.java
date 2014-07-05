package mods.eln.transparentnode.teleporter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

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

		
		name = newGuiTextField(6, 6, 80);
		target = newGuiTextField(6, 6+20, 80);
		start = newGuiButton(6,6+20+6+12,80, "Start");
		chargePower = newGuiVerticalTrackBar(6+80+6, 7, 20, 56);
		chargePower.setRange(2000, 20000);
		chargePower.setStepIdMax(20/2*10);
		
		
		name.setText(render.name);
		target.setText(render.targetName);
		chargePower.setValue(render.chargePower);
		
		name.setComment(0, "Current Transporter");
		target.setComment(0, "Target Transporter");
		chargePower.setComment(0, "Power Sink:");
		
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
		chargePower.setComment(0, Utils.plotPower("Power Sink:", chargePower.getValue()));
		start.enabled = render.state == TeleporterElement.StateIdle;
		
	/*	if(render.defaultOutput)
			toogleDefaultOutput.displayString = "default output is high";
		else
			toogleDefaultOutput.displayString = "default output is low";*/
	}
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 6+80+6+20+6+2, 70);
	}



	
}
