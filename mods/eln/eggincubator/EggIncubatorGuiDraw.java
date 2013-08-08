package mods.eln.eggincubator;

import org.lwjgl.opengl.GL11;



import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiVerticalTrackBar;
import mods.eln.gui.HelperStdContainer;
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


public class EggIncubatorGuiDraw extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    EggIncubatorRender render;

    
    public EggIncubatorGuiDraw(EntityPlayer player, IInventory inventory,EggIncubatorRender render)
    {
        super(new EggIncubatorContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;
        
      
    }
    
    public void initGui()
    {
    	super.initGui();
    	
    

    }
    

    @Override
    protected void preDraw(float f, int x, int y) {
    	// TODO Auto-generated method stub
    	super.preDraw(f, x, y);

    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
    	// TODO Auto-generated method stub
    	super.guiObjectEvent(object);

    }

	@Override
	protected GuiHelperContainer newHelper() {
		// TODO Auto-generated method stub
			return new GuiHelperContainer(this, 176, 194,8,84 + 194 - 166);
		//return new HelperStdContainer(this);
	}
    

    
    

}
