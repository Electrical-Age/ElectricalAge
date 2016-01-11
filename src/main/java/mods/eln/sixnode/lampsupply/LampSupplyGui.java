package mods.eln.sixnode.lampsupply;

import mods.eln.gui.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static mods.eln.i18n.I18N.tr;

public class LampSupplyGui extends GuiContainerEln {


    private LampSupplyRender render;

    private HashMap<Object,Integer> powerMap = new HashMap<Object, Integer>();
    private HashMap<Object,Integer> wirelessMap = new HashMap<Object, Integer>();

	public LampSupplyGui(LampSupplyRender render, EntityPlayer player, IInventory inventory){
		super(new LampSupplyContainer(player, inventory));
		this.render = render;
	}

    class AggregatorBt extends GuiButtonEln {
        byte id;
        int channel;
        public AggregatorBt(int x, int y, int width, int height, String str,int channel, byte id) {
            super(x, y, width, height, str);
            this.id = id;
            this.channel = channel;
        }

        @Override
        public void onMouseClicked() {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);

                stream.writeByte(LampSupplyElement.setSelectedAggregator);
                stream.writeByte(channel);
                stream.writeByte(id);

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }
            super.onMouseClicked();
        }

        @Override
        public void idraw(int x, int y, float f) {
            this.enabled = render.entries.get(channel).aggregator != id;
            super.idraw(x, y, f);
        }
    }

	@Override
	public void initGui() {
		super.initGui();
        int y = 6;

        int x;
        for(int id = 0;id < render.descriptor.channelCount;id++) {
            x = 6;

            LampSupplyElement.Entry e = render.entries.get(id);
            GuiTextFieldEln powerChannel = newGuiTextField( x, y, 101); x += powerChannel.getWidth() + 12;
            powerChannel.setText(e.powerChannel);
            powerChannel.setComment(0, tr("Power channel name"));
            powerMap.put(powerChannel,id);

            GuiTextFieldEln wirelessChannel = newGuiTextField( x , y, 101); x += wirelessChannel.getWidth() + 12;
            wirelessChannel.setText(e.wirelessChannel);
            wirelessChannel.setComment(0, tr("Wireless channel name"));
            wirelessMap.put(wirelessChannel,id);
            y += wirelessChannel.getHeight() + 2;
            x = 6;
            int w = 68;
            AggregatorBt buttonBigger, buttonSmaller, buttonToogle;

            add(buttonBigger = new AggregatorBt(x, y, w, 20, tr("Biggest"),id, (byte) 0)); x += 2 + w;
            add(buttonSmaller = new AggregatorBt(x, y, w, 20, tr("Smallest"),id, (byte) 1)); x += 2 + w;
            add(buttonToogle = new AggregatorBt(x, y, w, 20, tr("Toggle"),id, (byte) 2)); x += 2 + w;

            buttonBigger.setHelper(helper);
            int lineNumber = 0;
            for (String line: tr("Uses the biggest\nvalue on the channel.").split("\n"))
                buttonBigger.setComment(lineNumber++, line);

            buttonSmaller.setHelper(helper);
            lineNumber = 0;
            for (String line: tr("Uses the smallest\nvalue on the channel.").split("\n"))
                buttonSmaller.setComment(lineNumber++, line);

            buttonToogle.setHelper(helper);
            lineNumber = 0;
            for (String line: tr("Toggles the output each time\nan emitter's value rises.\nUseful to allow multiple buttons\nto control the same light.").split("\n"))
                buttonToogle.setComment(lineNumber++, line);
            y += buttonToogle.height + 6;
        }
	}
	
	@Override
	protected GuiHelperContainer newHelper() {
		return new GuiHelperContainer(this, 220, 205, 8, 125);
	}

	@Override
	public void guiObjectEvent(IGuiObject object) {
		if (powerMap.containsKey(object)) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);
                stream.writeByte(LampSupplyElement.setPowerName);
                stream.writeByte(powerMap.get(object));
                stream.writeUTF(((GuiTextFieldEln)object).getText());

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
        if (wirelessMap.containsKey(object)) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream stream = new DataOutputStream(bos);

                render.preparePacketForServer(stream);
                stream.writeByte(LampSupplyElement.setWirelessName);
                stream.writeByte(wirelessMap.get(object));
                stream.writeUTF(((GuiTextFieldEln)object).getText());

                render.sendPacketToServer(bos);
            } catch (IOException e) {

                e.printStackTrace();
            }

        }
		super.guiObjectEvent(object);
	}
}
