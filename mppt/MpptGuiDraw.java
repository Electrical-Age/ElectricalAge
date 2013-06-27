package mods.eln.mppt;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.TransparentNodeElementInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.StatCollector;


public class MpptGuiDraw extends GuiContainer {

	
    private TransparentNodeElementInventory inventory;
    MpptRender render;
    GuiButton buttonGrounded;
    GuiVerticalTrackBar vuMeterUtarget;
    
    
    public MpptGuiDraw(EntityPlayer player, IInventory inventory,MpptRender render)
    {
        super(new MpptContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	vuMeterUtarget = new GuiVerticalTrackBar(width*1/3,height/5,20,60);
    	vuMeterUtarget.setStepIdMax((int)100);
    	vuMeterUtarget.setEnable(true);
    	vuMeterUtarget.setRange((float)render.descriptor.outUmin,(float)render.descriptor.outUmax);
    	syncVumeterUtarget();
    	
    	buttonGrounded = new GuiButton(1, width/2,height/2,100,20, "");
    }
    
    public void syncVumeterUtarget()
    {
    	vuMeterUtarget.setValue(render.UtargetSyncValue);
    	render.UtargetSyncNew = false;
    }
    
    public void drawScreen(int par1, int par2, float par3)
    {
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        buttonGrounded.displayString = "Grounded : " + render.grounded;
        this.buttonGrounded.drawButton(Minecraft.getMinecraft(), par1, par2);
    }
    
    protected void mouseClicked(int x, int y, int par3)
    {
        super.mouseClicked(x, y, par3);   
        
        if(this.buttonGrounded.mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	System.out.println("miaou");
        //	render.clientSetGrounded(!render.getGrounded());
        }
        
        vuMeterUtarget.mouseClicked(x, y, par3);
    }
    
    @Override
    protected void mouseMovedOrUp(int par1, int par2, int par3) {
    	// TODO Auto-generated method stub
    	super.mouseMovedOrUp(par1, par2, par3);
    	if(vuMeterUtarget.mouseMovedOrUp(par1, par2, par3))
    	{
    		render.clientSetUtarget(vuMeterUtarget.getValue());
    	}

    }	
    
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
        fontRenderer.drawString("Tiny", 8, 6, 4210752);
        fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);


    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2,
                    int par3) {
    		Utils.bindTextureByName("/gui/trap.png");
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            //this.mc.renderEngine.bindTexture(texture);
            int x = (width - xSize) / 2;
            int y = (height - ySize) / 2;
            this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
            
                     
            if(render.UtargetSyncNew) syncVumeterUtarget();
            vuMeterUtarget.mouseMove(par2, par3);
            vuMeterUtarget.draw(par1, par2, par3);
            
    }
}
