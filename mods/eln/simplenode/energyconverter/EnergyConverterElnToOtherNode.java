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

public abstract class EnergyConverterElnToOtherNode extends SimpleNode {



	@Override
	public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
		if(directionA == front) return maskElectricalPower;
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

	NbtElectricalLoad load = new NbtElectricalLoad("load");
	NbtResistor powerInResistor = new NbtResistor("powerInResistor", load, null);
	ElectricalProcess electricalProcess = new ElectricalProcess();
	VoltageStateWatchDog watchdog = new VoltageStateWatchDog();
	@Override
	public void initialize() {
		electricalLoadList.add(load);
		electricalComponentList.add(powerInResistor);
		electricalProcessList.add(electricalProcess);
		slowProcessList.add(watchdog);

		Eln.applySmallRs(load);

		load.isPrivateSubSystem();
		
		
    	WorldExplosion exp = new WorldExplosion(this).machineExplosion();
    	watchdog.set(load).setUNominal(inStdVoltage).set(exp);

		
		connect();
	}
	public double energyBuffer = 0;
	public double conversionRatio = 20/4;
	public double timeRatio = 20;
	public double energyBufferMax = 500;
	public double inStdVoltage = 50;
	public double inPowerMax = 1000;
	public double otherOutMax = 32;
	
	
	public double inPowerFactor = 0.5;

	class ElectricalProcess implements IProcess {
		double timeout = 0;
		@Override
		public void process(double time) {
			energyBuffer+=powerInResistor.getP()*time;
			timeout -= time;
			if(timeout < 0){
				timeout = 0.05;
				double energyMiss = energyBufferMax-energyBuffer;
				if(energyMiss<= 0){
					powerInResistor.highImpedance();
				}else{
					double factor = Math.min(1,energyMiss/energyBufferMax*2);
					if(factor < 0.005) factor = 0;
					double inP = factor*inPowerMax*inPowerFactor;
					powerInResistor.setR(inStdVoltage*inStdVoltage/inP);
				}
			}
		}
	}
	
	
	public double getOtherModEnergyBuffer(){
		return energyBuffer*conversionRatio;
	}
	public double getOtherModOutMax(){
		return Math.min(getOtherModEnergyBuffer(), otherOutMax);
	}
	public void drawEnergy(double otherModEnergy){
		energyBuffer -= otherModEnergy/conversionRatio;
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
		// TODO Auto-generated method stub
		return true;
	}
	
	
	@Override
	public void publishSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.publishSerialize(stream);
		
		try {
			stream.writeFloat((float) inPowerFactor);
			stream.writeFloat((float) inPowerMax);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static final byte setInPowerFactor = 1;
	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
