package mods.eln.electricalbreaker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
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

        setUmin = newGuiTextField(8,28,100);
        setUmax = newGuiTextField(8,8,100);
        
        setUmin.setText(render.uMin);
        setUmax.setText(render.uMax);
		toogleSwitch = newGuiButton(100, 50 + 60,100, "toogle switch");


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
    	toogleSwitch.displayString = "state is " + render.switchState;
    }


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 166,8,84, "electricalbreaker.png");
	}


}
