package mods.eln.electricalalarm;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
public class ElectricalAlarmGui extends GuiScreenEln {

	public ElectricalAlarmGui(EntityPlayer player,ElectricalAlarmRender render) {
		this.render = render;
	}

	GuiButton toogleDefaultOutput;
	ElectricalAlarmRender render;

	@Override
	public void initGui() {
		super.initGui();

		toogleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, "toogle switch");
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
    	if(object == toogleDefaultOutput) {
    		render.clientSend(ElectricalAlarmElement.clientSoundToggle);
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if(!render.mute)
			toogleDefaultOutput.displayString = "Sound is not muted";
		else
			toogleDefaultOutput.displayString = "Sound is muted";
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 128, 32);
	}
}
