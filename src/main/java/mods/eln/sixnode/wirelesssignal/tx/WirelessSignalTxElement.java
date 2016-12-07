package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WirelessSignalTxElement extends SixNodeElement implements IWirelessSignalTx {
    
	public static final HashMap<String, ArrayList<IWirelessSignalTx>> channelMap = new HashMap<String, ArrayList<IWirelessSignalTx>>(); 
	
	NbtElectricalGateInput inputGate = new NbtElectricalGateInput("inputGate");

	WirelessSignalTxDescriptor descriptor;
    
	public String channel = "Default channel";

	private LightningGlitchProcess lightningGlitchProcess;

    public static final byte setChannelId = 1;

    public WirelessSignalTxElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(inputGate);
		slowProcessList.add(lightningGlitchProcess = new LightningGlitchProcess(getCoordonate()));
		this.descriptor = (WirelessSignalTxDescriptor) descriptor;
		channelRegister(this);
	}
    
	static public class LightningGlitchProcess implements IProcess {
        double range = 64;
        Coordonate c;
        double glichedTimer = 0;
        double glichedStrangth = 0;
        final double glitchLength = 6;

        public double glitchOffset = 0;
        
		public LightningGlitchProcess(Coordonate c) {
			this.c = c;
		}
		
		@Override
		public void process(double time) {
			if (glichedTimer > 0)
				glichedTimer -= time/* * Utils.rand(0.2, 1.8)*/;
			
			double strangth = range - Eln.instance.serverEventListener.getLightningClosestTo(c);
			if (strangth > 0 && glichedTimer <= 0){
				glichedTimer = glitchLength;
				glichedStrangth = (strangth) / range;
				glichedStrangth = Math.pow(glichedStrangth, 1.4);
			}
			
			if (glichedTimer > 0) {
				double phase = glitchLength - glichedTimer;
				glitchOffset = Math.sin(phase * Math.PI * 2 * 3) * glichedStrangth * Math.pow(glichedTimer / glitchLength, 4);
			} else {
				glitchOffset = 0;
			}
		}
	}
    
	public static void channelRegister(IWirelessSignalTx tx) {
		String channel = tx.getChannel();
		ArrayList<IWirelessSignalTx> list = channelMap.get(channel);
		if (list == null) 
			channelMap.put(channel, list =  new ArrayList<IWirelessSignalTx>());
		list.add(tx);
	}
	
	public static void channelRemove(IWirelessSignalTx tx) {
		String channel = tx.getChannel();
		ArrayList<IWirelessSignalTx> list = channelMap.get(channel);
		if (list == null) return;
		list.remove(tx);
		if (list.isEmpty())
			channelMap.remove(channel);
	}
    
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return inputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return NodeBase.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return inputGate.plot("Input gate");
	}

	@Nullable
	@Override
	public Map<String, String> getWaila() {
		Map<String, String> info = new HashMap<String, String>();
		info.put(I18N.tr("Channel"), channel);
		info.put(I18N.tr("Input voltage"), Utils.plotVolt("", inputGate.getU()));
		return info;
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;	
		}
		return false;
	}
    
	@Override
	public void destroy(EntityPlayerMP entityPlayer) {
		unregister();
		super.destroy(entityPlayer);
	}
    
	@Override
	public void unload() {
		super.unload();
		unregister();
	}
	
	void unregister() {
		channelRemove(this);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setString("channel", channel);
	}
    
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		channelRemove(this);
		
		super.readFromNBT(nbt);
		channel = nbt.getString("channel");
		
		channelRegister(this);
	}

	@Override
	public Coordonate getCoordonate() {
		return sixNode.coordonate;
	}

	@Override
	public int getRange() {
		return descriptor.range;
	}

	@Override
	public String getChannel() {
		return channel;
	}
    
	@Override
	public double getValue() {
		return inputGate.getNormalized() + lightningGlitchProcess.glitchOffset;
	}
    
	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		
		try {
			switch (stream.readByte()) {
                case setChannelId:
                    channelRemove(this);
                    channel = stream.readUTF();
                    needPublish();
                    channelRegister(this);
                    break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeUTF(channel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
