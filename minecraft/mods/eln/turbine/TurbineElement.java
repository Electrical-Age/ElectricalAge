package mods.eln.turbine;

import java.io.DataOutputStream;

import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.ElectricalSourceECProcess;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TransformerProcess;
import mods.eln.sim.TurbineThermalProcess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class TurbineElement extends TransparentNodeElement{

	private static final double[]  voltageFunctionTable = {0.000,0.5,0.8,0.9,0.95,1.0,1.1,1.2};
	private static FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,1.2);

	public NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	public NodeElectricalLoad negativeLoad = new NodeElectricalLoad("negativeLoad");
	public NodeThermalLoad warmLoad = new NodeThermalLoad("warmLoad");
	public NodeThermalLoad coolLoad = new NodeThermalLoad("coolLoad");

	public ElectricalPowerSource electricalPowerSourceProcess = new ElectricalPowerSource(positiveLoad,negativeLoad); 
	public TurbineInOutProcess turbineInOutProcess = new TurbineInOutProcess(this);
	
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 1, this);
	
	
	TurbineDescriptor descriptor;
	
	public TurbineElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (TurbineDescriptor) descriptor;
		
	   	electricalLoadList.add(positiveLoad);
	   	electricalLoadList.add(negativeLoad);
	   	
    	thermalLoadList.add(warmLoad);
    	thermalLoadList.add(coolLoad);

    	
    	electricalProcessList.add(electricalPowerSourceProcess);
    	thermalProcessList.add(turbineInOutProcess);
    
	}

	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front) return positiveLoad;
		if(side == front.back()) return (grounded ? positiveLoad : negativeLoad);
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		if(side == front.left()) return warmLoad;
		if(side == front.right()) return coolLoad;
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu == lrdu.Down)
		{
			if(side == front) return Node.maskElectricalPower;
			if(side == front.back()) return Node.maskElectricalPower;
			if(side == front.left()) return  Node.maskThermal;
			if(side == front.right()) return Node.maskThermal;
		}
		return 0;
	}


	@Override
	public String multiMeterString(Direction side) {
		if(side == front.left())return "";
		if(side == front.right())return "";
		if(side == front)return  Utils.plotVolt("U+",positiveLoad.Uc) + Utils.plotAmpere("I+",positiveLoad.getCurrent());
		if(side == front.back() && grounded == false)return  Utils.plotVolt("U-",negativeLoad.Uc) + Utils.plotAmpere("I-",negativeLoad.getCurrent());
		return  Utils.plotVolt("U",positiveLoad.Uc-negativeLoad.Uc) + Utils.plotAmpere("I",positiveLoad.getCurrent());

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		if(side == front.left())return  Utils.plotCelsius("T+",warmLoad.Tc) + Utils.plotPower("P+",warmLoad.getPower());
		if(side == front.right())return  Utils.plotCelsius("T-",coolLoad.Tc) + Utils.plotPower("P-",coolLoad.getPower());
		return  Utils.plotCelsius("T",warmLoad.Tc-coolLoad.Tc);

	}

	
	@Override
	public void initialize() {
/*
    	positiveLoad.setRs(0.05f);
    	positiveLoad.setC(0.1f);
    	
    	negativeLoad.setRs(0.05f);
    	negativeLoad.setC(0.1f);
    	
    	warmLoad.Rs = 0.001f;
     	warmLoad.C = 200.0f;
       	warmLoad.Rp = 10000000.0f;
          	
       	coolLoad.Rs = 0.01f;
    	coolLoad.C = 2000.0f;
    	coolLoad.Rp = 0.00001f;		
		
    	turbineThermalProcess.baseEfficiency = 0.8;
    	turbineThermalProcess.nominalU = 50;
    	turbineThermalProcess.nominalDeltaT = 200;
		*/
		descriptor.applyTo(positiveLoad,false);
		descriptor.applyTo(negativeLoad,grounded);
		
		descriptor.applyTo(warmLoad);
		descriptor.applyTo(coolLoad);
		

		//coolLoad.Rp = 0.001;
		
		connect();
    			
	}
	
	public void computeInventory()
	{

	}

    public void inventoryChange(IInventory inventory)
    {
    	computeInventory();
    }
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new TurbineContainer(node, player, inventory);
	}


	public float getLightOpacity() {
		// TODO Auto-generated method stub
		return 1.0f;
	}
}
