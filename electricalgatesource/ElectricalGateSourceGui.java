package mods.eln.electricalgatesource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalGateSourceGui extends GuiScreenEln{

	public ElectricalGateSourceGui(EntityPlayer player,ElectricalGateSourceRender render) {
		this.render = render;
	}



	ElectricalGateSourceRender render;
	GuiVerticalTrackBar voltage;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();


		voltage = newGuiVerticalTrackBar(10,10,20,50);
		voltage.setStepIdMax((int)50);
		voltage.setEnable(true);
    	voltage.setRange(0f,50f);

    	syncVoltage();
	}
    public void syncVoltage()
    {
    	voltage.setValue(render.voltageSyncValue);
    	render.voltageSyncNew = false;
    }


    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == voltage)
    	{
    		render.clientSetFloat(ElectricalGateSourceElement.setVoltagerId,voltage.getValue());
    	}
    }

    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	if(render.voltageSyncNew) syncVoltage();
    }

	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 176, 166, "electricalgatesource.png");
	}
	
}
