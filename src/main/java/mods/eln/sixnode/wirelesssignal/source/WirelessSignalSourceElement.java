package mods.eln.sixnode.wirelesssignal.source;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement.LightningGlitchProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

public class WirelessSignalSourceElement extends SixNodeElement implements IWirelessSignalTx {
    
	public static final HashMap<String, ArrayList<IWirelessSignalTx>> channelMap = new HashMap<String, ArrayList<IWirelessSignalTx>>();

	WirelessSignalSourceDescriptor descriptor;
	
	public AutoResetProcess autoResetProcess;
	boolean state = false;
	
	public String channel = "Default channel";
	private LightningGlitchProcess lightningGlitchProcess;

    public static final byte setChannelId = 1;

    public WirelessSignalSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

		front = LRDU.Down;
		this.descriptor = (WirelessSignalSourceDescriptor) descriptor;
		WirelessSignalTxElement.channelRegister(this);
		slowProcessList.add(lightningGlitchProcess = new LightningGlitchProcess(getCoordonate()));
		if (this.descriptor.autoReset) {
			slowProcessList.add(autoResetProcess = new AutoResetProcess());
			autoResetProcess.reset();
		}
	}
    
	class AutoResetProcess implements IProcess {
		double timeout = 0;
		double timeoutDelay = 0.21;

		@Override
		public void process(double time) {
			if (timeout > 0) {
				if (timeout - time < 0) {
					if (state) {
						state = false;
						needPublish();
					}
				}
				timeout -= time;
			}
		}

		void reset() {
			timeout = timeoutDelay;
		}
	}
    
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		return 0;
	}

	@Override
	public String multiMeterString() {
		return null;
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
		if (Utils.isPlayerUsingWrench(entityPlayer))
			return false;	
		
		state = ! state;
		if (state && autoResetProcess != null) autoResetProcess.reset();
		needPublish();
		return true;
	}
    
	@Override
	public void destroy(EntityPlayerMP entityPlayer) {
		WirelessSignalTxElement.channelRemove(this);
		super.destroy(entityPlayer);
	}
    
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setString("channel", channel);
		nbt.setBoolean("state", state);
	}
    
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		WirelessSignalTxElement.channelRemove(this);
		
		super.readFromNBT(nbt);
		channel = nbt.getString("channel");
		state = nbt.getBoolean("state");
        
		WirelessSignalTxElement.channelRegister(this);
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
		return (state ? 1.0 : 0.0) + lightningGlitchProcess.glitchOffset;
	}
    
	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		
		try {
			switch (stream.readByte()) {
                case setChannelId:
                    WirelessSignalTxElement.channelRemove(this);
                    channel = stream.readUTF();
                    needPublish();
                    WirelessSignalTxElement.channelRegister(this);
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
			stream.writeBoolean(state);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
