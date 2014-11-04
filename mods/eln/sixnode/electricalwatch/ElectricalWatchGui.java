package mods.eln.sixnode.electricalwatch;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class ElectricalWatchGui extends GuiContainerEln {

	public ElectricalWatchGui(EntityPlayer player, IInventory inventory, ElectricalWatchRender render) {
		super(new ElectricalWatchContainer(player, inventory));
		this.render = render;
	}

	ElectricalWatchRender render;

	@Override
	public void initGui() {
		super.initGui();
	}
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166 - 52, 8, 84 - 52);
	}
}
