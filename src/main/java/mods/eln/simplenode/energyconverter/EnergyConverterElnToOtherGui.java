package mods.eln.simplenode.energyconverter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
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

public class EnergyConverterElnToOtherGui extends GuiScreenEln {

	public EnergyConverterElnToOtherGui(EntityPlayer player, EnergyConverterElnToOtherEntity render) {
		this.render = render;
	}

	EnergyConverterElnToOtherEntity render;
	GuiVerticalTrackBar voltage;

	@Override
	public void initGui() {
		super.initGui();

		voltage = newGuiVerticalTrackBar(6, 6 + 2, 20, 50);
		voltage.setStepIdMax((int)100);
		voltage.setEnable(true);
    	voltage.setRange(0f, 1f);

    	syncVoltage();
	}
	
    public void syncVoltage() {
    	voltage.setValue(render.inPowerFactor);
    	render.hasChanges = false;
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if(object == voltage) {
    		render.sender.clientSendFloat(EnergyConverterElnToOtherNode.setInPowerFactor, voltage.getValue());
    	}
    }

    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if(render.hasChanges) syncVoltage();
    	voltage.setComment(0, "Input power is limited to  " + (int)(voltage.getValue()*render.inPowerMax) + " W");
    }

	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 12 + 20, 12 + 50 + 4);
	}
}
