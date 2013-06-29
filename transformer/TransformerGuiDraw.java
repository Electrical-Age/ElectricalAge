package mods.eln.transformer;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.IGuiObject;
import mods.eln.heatfurnace.HeatFurnaceContainer;
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


public class TransformerGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    TransformerRender render;
    GuiButton buttonGrounded;

    
    
    public TransformerGuiDraw(EntityPlayer player, IInventory inventory,TransformerRender render)
    {
        super(new TransformerContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    
    	buttonGrounded = newGuiButton(width/2,height/2,100, "");
    }
    

    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);
        buttonGrounded.displayString = "Grounded : " + render.grounded;

    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);
    	if(object == buttonGrounded)
    	{
    		//render.clientSetGrounded(!render.getGrounded());
    	}
    }

	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 176, 166, "transformer.png");
	}
    

    
    

}
