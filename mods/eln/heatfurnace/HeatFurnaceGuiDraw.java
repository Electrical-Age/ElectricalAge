package mods.eln.heatfurnace;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
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




public class HeatFurnaceGuiDraw extends GuiContainerEln {

	
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
    	

    	externalControl = newGuiButton(6,6,100, "");
    	takeFuel = newGuiButton(6,6+20+1,100, "");
    	
    	vuMeterGain = newGuiVerticalTrackBar(167-20,8,20,69);
    	vuMeterGain.setStepIdMax((int) (0.9f/0.01f));
    	vuMeterGain.setEnable(true);
    	vuMeterGain.setRange(0.1f,1.0f);
    	
    	syncVumeterGain();
   	
    	vuMeterHeat = newGuiVerticalTrackBarHeat(167-20 - 20 - 5,8,20,69);
    	vuMeterHeat.setStepIdMax(98);
    	vuMeterHeat.setEnable(true);
    	vuMeterHeat.setRange(0.0f,980.0f);
    	vuMeterHeat.setComment(0,"Temperature gauge");
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
       

    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	if(render.controleExternal)
    		externalControl.displayString = "Internal control";
    	else
    		externalControl.displayString = "External control";
    	//externalControl.displayString = "External control : " + render.controleExternal;
    	if(render.takeFuel)
    		takeFuel.displayString = "Take fuel";
    	else
    		takeFuel.displayString = "Decline fuel";
    	takeFuel.enabled = !render.controleExternal;
    	
    	
        vuMeterGain.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null && render.controleExternal == false);            
        if(render.gainSyncNew) syncVumeterGain();
        
        vuMeterHeat.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) != null && render.controleExternal == false);
        if(render.temperatureTargetSyncNew) syncVumeterHeat();
        
        vuMeterHeat.temperatureHit = (float) render.temperature;
        //vuMeterHeat.setVisible(render.controleExternal == false);

        vuMeterHeat.setComment(new String[]{});
        vuMeterHeat.setComment(0,"Temperature gauge");
        vuMeterHeat.setComment(1,Utils.plotCelsius("Current", render.temperature));
        if(render.controleExternal == false)
        	vuMeterHeat.setComment(2,Utils.plotCelsius("Target", vuMeterHeat.getValue()));
        vuMeterGain.setComment(0,"Control gauge at " +(int)(vuMeterGain.getValue()*100) + "%");
    }
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
        if(object == externalControl)
        {
        	render.clientToogleControl();
        }
        else if(object == takeFuel)
        {
        	render.clientToogleTakeFuel();
        }    
        else if(vuMeterGain == object)
    	{
    		render.clientSetGain(vuMeterGain.getValue());
    	}
        else if(vuMeterHeat == object)
    	{
    		render.clientSetTemperatureTarget(vuMeterHeat.getValue());
    	}
    }
    
    
    
    @Override
    protected void postDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.postDraw(f, x, y);
        drawString( 27, 51+17 + 3, Utils.plotPower("Power", render.power));

    }



	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}
}
