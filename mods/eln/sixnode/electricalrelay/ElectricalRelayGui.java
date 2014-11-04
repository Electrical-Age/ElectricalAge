package mods.eln.sixnode.electricalrelay;

import mods.eln.Translator;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.IGuiObject;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalRelayGui extends GuiScreenEln {

	public ElectricalRelayGui(EntityPlayer player,ElectricalRelayRender render) {
		this.render = render;
	}

	GuiButton toogleDefaultOutput;
	ElectricalRelayRender render;

	@Override
	public void initGui() {
		super.initGui();

		toogleDefaultOutput = newGuiButton(6, 32 / 2 - 10, 115, "toogle switch");
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
    	if(object == toogleDefaultOutput) {
    		render.clientToogleDefaultOutput();
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		if(render.defaultOutput)
			toogleDefaultOutput.displayString = Translator.translate("eln.core.tile.relay.state1");
		else
			toogleDefaultOutput.displayString = Translator.translate("eln.core.tile.relay.state0");
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 128, 32);
	}
}
