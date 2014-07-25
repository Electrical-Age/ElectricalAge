package mods.eln.simplenode.energyconverter;

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

public abstract class EnergyConverterElnToOtherNode extends SimpleNode {

	@Override
	public int getSideConnectionMask(Direction directionA, LRDU lrduA) {
		// TODO Auto-generated method stub
		return maskElectricalPower;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction directionA, LRDU lrduA) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction directionB, LRDU lrduB) {
		// TODO Auto-generated method stub
		return load;
	}

	NbtElectricalLoad load = new NbtElectricalLoad("load");
	NbtResistor powerInResistor = new NbtResistor("powerInResistor", load, null);
	ElectricalProcess electricalProcess = new ElectricalProcess();
	
	@Override
	public void initialize() {
		electricalLoadList.add(load);
		electricalComponentList.add(powerInResistor);
		electricalProcessList.add(electricalProcess);
		
		Eln.applySmallRs(load);

		load.isPrivateSubSystem();
		connect();
	}
	public double energyBuffer = 0;
	public double conversionRatio = 20/4;
	public double timeRatio = 20;
	public double energyBufferMax = 500;
	public double inStdVoltage = 50;
	public double inPowerMax = 1000;
	public double otherOutMax = 32;


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
					double inP = factor*inPowerMax;
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
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		energyBuffer = nbt.getDouble("energyBuffer");
	}
}
