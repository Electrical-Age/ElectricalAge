package mods.eln.sixnode.wirelesssignal.rx;

import net.minecraft.client.gui.GuiButton;
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
		channel.setComment(0, "Specify the channel");
		
		
		int x = 6;
		int y = 6+12+4;
		add(buttonBigger = new AggregatorBt(x,y,100,20,"Bigger",(byte) 0)); y += 20;
		add(buttonSmaller = new AggregatorBt(x,y,100,20,"Smaller",(byte) 1)); y += 20;
		add(buttonToogle = new AggregatorBt(x,y,100,20,"Event",(byte) 2)); y += 20;
		

	}
	
	@Override
	protected GuiHelper newHelper() {
		
		return new GuiHelper(this, 220+12, 12 + 4+24*3+12);
	}

	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		if(object == channel){
			render.clientSetString(WirelessSignalRxElement.setChannelId,channel.getText());
		}
		super.guiObjectEvent(object);
	}
	
}
