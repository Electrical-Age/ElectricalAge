package mods.eln.autominer;

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




public class AutoMinerGuiDraw extends GuiContainer {

	
    private TransparentNodeElementInventory inventory;
    AutoMinerRender render;
    GuiButton buttonGrounded;

    
    public AutoMinerGuiDraw(EntityPlayer player, IInventory inventory,AutoMinerRender render)
    {
        super(new AutoMinerContainer(null,player, inventory));
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
        }
 
    }
	
    @Override
    protected void mouseMovedOrUp(int par1, int par2, int par3) {
    	// TODO Auto-generated method stub
    	super.mouseMovedOrUp(par1, par2, par3);

    }
    
    
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
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
            
            
         }
}
