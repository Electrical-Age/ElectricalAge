package mods.eln.heatfurnace;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.TransparentNodeElementInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.StatCollector;




public class HeatFurnaceGuiDraw extends GuiContainer {

	
    private TransparentNodeElementInventory inventory;
    HeatFurnaceRender render;
    GuiButton externalControl,takeFuel;
    GuiVerticalTrackBar vuMeterGain;
    GuiVerticalTrackBarHeat vuMeterHeat;
    
    
    public HeatFurnaceGuiDraw(EntityPlayer player, IInventory inventory,HeatFurnaceRender render)
    {
        super(new HeatFurnaceContainer(null,player, inventory,render.descriptor));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	

    	externalControl = new GuiButton(1, width/2,height/2,100,20, "");
    	takeFuel = new GuiButton(1, width/2,height/2 + 20,100,20, "");
    	
    	vuMeterGain = new GuiVerticalTrackBar(width*1/3,height/5,20,60);
    	vuMeterGain.setStepIdMax((int) (0.9f/0.01f));
    	vuMeterGain.setEnable(true);
    	vuMeterGain.setRange(0.1f,1.0f);
    	syncVumeterGain();
   	
    	vuMeterHeat = new GuiVerticalTrackBarHeat(width*2/3,height/5,20,60);
    	vuMeterHeat.setStepIdMax(100);
    	vuMeterHeat.setEnable(true);
    	vuMeterHeat.setRange(0.0f,1000.0f);
    	syncVumeterHeat();

    }
    public void syncVumeterGain()
    {
    	vuMeterGain.setValue(render.gainSyncValue);
    	render.gainSyncNew = false;
    }
    public void syncVumeterHeat()
    {
    	vuMeterHeat.setValue(render.temperatureTargetSyncValue);
    	render.temperatureTargetSyncNew = false;
    }
       
    public void drawScreen(int par1, int par2, float par3)
    {
        super.drawScreen(par1, par2, par3);
        GL11.glDisable(GL11.GL_LIGHTING);
        externalControl.displayString = "External control : " + render.controleExternal;
        externalControl.drawButton(Minecraft.getMinecraft(), par1, par2);
        takeFuel.displayString = "Take fuel : " + render.takeFuel;
        takeFuel.drawButton(Minecraft.getMinecraft(), par1, par2);
        takeFuel.enabled = !render.controleExternal;
    }
    
    protected void mouseClicked(int x, int y, int par3)
    {
        super.mouseClicked(x, y, par3);   
        
        if(this.externalControl.mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	render.clientToogleControl();
        }
        
        if(this.takeFuel.mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	render.clientToogleTakeFuel();
        }       
        vuMeterGain.mouseClicked(x, y, par3);
        vuMeterHeat.mouseClicked(x, y, par3);
    }
	
    @Override
    protected void mouseMovedOrUp(int par1, int par2, int par3) {
    	// TODO Auto-generated method stub
    	super.mouseMovedOrUp(par1, par2, par3);
    	if(vuMeterGain.mouseMovedOrUp(par1, par2, par3))
    	{
    		render.clientSetGain(vuMeterGain.getValue());
    	}
    	if(vuMeterHeat.mouseMovedOrUp(par1, par2, par3))
    	{
    		render.clientSetTemperatureTarget(vuMeterHeat.getValue());
    	}
    }
    
    
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
            fontRenderer.drawString("Power : " + render.power + " T : " + render.temperature, 8, 6, 4210752);
            fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2,
                    int par3) {
    		
    		Utils.bindTextureByName("/gui/trap.png");
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
//            this.mc.renderEngine.bindTexture(texture);
            int x = (width - xSize) / 2;
            int y = (height - ySize) / 2;
            this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
            
            
            vuMeterGain.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null && render.controleExternal == false);            
            if(render.gainSyncNew) syncVumeterGain();
            vuMeterGain.mouseMove(par2, par3);
            vuMeterGain.draw(par1, par2, par3);
            
            vuMeterHeat.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) != null && render.controleExternal == false);
            if(render.temperatureTargetSyncNew) syncVumeterHeat();
            vuMeterHeat.mouseMove(par2, par3);
            vuMeterHeat.temperatureHit = (float) render.temperature;
            if(render.controleExternal == false) vuMeterHeat.draw(par1, par2, par3);
    }
}
