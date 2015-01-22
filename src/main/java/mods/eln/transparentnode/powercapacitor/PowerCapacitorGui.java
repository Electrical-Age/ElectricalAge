package mods.eln.transparentnode.powercapacitor;

import org.lwjgl.opengl.GL11;



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
import mods.eln.sim.mna.component.Inductor;
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


public class PowerCapacitorGui extends GuiContainerEln {

	
    private TransparentNodeElementInventory inventory;
    PowerCapacitorRender render;
    

    
    public PowerCapacitorGui(EntityPlayer player, IInventory inventory,PowerCapacitorRender render)
    {
        super(new PowerCapacitorContainer(player, inventory));
        this.inventory = (TransparentNodeElementInventory) inventory;
        this.render = render;   
    }
    
    public void initGui()
    {
    	super.initGui();
    }
    

    

	@Override
	public void guiObjectEvent(IGuiObject object) {
		
		super.guiObjectEvent(object);

	}
	@Override
	protected void preDraw(float f, int x, int y) {

		super.preDraw(f, x, y);
	}
	
	@Override
	protected void postDraw(float f, int x, int y) {
    	helper.drawString(8, 8, 0xFF000000, "Inductance : "  + Utils.plotValue(render.descriptor.getCValue(render.inventory),"F"));
    	helper.drawString(8, 8+8+1, 0xFF000000, "Nominal voltage : "  + Utils.plotValue(render.descriptor.getUNominalValue(render.inventory),"V"));
		super.postDraw(f, x, y);
	}

	@Override
	protected GuiHelperContainer newHelper() {
		
		return new GuiHelperContainer(this, 176, 166-54,8,84-54);
	}


	
}
