package mods.eln.transparentnode.solarpannel;

import org.lwjgl.opengl.GL11;




import mods.eln.Translator;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementInventory;
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


public class SolarPannelGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    SolarPannelRender render;
 
    GuiVerticalTrackBar vuMeterTemperature;
    
    public SolarPannelGuiDraw(EntityPlayer player, IInventory inventory,SolarPannelRender render)
    {
        super(new SolarPannelContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	
    	vuMeterTemperature = newGuiVerticalTrackBar(176/2+12,8,20,69);
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
    
    

	@Override
	public void guiObjectEvent(IGuiObject object) {
		
		super.guiObjectEvent(object);
    	if(vuMeterTemperature == object)
    	{
    		render.clientSetPannelAlpha(vuMeterTemperature.getValue());
    	}
	}
	@Override
	protected void preDraw(float f, int x, int y) {
		
		super.preDraw(f, x, y);
        if(render.pannelAlphaSyncNew) syncVumeter();
        //vuMeterTemperature.temperatureHit = (float) (SolarPannelSlowProcess.getSolarAlpha(render.tileEntity.worldObj));
        vuMeterTemperature.setEnable(! render.hasTracker);
        int sunAlpha = ((int)(180/Math.PI * SolarPannelSlowProcess.getSolarAlpha(render.tileEntity.getWorldObj()))-90);
        
        vuMeterTemperature.setComment(0,Translator.translate("eln.core.tile.solarpanel.panelalpha")+": " + ((int)(180/Math.PI * vuMeterTemperature.getValue())-90) + "\u00B0");
        if(Math.abs(sunAlpha)>90)
        	vuMeterTemperature.setComment(1,Translator.translate("eln.core.tile.solarpanel.night"));
        else
        	vuMeterTemperature.setComment(1,Translator.translate("eln.core.tile.solarpanel.sunalpha")+": " + sunAlpha + "\u00B0");
	}
	@Override
	protected void postDraw(float f, int x, int y) {
		
		super.postDraw(f, x, y);
		//drawString(8, 6,"Alpha " + render.pannelAlphaSyncNew);
	}

	@Override
	protected GuiHelperContainer newHelper() {
		
		return new GuiHelperContainer(this, 176, 166,8,84);
	}


	
}
