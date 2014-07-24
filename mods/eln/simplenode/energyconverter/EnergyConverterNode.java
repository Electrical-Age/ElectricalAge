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

public class EnergyConverterNode extends SimpleNode {


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
	
	@Override
	public void initialize() {
		electricalLoadList.add(load);

		load.setRs(10);
	

		connect();
	}
	
	
	double energyBuffer = 0;
	double energyBufferMax;
	

}
