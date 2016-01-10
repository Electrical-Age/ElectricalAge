package mods.eln.sixnode.electricalrelay;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

import static mods.eln.i18n.I18N.tr;

public class ElectricalRelayGui extends GuiScreenEln {

    GuiButton toggleDefaultOutput;
    ElectricalRelayRender render;

	public ElectricalRelayGui(EntityPlayer player,ElectricalRelayRender render) {
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();

		toggleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, tr("Toggle switch"));
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
    	if (object == toggleDefaultOutput) {
    		render.clientToogleDefaultOutput();
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if (render.defaultOutput)
			toggleDefaultOutput.displayString = tr("Normally closed");
		else
			toggleDefaultOutput.displayString = tr("Normally open");
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 128, 32);
	}
}
