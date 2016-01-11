package mods.eln.transparentnode.heatfurnace;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.HelperStdContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import static mods.eln.i18n.I18N.tr;

public class HeatFurnaceGuiDraw extends GuiContainerEln {
    
    private TransparentNodeElementInventory inventory;
    HeatFurnaceRender render;
    GuiButton externalControl, takeFuel;
    GuiVerticalTrackBar vuMeterGain;
    GuiVerticalTrackBarHeat vuMeterHeat;
    
    public HeatFurnaceGuiDraw(EntityPlayer player, IInventory inventory,HeatFurnaceRender render) {
        super(new HeatFurnaceContainer(null, player, inventory, render.descriptor));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
    }
    
    public void initGui() {
    	super.initGui();
    	
    	externalControl = newGuiButton(6, 6, 100, "");
    	takeFuel = newGuiButton(6, 6 + 20 + 4, 100, "");
    	
    	vuMeterGain = newGuiVerticalTrackBar(167 - 20 - 3, 8, 20, 69);
    	vuMeterGain.setStepIdMax((int) (0.9f / 0.01f));
    	vuMeterGain.setEnable(true);
    	vuMeterGain.setRange(0.1f, 1.0f);
    	
    	syncVumeterGain();
   	
    	vuMeterHeat = newGuiVerticalTrackBarHeat(167 - 20 - 20 - 5 - 6, 8, 20, 69);
    	vuMeterHeat.setStepIdMax(98);
    	vuMeterHeat.setEnable(true);
    	vuMeterHeat.setRange(0.0f, 980.0f);
		vuMeterHeat.setComment(0, tr("Temperature Gauge"));
		syncVumeterHeat();
    	
    	/*
    	GuiHelpText help = new GuiHelpText(0, 0, helper);
    	helper.add(help);
    	help.setComment(0, "Help me");
    	help.setComment(1, "Miaou");
*/
    }
    public void syncVumeterGain() {
    	vuMeterGain.setValue(render.gainSyncValue);
    	render.gainSyncNew = false;
    }
    
    public void syncVumeterHeat() {
    	vuMeterHeat.setValue(render.temperatureTargetSyncValue);
    	render.temperatureTargetSyncNew = false;
    }
    
    @Override
    protected void preDraw(float f, int x, int y) {
    	super.preDraw(f, x, y);
    	if (!render.controleExternal)
			externalControl.displayString = tr("Internal control");
		else
    		externalControl.displayString = tr("External control");
    	//externalControl.displayString = "External control : " + render.controleExternal;
    	if (render.takeFuel)
			takeFuel.displayString = tr("Take fuel");
		else
			takeFuel.displayString = tr("Decline fuel");
		takeFuel.enabled = !render.controleExternal;
    	
    	
        vuMeterGain.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null && !render.controleExternal);            
        if (render.gainSyncNew) syncVumeterGain();
        
        vuMeterHeat.setEnable(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) != null && !render.controleExternal);
        if (render.temperatureTargetSyncNew) syncVumeterHeat();
        
        vuMeterHeat.temperatureHit = (float) render.temperature;
        //vuMeterHeat.setVisible(render.controleExternal == false);

        vuMeterHeat.setComment(new String[]{});
		vuMeterHeat.setComment(0, tr("Temperature Gauge"));
		vuMeterHeat.setComment(1, tr("Current: %1$°C", Utils.plotValue(render.temperature)));
		if (!render.controleExternal)
        	vuMeterHeat.setComment(2, Utils.plotCelsius("Target:", vuMeterHeat.getValue()));
		vuMeterGain.setComment(0, tr("Control Gauge at %1$%", (int) (vuMeterGain.getValue()) * 100));

		vuMeterGain.setComment(1, tr("Power: %1$W", render.power));
	}
    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	super.guiObjectEvent(object);
        if (object == externalControl) {
        	render.clientToogleControl();
        } else if (object == takeFuel) {
        	render.clientToogleTakeFuel();
        } else if (vuMeterGain == object) {
    		render.clientSetGain(vuMeterGain.getValue());
    	} else if (vuMeterHeat == object) {
    		render.clientSetTemperatureTarget(vuMeterHeat.getValue());
    	}
    }
    
    @Override
    protected void postDraw(float f, int x, int y) {
    	super.postDraw(f, x, y);
        //drawString(27, 51 + 17 + 3, Utils.plotPower("Power", render.power));
    }

	@Override
	protected GuiHelperContainer newHelper() {
		return new HelperStdContainer(this);
	}
}
