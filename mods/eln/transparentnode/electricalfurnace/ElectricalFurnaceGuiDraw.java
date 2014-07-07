package mods.eln.transparentnode.electricalfurnace;

import java.awt.List;
import java.util.ArrayList;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.GuiVerticalVoltageSupplyBar;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.HelperStdContainerBig;
import mods.eln.gui.IGuiObject;
import mods.eln.item.HeatingCorpElement;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.PhysicalConstant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.StatCollector;

public class ElectricalFurnaceGuiDraw extends GuiContainerEln {

    private TransparentNodeElementInventory inventory;
    ElectricalFurnaceRender render;
    GuiButton buttonGrounded, autoShutDown;   
    GuiVerticalTrackBarHeat vuMeterTemperature;
    
    GuiVerticalVoltageSupplyBar supplyBar;
    
    public ElectricalFurnaceGuiDraw(EntityPlayer player, IInventory inventory, ElectricalFurnaceRender render) {
        super(new ElectricalFurnaceContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }

    public void initGui() {
    	super.initGui();
    	autoShutDown = newGuiButton(6, 6, 99, "");
    	buttonGrounded = newGuiButton(6 + 10 * 0, 6 + 20 + 4, 60 - 20, "");
    	vuMeterTemperature = newGuiVerticalTrackBarHeat(167 - 20 - 20 - 8 - 4, 8, 20, 69);
    	vuMeterTemperature.setStepIdMax(800 / 10);
    	vuMeterTemperature.setEnable(true);
    	vuMeterTemperature.setRange(0, 800);
    	vuMeterTemperature.setComment(new String[]{"Temperature Gauge"});
    	syncVumeter();
    	
    	supplyBar = new GuiVerticalVoltageSupplyBar(167 - 20 - 2, 8, 20, 69, helper);
    	add(supplyBar);
    }
    
    public void syncVumeter() {
    	vuMeterTemperature.setValue(render.temperatureTargetSyncValue);
    	render.temperatureTargetSyncNew = false;
    }
    
    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if(render.getPowerOn())
    		buttonGrounded.displayString = "Is ON";
    	else
    		buttonGrounded.displayString = "Is OFF";
    	
    	if(render.autoShutDown) {
    		buttonGrounded.enabled = false;
    		autoShutDown.displayString = "Auto Shutdown";
    	}
    	else {
    		autoShutDown.displayString = "Manual Shutdown";
    		buttonGrounded.enabled = true;
    	}
    	
        if(render.temperatureTargetSyncNew) syncVumeter();
        vuMeterTemperature.temperatureHit = render.temperature;
        
        vuMeterTemperature.setComment(1, "Current: " + Utils.plotValue(render.temperature + PhysicalConstant.Tamb, "\u00B0C"));
        vuMeterTemperature.setComment(2, "Target: " + Utils.plotValue(vuMeterTemperature.getValue() + PhysicalConstant.Tamb, "\u00B0C"));
    }
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
    	if(object == buttonGrounded) {
    		render.clientSetPowerOn(!render.getPowerOn());
    	}
    	else if(object == autoShutDown) {
    		render.clientSendId(ElectricalFurnaceElement.unserializeAutoShutDownId);
    	}
    	else if(object == vuMeterTemperature) {
    		render.clientSetTemperatureTarget(vuMeterTemperature.getValue());
    	}
    }
    
	@Override
	protected void postDraw(float f, int x, int y) {
		super.postDraw(f, x, y);
	    ((HelperStdContainer)helper).drawProcess(40, 57, render.processState);

	    //drawString(8, 6, Utils.plotPower("Consummation", render.heatingCorpResistorP));
	    
	    ItemStack stack = render.inventory.getStackInSlot(ElectricalFurnaceElement.heatingCorpSlotId);
	    if(stack == null) {
	    	supplyBar.setEnabled(false);
	    }
	    else {
	    	supplyBar.setEnabled(true);
	    	HeatingCorpElement desc = (HeatingCorpElement) HeatingCorpElement.getDescriptor(stack);
	    	supplyBar.setNominalU((float) desc.electricalNominalU);
	    }
    	supplyBar.setVoltage(render.voltage);
    	supplyBar.setPower(render.heatingCorpResistorP);
	}
	 
	@Override
	protected GuiHelperContainer newHelper() {
		return new HelperStdContainer(this);
	}
}
