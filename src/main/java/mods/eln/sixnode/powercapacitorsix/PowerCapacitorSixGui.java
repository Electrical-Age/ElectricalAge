package mods.eln.sixnode.powercapacitorsix;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeElementInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class PowerCapacitorSixGui extends GuiContainerEln {

    private SixNodeElementInventory inventory;
    PowerCapacitorSixRender render;

    public PowerCapacitorSixGui(EntityPlayer player, IInventory inventory, PowerCapacitorSixRender render) {
        super(new PowerCapacitorSixContainer(player, inventory));
        this.inventory = (SixNodeElementInventory) inventory;
        this.render = render;   
    }
    
    public void initGui() {
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
    	helper.drawString(8, 8, 0xFF000000, "Capacity : "  + Utils.plotValue(render.descriptor.getCValue(render.inventory), "F"));
    	helper.drawString(8, 8 + 8 + 1, 0xFF000000, "Nominal voltage : "  + Utils.plotValue(render.descriptor.getUNominalValue(render.inventory), "V"));
		super.postDraw(f, x, y);
	}

	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54);
	}
}
