package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

public class WirelessSignalTxGui extends GuiScreenEln{

	GuiTextFieldEln channel;
	private WirelessSignalTxRender render;
	
	
	public WirelessSignalTxGui(WirelessSignalTxRender render) {
		this.render = render;
	}
	
	@Override
	public void initGui() {
		
		super.initGui();
		channel = newGuiTextField(6, 6, 150);
		channel.setText(render.channel);
		channel.setComment(0, "Specify the channel");
	}
	
	@Override
	protected GuiHelper newHelper() {
		
		return new GuiHelper(this, 150+12, 12+12);
	}

	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == channel){
			render.clientSetString(WirelessSignalTxElement.setChannelId,channel.getText());
		}
		super.guiObjectEvent(object);
	}
	
}
