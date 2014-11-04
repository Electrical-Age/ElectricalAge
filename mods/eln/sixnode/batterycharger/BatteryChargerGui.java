package mods.eln.sixnode.batterycharger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import mods.eln.Translator;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

public class BatteryChargerGui extends GuiContainerEln {

	public BatteryChargerGui(BatteryChargerRender render, EntityPlayer player, IInventory inventory){
		super(new BatteryChargerContainer(player, inventory));
		this.render = render;
	}

	private BatteryChargerRender render;
	
	GuiButtonEln powerOn;
	
	@Override
	public void initGui() {
		super.initGui();
		powerOn = newGuiButton(97 + 10, 6 + 17 - 10, 40, "");
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		
		if(render.powerOn) {
			powerOn.displayString = Translator.translate("eln.core.on");
		}
		else {
			powerOn.displayString = Translator.translate("eln.core.off");
		}
	}
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 176, 166 - 40, 8, 84 - 40);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == powerOn){
			render.clientSend(BatteryChargerElement.toogleCharge);
		}
		super.guiObjectEvent(object);
	}
}
