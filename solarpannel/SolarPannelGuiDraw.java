package mods.eln.solarpannel;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
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


public class SolarPannelGuiDraw extends GuiContainer {

	
    private TransparentNodeElementInventory inventory;
    SolarPannelRender render;
    GuiButton buttonGrounded;   
    GuiVerticalTrackBarHeat vuMeterTemperature;
    
    public SolarPannelGuiDraw(EntityPlayer player, IInventory inventory,SolarPannelRender render)
    {
        super(new SolarPannelContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	buttonGrounded = new GuiButton(1, width*2/3,height/2,100,20, "");
    	vuMeterTemperature = new GuiVerticalTrackBarHeat(width*1/3,height/5,20,60);
    	vuMeterTemperature.setStepIdMax(181);
    	vuMeterTemperature.setEnable(true);
    	vuMeterTemperature.setRange((float)render.descriptor.alphaMin,(float)render.descriptor.alphaMax);
    	syncVumeter();
    }
    
    public void syncVumeter()
    {
    	vuMeterTemperature.setValue(render.pannelAlphaSyncValue);
    	render.pannelAlphaSyncNew = false;
    }
    
    public void drawScreen(int par1, int par2, float par3)
    {
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        buttonGrounded.displayString = "powerOn : caca" ;
        this.buttonGrounded.drawButton(Minecraft.getMinecraft(), par1, par2);
    }
    
    protected void mouseClicked(int x, int y, int par3)
    {
        super.mouseClicked(x, y, par3);   
      //  System.out.println("c");
        if(this.buttonGrounded.mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	//render.clientSetPowerOn(!render.getPowerOn());
        }
        vuMeterTemperature.mouseClicked(x, y, par3);
    }
	
    @Override
    protected void mouseMovedOrUp(int x, int y, int which) {
    	// TODO Auto-generated method stub
    	super.mouseMovedOrUp(x, y, which);
    	if(vuMeterTemperature.mouseMovedOrUp(x, y, which))
    	{
    		render.clientSetPannelAlpha(vuMeterTemperature.getValue());
    	}
    }
    
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
        fontRenderer.drawString("Alpha " + render.pannelAlphaSyncNew, 8, 6, 4210752);
        fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int x,
                    int y) {
    	vuMeterTemperature.setEnable(! render.hasTracker);
    	
    	Utils.bindTextureByName("/gui/trap.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
       // this.mc.renderEngine.bindTexture(texture);
        int xDraw = (width - xSize) / 2;
        int yDraw = (height - ySize) / 2;
        this.drawTexturedModalRect(xDraw, yDraw, 0, 0, xSize, ySize);
        
        if(render.pannelAlphaSyncNew) syncVumeter();
        vuMeterTemperature.mouseMove(x, y);
        vuMeterTemperature.temperatureHit = (float) (SolarPannelSlowProcess.getSolarAlpha(render.tileEntity.worldObj));
        vuMeterTemperature.draw(par1,x,y);
            
    }
}
