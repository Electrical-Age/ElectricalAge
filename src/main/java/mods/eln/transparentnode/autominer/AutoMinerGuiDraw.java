package mods.eln.transparentnode.autominer;

import org.lwjgl.opengl.GL11;

 

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.GuiVerticalTrackBarHeat;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.transparentnode.autominer.AutoMinerSlowProcess.jobType;
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

public class AutoMinerGuiDraw extends GuiContainerEln {

    private TransparentNodeElementInventory inventory;
    AutoMinerRender render;

    public AutoMinerGuiDraw(EntityPlayer player, IInventory inventory, AutoMinerRender render) {
        super(new AutoMinerContainer(null, player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render; 
    }

    
    @Override
    protected void postDraw(float f, int x, int y) {
    	if(render.job == jobType.chestFull){
    		drawString( 8,7, "Chest missing on the"); 
    		drawString( 8,7+9, "back of the Auto Miner");
    	}
    	super.postDraw(f, x, y);
    }
    
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166+18*2-90, 8, 84-90+18*2);
	}
}
