package mods.eln.electricalgatesource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.gui.GuiVerticalTrackBar;
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

public class ElectricalGateSourceGui extends GuiContainer{

	public ElectricalGateSourceGui(EntityPlayer player, IInventory inventory,ElectricalGateSourceRender render) {
		super(new ElectricalGateSourceContainer(player, inventory));
		this.render = render;
	}



	ElectricalGateSourceRender render;
	GuiVerticalTrackBar voltage;

	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();


		voltage = new GuiVerticalTrackBar(width*1/3,height/5,20,50);
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
	protected void keyTyped(char par1, int par2)
    {
 
        {
            super.keyTyped(par1, par2);
        }
    }
    protected void mouseClicked(int par1, int par2, int par3)
    {
        super.mouseClicked(par1, par2, par3);
        voltage.mouseClicked(par1, par2, par3);
    }

    @Override
    protected void mouseMovedOrUp(int par1, int par2, int par3) {
    	// TODO Auto-generated method stub
    	super.mouseMovedOrUp(par1, par2, par3);
    	if(voltage.mouseMovedOrUp(par1, par2, par3))
    	{
    		render.clientSetFloat(ElectricalGateSourceElement.setVoltagerId,voltage.getValue());
    	}

    }
    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
    
    }
   
    /**
     * Draws the screen and all the components in it.
     */
    @Override
    public void drawScreen(int par1, int par2, float par3)
    {
  
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);

        
    }

	
    public boolean doesGuiPauseGame()
    {
        return false;
    }

	@Override
	protected void drawGuiContainerBackgroundLayer(float par1, int par2,
            int par3) {
		// TODO Auto-generated method stub
       if(render.voltageSyncNew) syncVoltage();
       voltage.mouseMove(par2, par3);
       voltage.draw(par1, par2, par3);
        
	}
	
}
