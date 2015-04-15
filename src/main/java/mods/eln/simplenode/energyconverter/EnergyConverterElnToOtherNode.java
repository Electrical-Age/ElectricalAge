package mods.eln.simplenode.energyconverter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.simple.SimpleNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtResistor;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;

public class EnergyConverterElnToOtherNode extends SimpleNode {

	EnergyConverterElnToOtherDescriptor descriptor;

    NbtElectricalLoad load = new NbtElectricalLoad("load");
    NbtResistor powerInResistor = new NbtResistor("powerInResistor", load, null);
    ElectricalProcess electricalProcess = new ElectricalProcess();
    VoltageStateWatchDog watchdog = new VoltageStateWatchDog();

    public double energyBuffer = 0;
    public double energyBufferMax;
    public double inStdVoltage;
    public double inPowerMax;
    //public double otherOutMax = 32;

    public double inPowerFactor = 0.5;

    public static final byte setInPowerFactor = 1;

    @Override
	protected void setDescriptorKey(String key) {
		super.setDescriptorKey(key);
		descriptor = (EnergyConverterElnToOtherDescriptor) getDescriptor();
	}
	
	@Override
	public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
		if (directionA == getFront()) return maskElectricalPower;
		return 0;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
		return null;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
		return load;
	}

	@Override
	public void initialize() {
		electricalLoadList.add(load);
		electricalComponentList.add(powerInResistor);
		electricalProcessList.add(electricalProcess);
		slowProcessList.add(watchdog);

		Eln.applySmallRs(load);

		load.setAsPrivate();
		
		descriptor.applyTo(this);

    	WorldExplosion exp = new WorldExplosion(this).machineExplosion();
    	watchdog.set(load).setUNominal(inStdVoltage).set(exp);

		connect();
	}

	class ElectricalProcess implements IProcess {
		double timeout = 0;

        @Override
		public void process(double time) {
			energyBuffer += powerInResistor.getP() * time;
			timeout -= time;
			if (timeout < 0) {
				timeout = 0.05;
				double energyMiss = energyBufferMax - energyBuffer;
				if (energyMiss<= 0) {
					powerInResistor.highImpedance();
				} else {
					double factor = Math.min(1, energyMiss / energyBufferMax * 2);
					if (factor < 0.005) factor = 0;
					double inP = factor * inPowerMax * inPowerFactor;
					powerInResistor.setR(inStdVoltage * inStdVoltage / inP);
				}
			}
		}
	}

	public double getOtherModEnergyBuffer(double conversionRatio) {
		return energyBuffer*conversionRatio;
	}

	public void drawEnergy(double otherModEnergy, double conversionRatio) {
		energyBuffer -= otherModEnergy / conversionRatio;
	}

	public double getOtherModOutMax(double otherOutMax, double conversionRatio) {
		return Math.min(getOtherModEnergyBuffer(conversionRatio), otherOutMax);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setDouble("energyBuffer", energyBuffer);
		nbt.setDouble("inPowerFactor", inPowerFactor);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		energyBuffer = nbt.getDouble("energyBuffer");
		inPowerFactor = nbt.getDouble("inPowerFactor");
	}

	@Override
	public boolean hasGui(Direction side) {
		return true;
	}

	@Override
	public void publishSerialize(DataOutputStream stream) {
		super.publishSerialize(stream);
		
		try {
			stream.writeFloat((float) inPowerFactor);
			stream.writeFloat((float) inPowerMax);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
		try {
			switch (stream.readByte()) {
                case setInPowerFactor:
                    inPowerFactor = stream.readFloat();
                    needPublish();
                    break;

                default:
                    break;
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}

    public static String getNodeUuidStatic() {
		return "ElnToOther";
	}
}
