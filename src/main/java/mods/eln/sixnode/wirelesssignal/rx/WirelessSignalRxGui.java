package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

public class WirelessSignalRxGui extends GuiScreenEln {

	GuiTextFieldEln channel;
	private WirelessSignalRxRender render;
	
	AggregatorBt buttonBigger, buttonSmaller, buttonToogle;
	
	class AggregatorBt extends GuiButtonEln {
		byte id;
        
		public AggregatorBt(int x, int y, int width, int height, String str, byte id) {
			super(x, y, width, height, str);
			this.id = id;
		}

		@Override
		public void onMouseClicked() {
			render.clientSetByte(WirelessSignalRxElement.setSelectedAggregator, id);
			super.onMouseClicked();
		}

		@Override
		public void idraw(int x, int y, float f) {
			this.enabled = render.selectedAggregator != id;
			super.idraw(x, y, f);
		}
	}
	
	public WirelessSignalRxGui(WirelessSignalRxRender render) {
		this.render = render;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		channel = newGuiTextField(6, 6, 220);
		channel.setText(render.channel);
		channel.setComment(0, "Specify the channel");
		
		int w = 72;
		int x = 6;
		int y = 6 + 12 + 4;
		add(buttonBigger = new AggregatorBt(x, y, w, 20, "Biggest", (byte) 0)); x += 2 + w;
		add(buttonSmaller = new AggregatorBt(x, y, w, 20, "Smallest", (byte) 1)); x += 2 + w;
		add(buttonToogle = new AggregatorBt(x, y, w, 20, "Toggle", (byte) 2)); x += 2 + w;

		buttonBigger.setHelper(helper);
		buttonBigger.setComment(0, "Gets the biggest value");
		buttonBigger.setComment(1, "emitted on the channel.");
		
		buttonSmaller.setHelper(helper);
		buttonSmaller.setComment(0, "Gets the smallest value");
		buttonSmaller.setComment(1, "emitted on the channel.");
		
		buttonToogle.setHelper(helper);
		buttonToogle.setComment(0, "Toggle the output each time");
		buttonToogle.setComment(1, "an emitter's value rises.");
		buttonToogle.setComment(2, "Very useful to connect multiple");
		buttonToogle.setComment(3, "buttons to control a light in your house.");
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 220 + 12, 12 + 1 + 24 * 1 + 12);
	}

	@Override
	protected void preDraw(float f, int x, int y) {
		if (render.connection)
			channel.setComment(1, "\u00a72Connected");
		else
			channel.setComment(1, "\u00a74Unconnected");

		super.preDraw(f, x, y);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if (object == channel) {
			render.clientSetString(WirelessSignalRxElement.setChannelId, channel.getText());
		}
		super.guiObjectEvent(object);
	}
}
