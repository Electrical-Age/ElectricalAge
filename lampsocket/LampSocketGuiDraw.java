package mods.eln.lampsocket;

import org.lwjgl.opengl.GL11;



import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeElementInventory;
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
/*
public class MachineGuiClientExample extends GuiScreen {
	MachineBlockEntity tileEntity;
    public MachineGuiClientExample (InventoryPlayer inventoryPlayer,	NodeBlockEntity tileEntity) {
	    //the container is instanciated and passed to the superclass for handling
	    super();
	    this.tileEntity = (MachineBlockEntity) tileEntity;
	}

}
*/

public class LampSocketGuiDraw extends GuiContainer {

	
    private SixNodeElementInventory inventory;
    LampSocketRender lampRender;
    GuiButton buttonGrounded;
    public LampSocketGuiDraw(EntityPlayer player, IInventory inventory,LampSocketRender lampRender)
    {
        super(new LampSocketContainer(player, inventory,lampRender.lampSocketDescriptor));
        this.inventory = (SixNodeElementInventory) inventory;
        this.lampRender = lampRender;
        
      
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
        buttonGrounded.displayString = "Grounded : " + lampRender.grounded;
        this.buttonGrounded.drawButton(Minecraft.getMinecraft(), par1, par2);
    }
    
    protected void mouseClicked(int x, int y, int par3)
    {
        super.mouseClicked(x, y, par3);   
        
        if(this.buttonGrounded.mousePressed(Minecraft.getMinecraft(), x, y))
        {
        	System.out.println("miaou");
        	lampRender.clientSetGrounded(!lampRender.getGrounded());
        }
    }
	
    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
            fontRenderer.drawString("Tiny", 8, 6, 4210752);
            fontRenderer.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2,
                    int par3) {
    	Utils.bindTextureByName("/gui/trap.png");
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
  //          this.mc.renderEngine.bindTexture(texture);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }
}
