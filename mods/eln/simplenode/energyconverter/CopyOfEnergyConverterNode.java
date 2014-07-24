package mods.eln.simplenode.energyconverter;

import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyHandler;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.INodeInfo;
import mods.eln.node.simple.SimpleNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;

public class CopyOfEnergyConverterNode extends SimpleNode implements IEnergyHandler{


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

	@Override
	public INodeInfo getInfo() {
		// TODO Auto-generated method stub
		return getInfoStatic();
	}

	public static INodeInfo getInfoStatic() {
		// TODO Auto-generated method stub
		return new INodeInfo() {
			@Override
			public String getUuid() {
				// TODO Auto-generated method stub
				return "eln.econv";
			}
		};
	}

	NbtElectricalLoad load = new NbtElectricalLoad("load");
	Resistor resistor = new Resistor(load, null);
	@Override
	public void initialize() {
		electricalLoadList.add(load);
		electricalComponentList.add(resistor);

		load.setRs(10);
		resistor.setR(90);
		
		
		connect();
	}
	
	
	protected EnergyStorage storage = new EnergyStorage(32000);

	@Override
	public void readFromNBT(NBTTagCompound nbt) {

		super.readFromNBT(nbt);
		storage.readFromNBT(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {

		super.writeToNBT(nbt);
		storage.writeToNBT(nbt);
	}

	/* IEnergyHandler */
	@Override
	public boolean canConnectEnergy(ForgeDirection from) {

		return true;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {

		return storage.receiveEnergy(maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {

		return storage.extractEnergy(maxExtract, simulate);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {

		return storage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {

		return storage.getMaxEnergyStored();
	}

}
