package mods.eln.electricalfurnace;

import java.awt.List;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.HelperStdContainerBig;
import mods.eln.gui.IGuiObject;
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


public class ElectricalFurnaceGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    ElectricalFurnaceRender render;
    GuiButton buttonGrounded;   
    GuiVerticalTrackBarHeat vuMeterTemperature;
    
    public ElectricalFurnaceGuiDraw(EntityPlayer player, IInventory inventory,ElectricalFurnaceRender render)
    {
        super(new ElectricalFurnaceContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }

    public void initGui()
    {
    	super.initGui();
    	
    	buttonGrounded = newGuiButton(7,57,60, "");
    	vuMeterTemperature = newGuiVerticalTrackBarHeat(167-20,8,20,69);
    	vuMeterTemperature.setStepIdMax(800/10);
    	vuMeterTemperature.setEnable(true);
    	vuMeterTemperature.setRange(0,800);
    	vuMeterTemperature.setComment(new String[]{
    			"temperature gauge"
    			});
    	syncVumeter();
    }
    
    public void syncVumeter()
    {
    	vuMeterTemperature.setValue(render.temperatureTargetSyncValue);
    	render.temperatureTargetSyncNew = false;
    }
    
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
    	if( render.getPowerOn())
    		buttonGrounded.displayString = "Turn OFF";
    	else
    		buttonGrounded.displayString = "Turn ON";
    	//buttonGrounded.displayString = "powerOn : " + render.getPowerOn();
    	
        if(render.temperatureTargetSyncNew) syncVumeter();
        vuMeterTemperature.temperatureHit = render.temperature;
        
        
        vuMeterTemperature.setComment(1,"Current " + Utils.plotValue(render.temperature, "C°"));
        vuMeterTemperature.setComment(2,"Target " + Utils.plotValue(vuMeterTemperature.getValue(), "C°"));
    }
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == buttonGrounded)
    	{
    		render.clientSetPowerOn(!render.getPowerOn());
    	}
    	else if(object == vuMeterTemperature)
    	{
    		render.clientSetTemperatureTarget(vuMeterTemperature.getValue());
    	}
    }
    

    
	 @Override
	protected void postDraw(float f, int x, int y) {
		// TODO Auto-generated method stub
		super.postDraw(f, x, y);
	    ((HelperStdContainer)helper).drawProcess(176/2 - 25 +  0,28, render.processState);

	    drawString( 8, 6,Utils.plotPower("Consummation", render.heatingCorpResistorP));
	}
	 


	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new HelperStdContainer(this);
	}
}
