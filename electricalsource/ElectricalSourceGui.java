package mods.eln.electricalsource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.network.packet.Packet250CustomPayload;

public class ElectricalSourceGui extends GuiScreen{

	GuiButton validate;
	GuiTextField voltage;
	ElectricalSourceRender render;
	
	public ElectricalSourceGui(ElectricalSourceRender render) {
		this.render = render;
	}
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		validate = new GuiButton(1, 100, 50, "miaou");
		
		buttonList.add(validate);	
		voltage = new GuiTextField(this.fontRenderer, 120, 100, 103, 12);
		this.voltage.setTextColor(-1);
        this.voltage.setDisabledTextColour(-1);
        this.voltage.setEnableBackgroundDrawing(true);
        this.voltage.setMaxStringLength(30);
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

    
    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == validate)
    	{
			float newVoltage;
			
			try{
				newVoltage = Float.parseFloat (voltage.getText());
			} catch(NumberFormatException e)
			{
				return;
			}
			
			try {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
			    DataOutputStream stream = new DataOutputStream(bos);   	
			
			    render.preparePacketForServer(stream);
				
				stream.writeByte(ElectricalSourceElement.setVoltageId);
				stream.writeFloat(newVoltage);
				
				render.sendPacketToServer(bos);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
    	}
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int par1, int par2, float par3)
    {
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        this.voltage.drawTextBox();
    }

	
    public boolean doesGuiPauseGame()
    {
        return false;
    }
}
