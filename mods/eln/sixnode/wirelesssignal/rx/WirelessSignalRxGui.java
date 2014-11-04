package mods.eln.sixnode.wirelesssignal.rx;

import net.minecraft.client.gui.GuiButton;
import mods.eln.Translator;
import mods.eln.gui.GuiButtonEln;
import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;

public class WirelessSignalRxGui extends GuiScreenEln{

	GuiTextFieldEln channel;
	private WirelessSignalRxRender render;
	
	AggregatorBt buttonBigger,buttonSmaller,buttonToogle;
	
	class AggregatorBt extends GuiButtonEln{
		byte id;
		public AggregatorBt(int x, int y, int width, int height, String str,byte id) {
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
		channel.setComment(0, Translator.translate("eln.core.tile.wireless.specifychannel"));
		
		int w = 72;
		int x = 6;
		int y = 6+12+4;
		add(buttonBigger = new AggregatorBt(x,y,w,20,Translator.translate("eln.core.tile.wirelessreceiver.btnb.name"),(byte) 0)); x += 2+w;
		add(buttonSmaller = new AggregatorBt(x,y,w,20,Translator.translate("eln.core.tile.wirelessreceiver.btns.name"),(byte) 1)); x += 2+w;
		add(buttonToogle = new AggregatorBt(x,y,w,20,Translator.translate("eln.core.tile.wirelessreceiver.btnt.name"),(byte) 2)); x += 2+w;
		

		buttonBigger.setHelper(helper);
		buttonBigger.setComment(0, Translator.translate("eln.core.tile.wirelessreceiver.btnb.hint0"));
		buttonBigger.setComment(1, Translator.translate("eln.core.tile.wirelessreceiver.btnb.hint1"));
		
		buttonSmaller.setHelper(helper);
		buttonSmaller.setComment(0, Translator.translate("eln.core.tile.wirelessreceiver.btns.hint0"));
		buttonSmaller.setComment(1, Translator.translate("eln.core.tile.wirelessreceiver.btns.hint1"));
		
		buttonToogle.setHelper(helper);
		buttonToogle.setComment(0, Translator.translate("eln.core.tile.wirelessreceiver.btnt.hint0"));
		buttonToogle.setComment(1, Translator.translate("eln.core.tile.wirelessreceiver.btnt.hint1"));
		buttonToogle.setComment(2, Translator.translate("eln.core.tile.wirelessreceiver.btnt.hint2"));
		buttonToogle.setComment(3, Translator.translate("eln.core.tile.wirelessreceiver.btnt.hint3"));
	}
	
	@Override
	protected GuiHelper newHelper() {
		return new GuiHelper(this, 220+12, 12 + 1+24*1+12);
	}

	
	@Override
	protected void preDraw(float f, int x, int y) {
		if(render.connection)
			channel.setComment(1, Translator.translate("eln.core.tile.wirelesstransmitter.conn"));
		else
			channel.setComment(1, Translator.translate("eln.core.tile.wirelesstransmitter.unconn"));


		super.preDraw(f, x, y);
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == channel){
			render.clientSetString(WirelessSignalRxElement.setChannelId,channel.getText());
		}
		super.guiObjectEvent(object);
	}
	
}
