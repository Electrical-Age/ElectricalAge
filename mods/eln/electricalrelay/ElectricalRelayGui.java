package mods.eln.electricalrelay;

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
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalRelayGui extends GuiScreenEln {

	public ElectricalRelayGui(EntityPlayer player,ElectricalRelayRender render) {
		this.render = render;
	}

	GuiButton toogleDefaultOutput;
	ElectricalRelayRender render;

	@Override
	public void initGui() {
		super.initGui();

		toogleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, "toogle switch");
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
    	if(object == toogleDefaultOutput) {
    		render.clientToogleDefaultOutput();
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if(render.defaultOutput)
			toogleDefaultOutput.displayString = "Normally closed";
		else
			toogleDefaultOutput.displayString = "Normally open";
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 128, 32);
	}
}
