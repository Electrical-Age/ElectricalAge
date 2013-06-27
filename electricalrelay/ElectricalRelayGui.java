package mods.eln.electricalrelay;

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

public class ElectricalRelayGui extends GuiContainer{

	public ElectricalRelayGui(EntityPlayer player, IInventory inventory,ElectricalRelayRender render) {
		super(new ElectricalRelayContainer(player, inventory));
		this.render = render;
	}


	GuiButton toogleDefaultOutput;
	ElectricalRelayRender render;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();

		toogleDefaultOutput = new GuiButton(1, 100, 50 + 60, "toogle switch");
		buttonList.add(toogleDefaultOutput);

	}
	
	@Override
	protected void keyTyped(char par1, int par2)
    {
    
        {
            super.keyTyped(par1, par2);
        }
    }
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
      
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    	if(par1GuiButton == toogleDefaultOutput)
    	{
    		render.clientToogleDefaultOutput();
    	}
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
    	toogleDefaultOutput.displayString = "default output is " + render.defaultOutput;
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
 
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
