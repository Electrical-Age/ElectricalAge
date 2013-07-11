package mods.eln.electricalbreaker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
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

public class ElectricalBreakerGui extends GuiContainerEln{

	public ElectricalBreakerGui(EntityPlayer player, IInventory inventory,ElectricalBreakerRender render) {
		super(new ElectricalBreakerContainer(player, inventory));
		this.render = render;
	}


	GuiButton toogleSwitch;
	GuiTextFieldEln setUmin,setUmax;
	ElectricalBreakerRender render;
	
	enum SelectedType{none,min,max};

	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        setUmin = newGuiTextField(12,58/2 + 3,50);
        setUmax = newGuiTextField(12,58/2 - 5 - 10,50);

        setUmin.setText(render.uMin);
        setUmax.setText(render.uMax);
        
        setUmin.setComment(0,"Minimum voltage before cutting off");
        setUmax.setComment(0,"Maximum voltage before cutting off");
        
		toogleSwitch = newGuiButton(72-2, 58/2-10,70, "toogle switch");


	}
	


    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == setUmax)
    	{
			try{
				render.clientSetVoltageMax(Float.parseFloat (setUmax.getText()));
			} catch(NumberFormatException e)
			{

			}
    	}
    	else if(object == setUmin)
    	{
			try{
				render.clientSetVoltageMin(Float.parseFloat (setUmin.getText()));
			} catch(NumberFormatException e)
			{

			}
    	}
    	else if(object == toogleSwitch)
    	{
    		render.clientToogleSwitch();
    	}
    }

   
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	if(!render.switchState)
    		toogleSwitch.displayString = "Switch is OFF";
    	else
    		toogleSwitch.displayString = "Switch is ON";
    }


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainerSmall(this);
	}


}
