package mods.eln.sixnode.wirelesssignal;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

public class WirelessSignalRxGui extends GuiScreenEln{

	GuiTextFieldEln channel;
	private WirelessSignalRxRender render;
	
	
	public WirelessSignalRxGui(WirelessSignalRxRender render) {
		this.render = render;
	}
	
	@Override
	public void initGui() {
		// TODO Auto-generated method stub
		super.initGui();
		channel = newGuiTextField(6, 6, 150);
		channel.setText(render.channel);
		channel.setComment(0, "Specify the channel");
	}
	
	@Override
	protected GuiHelper newHelper() {
		// TODO Auto-generated method stub
		return new GuiHelper(this, 150+12, 12+12);
	}

	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == channel){
			render.clientSetString(WirelessSignalRxElement.setChannelId,channel.getText());
		}
		super.guiObjectEvent(object);
	}
	
}
