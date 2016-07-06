package mods.eln.sixnode.electricalfiredetector;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataOutputStream;
import java.io.IOException;

import static mods.eln.i18n.I18N.tr;

public class ElectricalFireDetectorElement extends SixNodeElement {

	ElectricalFireDetectorDescriptor descriptor;

    public NbtElectricalGateOutput outputGate;
    public NbtElectricalGateOutputProcess outputGateProcess;
    public ElectricalFireDetectorSlowProcess slowProcess;

	public boolean firePresent = false;

    public ElectricalFireDetectorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);

		this.descriptor = (ElectricalFireDetectorDescriptor) descriptor;

		slowProcess = new ElectricalFireDetectorSlowProcess(this);

		if (!this.descriptor.batteryPowered) {
			outputGate = new NbtElectricalGateOutput("outputGate");
			outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);
			electricalLoadList.add(outputGate);
			electricalComponentList.add(outputGateProcess);
		}

    	slowProcessList.add(slowProcess);
	}

 	public static boolean canBePlacedOnSide(Direction side, int type) {
		return true;
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if (!descriptor.batteryPowered && front == lrdu.left()) return outputGate;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if (!descriptor.batteryPowered && front == lrdu.left()) return NodeBase.maskElectricalOutputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		if (descriptor.batteryPowered) {
			return tr("Fire detected: ") + firePresent;
		} else {
			return Utils.plotVolt("U:", outputGate.getU()) + Utils.plotAmpere("I:", outputGate.getCurrent());
		}
	}

	@Override
	public String thermoMeterString() {
		return "";
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return onBlockActivatedRotate(entityPlayer);
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(true);
			stream.writeBoolean(firePresent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean hasGui() {
		return false;//descriptor.batteryPowered;
	}
}
