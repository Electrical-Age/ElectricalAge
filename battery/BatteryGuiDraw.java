package mods.eln.battery;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
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


public class BatteryGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    BatteryRender render;
    GuiButtonEln buttonGrounded;
    
    public BatteryGuiDraw(EntityPlayer player, IInventory inventory,BatteryRender render)
    {
        super(new BatteryContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	buttonGrounded = newGuiButton(8,24,100, "");
    }
    

    
    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(buttonGrounded == object)
    	{
    		render.clientSetGrounded(!render.grounded);
    	}
    }
	
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {

    }
    
    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
        buttonGrounded.displayString = "Grounded : " + render.grounded;

    }


    @Override
    protected void postDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.postDraw(f, x, y);
    	String str = "";
    	double i = render.current;
    	double p = render.current*(render.voltagePositive-render.voltageNegative);
    	if(p > 0)
    	{
    		str = Utils.plotTime("Discharge time : ", render.energy/p); 
    	}
    	else
    	{
    		str = Utils.plotTime("Charge time : ", -render.energy/p); 
    	}
        drawString(8, 16,
        		Utils.plotEnergy("Energy", render.energy) +  Utils.plotEnergy("/", render.descriptor.electricalStdEnergy * render.life) + "-> " + str);
        drawString(8, 8 ,"Life : " + String.format("%3.1f",render.life*100.0) + "%");
    }

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 166,8,84, "battery.png");
	}
	

}
