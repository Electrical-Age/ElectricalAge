package mods.eln.sixnode.electricaltimeout;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
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

public class ElectricalTimeoutGui extends GuiScreenEln {

	public ElectricalTimeoutGui(EntityPlayer player,ElectricalTimeoutRender render) {
		this.render = render;
	}

	GuiButton set,reset;
	GuiTextFieldEln timeoutValue;
	ElectricalTimeoutRender render;
	
	@Override
	public void initGui() {
		super.initGui();

        reset = newGuiButton(6, 6, 50, "reset");
		set = newGuiButton(6, 6 + 20 + 4, 50, "set");

		timeoutValue = newGuiTextField(6, 6 + 20 * 2 + 4 * 2, 50);

        timeoutValue.setText(render.timeoutValue);
        timeoutValue.setComment(0, "The amount of time the");
        timeoutValue.setComment(1, "output is kept high");
	}

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if(object == set) {
    		render.clientSend(ElectricalTimeoutElement.setId);
    	}
    	else if(object == reset) {
    		render.clientSend(ElectricalTimeoutElement.resetId);
    	}
    	else if(object == timeoutValue) {
    		try {
    			float value = NumberFormat.getInstance().parse(timeoutValue.getText()).floatValue();
    			render.clientSetFloat(ElectricalTimeoutElement.setTimeOutValueId, value);
    		} catch(ParseException e) {
    		}	
    	}
    }

	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 50 + 12, 6 + 20 * 2 + 4 * 2 + 12 + 6);
	}
}
