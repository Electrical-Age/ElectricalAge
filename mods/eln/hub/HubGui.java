package mods.eln.hub;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import org.lwjgl.opengl.GL11;

import mods.eln.electricaltimout.ElectricalTimeoutElement;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.HelperStdContainerSmall;
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

public class HubGui extends GuiContainerEln{

	public HubGui(EntityPlayer player, IInventory inventory,HubRender render) {
		super(new HubContainer(player, inventory));
		this.render = render;
	}


	GuiButtonEln connectionGridToggle[] = new GuiButtonEln[6];

	HubRender render;
	

	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		for(int idx = 0;idx < 6;idx++){
			connectionGridToggle[idx] = newGuiButton(6, 6 + idx * 20, 50, "");
		}
	}
	
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	for(int idx = 0;idx < 6;idx++){
    		if(object == connectionGridToggle[idx]){
    			render.clientSetByte(HubElement.clientConnectionGridToggle,(byte) idx);
    		}
    	}
    }

	@Override
	protected void preDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.preDraw(f, x, y);
		for(int idx = 0;idx < 6;idx++){
			connectionGridToggle[idx].displayString = render.connectionGrid[idx] ? "is on" : "is off";
    	}
	}

	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166+50,8,84+50);				
	}


}
