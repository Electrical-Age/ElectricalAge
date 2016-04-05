package mods.eln.sixnode.modbusrtu;

import mods.eln.gui.*;
import mods.eln.gui.IGuiObject.IGuiObjectObserver;
import mods.eln.wiki.GuiVerticalExtender;
import net.minecraft.entity.player.EntityPlayer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ModbusRtuGui extends GuiScreenEln {

    ModbusRtuRender render;

    GuiTextFieldEln station, name;
    GuiVerticalExtender extender;

    int extenderYStart;
    GuiButtonEln txAddButton;
    GuiButtonEln rxAddButton;

    HashMap<Integer, GuiTextFieldEln> uuidToRxName = new HashMap<Integer, GuiTextFieldEln>();

    GuiHelper helper;

    public ModbusRtuGui(EntityPlayer player, ModbusRtuRender render) {
		this.render = render;
	}

	@Override
	public void initGui() {
		super.initGui();

		int y = 6;

		render.rxTxChange = true;
		
		extenderYStart = y + 6;
	}

	int generateTxRd(int x, int y) {
		uuidToRxName.clear();
		float extenderPosition = 0;
		if (extender != null) {
			extenderPosition = extender.getSliderPosition();
			remove(extender);
		}
		
		y = 0;
		extender = new GuiVerticalExtender(6, 6, helper.xSize - 12, helper.ySize - 12, helper);
		add(extender);

		GuiLabel title = new GuiLabel(2, y, tr("Modbus RTU"));
		extender.add(title);
		
		y += 10;
		
		station = new GuiTextFieldEln(fontRendererObj, 2, y, 30,helper); 
		if (render.station != -1)
			station.setText(render.station);
		station.setObserver(this);
		station.setComment(0, tr("Station ID"));
		extender.add(station);
		
		name = new GuiTextFieldEln(fontRendererObj,2+station.getWidth() + 12, y, 101,helper);  y+= name.getHeight();
		name.setText(render.name);
		name.setObserver(this);
		name.setComment(0, tr("Station name"));
		extender.add(name);
		
		y += 5;
		//y = 0;
				
		{
			x = 2;
			GuiLabel txLabel = new GuiLabel(x, y + 6, tr("Wireless TX"));
			extender.add(txLabel);
			
			x += 65;
			txAddButton = new GuiButtonEln(x, y, 40, 20, tr("Add"));
			txAddButton.setObserver(this);
			extender.add(txAddButton);
			
			y += 20;
		}

		List<WirelessTxStatus> tempListTx = new ArrayList<WirelessTxStatus>(render.wirelessTxStatusList.values());

		while (tempListTx.size() != 0) {
			int smaller = Integer.MAX_VALUE;
			WirelessTxStatus best = null;
			for (WirelessTxStatus tx : tempListTx) {
				if (tx.uuid < smaller) {
					smaller = tx.uuid;
					best = tx;
				}
			}
			tempListTx.remove(best);
			WirelessTxStatus tx = best;
			
			y += 2;
			x = 2;
			GuiTextFieldEln txName = new GuiTextFieldEln(fontRendererObj, x, y + 4, 90, helper);
			txName.setText(tx.name);
			txName.setComment(0, tr("Channel name"));
			extender.add(txName);
			
			x += txName.getWidth() + 12;

			GuiTextFieldEln txId = new GuiTextFieldEln(fontRendererObj, x , y + 4, 40, helper);
			if (tx.id != -1)
				txId.setText(tx.id);
			else
				txId.setText("");
			txId.setComment(0, tr("Modbus ID"));
			extender.add(txId);
			
			x += txId.getWidth() + 12;

			GuiButtonEln txDelete = new GuiButtonEln(x, y, 20, 20, "X");
			txDelete.setObserver(new WirelessTxDeleteListener(render, tx.uuid));
			extender.add(txDelete);
			
			WirelessTxStatusListener configListener = new WirelessTxStatusListener(render, tx.uuid, txName, txId);
			
			y += 20;
		}
		
		{
			x = 2;
			y += 6;
			GuiLabel rxLabel = new GuiLabel(x, y + 6, tr("Wireless RX"));
			extender.add(rxLabel);
			rxAddButton = new GuiButtonEln(x + 65, y, 40, 20, tr("Add"));
			rxAddButton.setObserver(this);
			extender.add(rxAddButton);

			y += 20;
		}
		
		List<WirelessRxStatus> tempListRx = new ArrayList<WirelessRxStatus>(render.wirelessRxStatusList.values());

        while (tempListRx.size() != 0) {
			int smaller = Integer.MAX_VALUE;
			WirelessRxStatus best = null;
			for (WirelessRxStatus rx : tempListRx) {
				if (rx.uuid < smaller){
					smaller = rx.uuid;
					best = rx;
				}
			}
			tempListRx.remove(best);
			WirelessRxStatus rx = best;
			
			y += 2;
			x = 2;
			GuiTextFieldEln rxName = new GuiTextFieldEln(fontRendererObj, x, y + 4, 90, helper);
			rxName.setText(rx.name);
			rxName.setComment(0, tr("Channel name"));
			extender.add(rxName);
			
			x += rxName.getWidth() + 12;

			GuiTextFieldEln rxId = new GuiTextFieldEln(fontRendererObj, x, y + 4, 40, helper);
			if (rx.id != -1)
				rxId.setText(rx.id);
			else
				rxId.setText("");
			rxId.setComment(0, tr("Modbus ID"));
			extender.add(rxId);
			
			x += rxId.getWidth() + 12;

			GuiButtonEln rxDelete = new GuiButtonEln(x, y, 20, 20, "X");
			rxDelete.setObserver(new WirelessRxDeleteListener(render, rx.uuid));
			extender.add(rxDelete);
			
			WirelessRxStatusListener configListener = new WirelessRxStatusListener(render, rx.uuid, rxName, rxId);
			
			uuidToRxName.put(rx.uuid, rxName);
			y += 20;
		}
		
		y += 6;
		extender.setSliderPosition(extenderPosition);
		return y;
	}
	
	@Override
	public void guiObjectEvent(IGuiObject object) {
		super.guiObjectEvent(object);
    	if (object == station) {
    		try {
    			render.clientSetInt(ModbusRtuElement.setStation, Integer.parseInt(station.getText()));
			} catch (NumberFormatException e) {
				// TODO: handle exception
			}
    		
    	} else if (object == name) {
    		render.clientSetString(ModbusRtuElement.setName, name.getText());
    	} else if (object == txAddButton) {
    		render.clientSetString(ModbusRtuElement.serverTxAdd, "newTX");
    	} else if (object == rxAddButton) {
    		render.clientSetString(ModbusRtuElement.serverRxAdd, "newRX");
    	}
	}
	
	@Override
	protected void preDraw(float f, int x, int y) {
		super.preDraw(f, x, y);
		
		if (render.rxTxChange) {
			generateTxRd(0, extenderYStart);
			render.rxTxChange = false;
		}
		
		for (WirelessRxStatus rx: render.wirelessRxStatusList.values()) {
			GuiTextFieldEln name = uuidToRxName.get(rx.uuid);
			if (rx.connected)
				name.setComment(1, "\u00a72" + tr("Connected"));
			else
				name.setComment(1, "\u00a74" + tr("Not connected"));
		}
	}

	@Override
	protected GuiHelper newHelper() {
		return helper = new GuiHelper(this, 190, 200);
	}
	
	static class WirelessTxDeleteListener implements IGuiObjectObserver {
		private ModbusRtuRender render;
		private int uuid;

		public WirelessTxDeleteListener(ModbusRtuRender render, int uuid) {
			this.render = render;
			this.uuid = uuid;
		}

		@Override
		public void guiObjectEvent(IGuiObject object) {
	        try {
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		        DataOutputStream stream = new DataOutputStream(bos);   	
		
		        render.preparePacketForServer(stream);
				
		        stream.writeByte(ModbusRtuElement.serverTxDelete);
		        stream.writeInt(uuid);

				render.sendPacketToServer(bos);
			} catch (IOException e) {
				e.printStackTrace();
			} 			
		}
	}

	static class WirelessTxStatusListener implements IGuiObjectObserver {
		GuiTextFieldEln name,id;
		private ModbusRtuRender render;

        int uuid;

        public WirelessTxStatusListener(ModbusRtuRender render, int uuid, GuiTextFieldEln name, GuiTextFieldEln id) {
			this.uuid = uuid;
			this.id = id;
			this.name = name;
			this.render = render;
			
			id.setGuiObserver(this);
			name.setGuiObserver(this);
		}

		@Override
		public void guiObjectEvent(IGuiObject object) {
			String nameStr = name.getText();
			int idInt = -1;
			try {
				idInt = Integer.parseInt(id.getText());
			} catch (NumberFormatException e) {
				idInt = -1;
			}
			
	        try {
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		        DataOutputStream stream = new DataOutputStream(bos);   	
		
		        render.preparePacketForServer(stream);
				
		        stream.writeByte(ModbusRtuElement.serverTxConfig);
		        stream.writeInt(uuid);
				stream.writeUTF(nameStr);
				stream.writeInt(idInt);

				render.sendPacketToServer(bos);
			} catch (IOException e) {
				e.printStackTrace();
			}   
		}
	}

	static class WirelessRxDeleteListener implements IGuiObjectObserver {
		private ModbusRtuRender render;
		private int uuid;

		public WirelessRxDeleteListener(ModbusRtuRender render, int uuid) {
			this.render = render;
			this.uuid = uuid;
		}

		@Override
		public void guiObjectEvent(IGuiObject object) {
	        try {
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		        DataOutputStream stream = new DataOutputStream(bos);   	
		
		        render.preparePacketForServer(stream);
				
		        stream.writeByte(ModbusRtuElement.serverRxDelete);
		        stream.writeInt(uuid);

				render.sendPacketToServer(bos);
			} catch (IOException e) {
				e.printStackTrace();
			} 			
		}
	}

	static class WirelessRxStatusListener implements IGuiObjectObserver {
		GuiTextFieldEln name, id;
		private ModbusRtuRender render;

        int uuid;

        public WirelessRxStatusListener(ModbusRtuRender render, int uuid, GuiTextFieldEln name, GuiTextFieldEln id) {
			this.uuid = uuid;
			this.id = id;
			this.name = name;
			this.render = render;
			
			id.setGuiObserver(this);
			name.setGuiObserver(this);
		}

		@Override
		public void guiObjectEvent(IGuiObject object) {
			String nameStr = name.getText();
			int idInt = -1;
			try {
				idInt = Integer.parseInt(id.getText());
			} catch (NumberFormatException e) {
				idInt = -1;
			}
			
	        try {
		    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
		        DataOutputStream stream = new DataOutputStream(bos);   	
		
		        render.preparePacketForServer(stream);
				
		        stream.writeByte(ModbusRtuElement.serverRxConfig);
		        stream.writeInt(uuid);
				stream.writeUTF(nameStr);
				stream.writeInt(idInt);

				render.sendPacketToServer(bos);
			} catch (IOException e) {
				e.printStackTrace();
			}   
		}
	}
}
