package mods.eln.sixnode.batterycharger;

import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.IGuiObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class BatteryChargerGui extends GuiContainerEln {

    private BatteryChargerRender render;

    GuiButtonEln powerOn;

	public BatteryChargerGui(BatteryChargerRender render, EntityPlayer player, IInventory inventory) {
		super(new BatteryChargerContainer(player, inventory));
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();
		powerOn = newGuiButton(97 + 10, 6 + 17 - 10, 40, "");
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		
		if (render.powerOn) {
			powerOn.displayString = "Is ON";
		} else {
			powerOn.displayString = "Is OFF";
		}
	}
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166 - 40, 8, 84 - 40);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if (object == powerOn){
			render.clientSend(BatteryChargerElement.toogleCharge);
		}
		super.guiObjectEvent(object);
	}
}
