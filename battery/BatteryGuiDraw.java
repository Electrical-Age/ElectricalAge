package mods.eln.battery;

import org.lwjgl.opengl.GL11;



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


public class BatteryGuiDraw extends GuiContainer {

	
    private TransparentNodeElementInventory inventory;
    BatteryRender render;
    GuiButton buttonGrounded;
    public BatteryGuiDraw(EntityPlayer player, IInventory inventory,BatteryRender render)
    {
        super(new BatteryContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    	buttonGrounded = new GuiButton(1, width/2,height/2,100,20, "");
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
        	render.clientSetGrounded(!render.grounded);
        }
    }
	
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
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
            fontRenderer.drawString(Utils.plotEnergy("Energy", render.energy) +  Utils.plotEnergy("/", render.descriptor.electricalStdEnergy * render.life)    
            						+ "-> " + str, 8, 6, 4210752);
            fontRenderer.drawString("Life : " + String.format("%3.1f",render.life*100.0) + "%", 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2,
                    int par3) {
       Utils.bindTextureByName("/gui/trap.png");
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      
            int x = (width - xSize) / 2;
            int y = (height - ySize) / 2;
            this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }
}
