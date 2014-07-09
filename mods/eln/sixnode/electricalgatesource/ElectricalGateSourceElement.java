package mods.eln.sixnode.electricalgatesource;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.swing.text.MaskFormatter;

import mods.eln.Eln;
import mods.eln.item.LampDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.IVoltageDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.lampsocket.LampSocketContainer;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalGateSourceElement extends SixNodeElement {

	public ElectricalGateSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ElectricalGateSourceDescriptor) descriptor;

		front = LRDU.Left;

		electricalLoadList.add(outputGate);
		electricalComponentList.add(outputGateProcess);

		if (this.descriptor.autoReset) {
			slowProcessList.add(autoResetProcess = new AutoResetProcess());
			autoResetProcess.reset();
		}
	}

	public ElectricalGateSourceDescriptor descriptor;
	public NbtElectricalLoad outputGate = new NbtElectricalLoad("outputGate");

	public NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);

	public AutoResetProcess autoResetProcess;

	class AutoResetProcess implements IProcess {
		double timeout = 0;
		double timeoutDelay = 0.21;

		@Override
		public void process(double time) {
			if (timeout > 0) {
				if (timeout - time < 0) {
					outputGateProcess.setOutputNormalized(0);
					needPublish();
				}
				timeout -= time;
			}
		}

		void reset(){
			timeout = timeoutDelay;
		}
	}

	LRDU front;

	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		byte value = nbt.getByte("front");
		front = LRDU.fromInt((value >> 0) & 0x3);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("front", (byte) ((front.toInt() << 0)));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (front == lrdu) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (front == lrdu) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		return Utils.plotUIP(outputGate.getU(), outputGate.getCurrent());
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte((front.toInt() << 4));
			stream.writeFloat((float) outputGateProcess.getU());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		Eln.instance.signalCableDescriptor.applyTo(outputGate);
		computeElectricalLoad();
	}

	@Override
	protected void inventoryChanged() {
		computeElectricalLoad();
	}

	public void computeElectricalLoad() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();

		if (Utils.isPlayerUsingWrench(entityPlayer)) {
			front = front.getNextClockwise();
			sixNode.reconnect();
			sixNode.setNeedPublish(true);
			return true;
		}
		else if (!Utils.playerHasMeter(entityPlayer) && descriptor.onOffOnly) {
			outputGateProcess.state(!outputGateProcess.getOutputOnOff());
			play(new SoundCommand("random.click").mulVolume(0.3F, 0.6F).smallRange());
			if(autoResetProcess != null)
				autoResetProcess.reset();
			needPublish();
			return true;
		}
		// front = LRDU.fromInt((front.toInt() + 1)&3);
		return false;
	}

	public static final byte setVoltagerId = 1;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			switch (stream.readByte()) {
			case setVoltagerId:
				outputGateProcess.setU(stream.readFloat());
				needPublish();
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasGui() {
		return !descriptor.onOffOnly;
	}
}
