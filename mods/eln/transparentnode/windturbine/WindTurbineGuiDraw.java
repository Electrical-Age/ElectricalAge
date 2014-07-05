package mods.eln.transparentnode.windturbine;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
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


public class WindTurbineGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    WindTurbineRender render;

    
    public WindTurbineGuiDraw(EntityPlayer player, IInventory inventory,WindTurbineRender render)
    {
        super(new WindTurbineContainer(null,player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	

   
    }

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelperContainer(this, 176, 166,8,84);
	}
	
}
