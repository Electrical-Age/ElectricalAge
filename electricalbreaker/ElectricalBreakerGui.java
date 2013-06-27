package mods.eln.electricalbreaker;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalBreakerGui extends GuiContainer{

	public ElectricalBreakerGui(EntityPlayer player, IInventory inventory,ElectricalBreakerRender render) {
		super(new ElectricalBreakerContainer(player, inventory));
		this.render = render;
	}


	GuiButton validate,setUmin,setUmax,toogleSwitch;
	GuiTextField voltage;
	ElectricalBreakerRender render;
	
	enum SelectedType{none,min,max};
	SelectedType selected = SelectedType.none;
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

        setUmin = new GuiButton(1, 100, 50 + 20, "SetUmin");
        setUmax = new GuiButton(1, 100, 50 + 40, "SetUmax");
		validate = new GuiButton(1, 100, 50, "validate");
		toogleSwitch = new GuiButton(1, 100, 50 + 60, "toogle switch");
		buttonList.add(toogleSwitch);
		buttonList.add(setUmin);
		buttonList.add(setUmax);	
		buttonList.add(validate);	
		
		voltage = new GuiTextField(this.fontRenderer, 120, 140, 103, 12);
		this.voltage.setTextColor(-1);
        this.voltage.setDisabledTextColour(-1);
        this.voltage.setEnableBackgroundDrawing(true);
        this.voltage.setMaxStringLength(30);

	
        
        noneEntry();
	}
	
	@Override
	protected void keyTyped(char par1, int par2)
    {
        if (this.voltage.textboxKeyTyped(par1, par2))
        {

        }
        else
        {
            super.keyTyped(par1, par2);
        }
    }
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        this.voltage.mouseClicked(par1, par2, par3);
    }

    void noneEntry()
    {
    	validate.enabled = false;
    	setUmax.enabled = true;
    	setUmin.enabled = true;
    	voltage.setVisible(false);
    	selected = SelectedType.none;
    }
    void setEntry()
    {
    	validate.enabled = true;
    	setUmax.enabled = false;
    	setUmin.enabled = false;
    	voltage.setVisible(true);
    }
    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == validate)
    	{
			float newVoltage;
			
			try{
				newVoltage = Float.parseFloat (voltage.getText());
				switch (selected) {
				case max:
					render.clientSetVoltageMax(newVoltage);
					break;
				case min:
					render.clientSetVoltageMin(newVoltage);
					break;

				default:
					break;
				}
			} catch(NumberFormatException e)
			{
				noneEntry();
				return;
			}
			
			noneEntry();
    	}
    	else if(par1GuiButton == setUmax)
    	{
    		selected = SelectedType.max;
    		setEntry();
    	}
    	else if(par1GuiButton == setUmin)
    	{
    		selected = SelectedType.min;
    		setEntry();
    	}
    	else if(par1GuiButton == toogleSwitch)
    	{
    		render.clientToogleSwitch();
    	}
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
    	toogleSwitch.displayString = "state is " + render.switchState;
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        this.voltage.drawTextBox();
        
    }

	
    public boolean doesGuiPauseGame()
    {
        return false;
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		// TODO Auto-generated method stub
		
	}
	
}
