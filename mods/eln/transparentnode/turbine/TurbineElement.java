package mods.eln.transparentnode.turbine;

import java.io.DataOutputStream;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class TurbineElement extends TransparentNodeElement{

	private static final double[]  voltageFunctionTable = {0.000,0.5,0.8,0.9,0.95,1.0,1.1,1.2};
	private static FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,1.2);

	public NodeElectricalLoad positiveLoad = new NodeElectricalLoad("positiveLoad");
	public NodeThermalLoad warmLoad = new NodeThermalLoad("warmLoad");
	public NodeThermalLoad coolLoad = new NodeThermalLoad("coolLoad");

	public PowerSource electricalPowerSourceProcess = new PowerSource(positiveLoad); 
	public TurbineThermalProcess turbineThermaltProcess = new TurbineThermalProcess(this);
	public TurbineElectricalProcess turbineElectricalProcess = new TurbineElectricalProcess(this);
	public TurbineSlowProcess turbineSlowProcess = new TurbineSlowProcess(this);
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 1, this);
	
	
	TurbineDescriptor descriptor;
	
	public TurbineElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (TurbineDescriptor) descriptor;
		
	   	electricalLoadList.add(positiveLoad);
	   	
    	thermalLoadList.add(warmLoad);
    	thermalLoadList.add(coolLoad);
    	
    
    	//electricalProcessList.add(turbineElectricalProcess);
    	slowProcessList.add(turbineSlowProcess);
    	
    	electricalComponentList.add(electricalPowerSourceProcess);
    	thermalProcessList.add(turbineThermaltProcess);
    
	}

	@Override
	public void connectJob() {
		// TODO Auto-generated method stub
		super.connectJob();
		Eln.simulator.mna.addProcess(turbineElectricalProcess);
	}
	@Override
	public void disconnectJob() {
		// TODO Auto-generated method stub
		super.disconnectJob();
		Eln.simulator.mna.removeProcess(turbineElectricalProcess);
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
		if(side == front.back()) return positiveLoad;
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
			if(side == front) return NodeBase.maskElectricalPower;
			if(side == front.back()) return NodeBase.maskElectricalPower;
			if(side == front.left()) return  NodeBase.maskThermal;
			if(side == front.right()) return NodeBase.maskThermal;
		}
		return 0;
	}


	@Override
	public String multiMeterString(Direction side) {
		if(side == front.left())return "";
		if(side == front.right())return "";
		if(side == front || side == front.back())return  Utils.plotVolt("U+:",positiveLoad.getU()) + Utils.plotAmpere("I+:",positiveLoad.getCurrent());
		return  Utils.plotVolt("U:",positiveLoad.getU()) + Utils.plotAmpere("I:",positiveLoad.getCurrent());

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		if(side == front.left())return  Utils.plotCelsius("T+:",warmLoad.Tc) + Utils.plotPower("P+:",warmLoad.getPower());
		if(side == front.right())return  Utils.plotCelsius("T-:",coolLoad.Tc) + Utils.plotPower("P-:",coolLoad.getPower());
		return  Utils.plotCelsius("T:",warmLoad.Tc-coolLoad.Tc);

	}

	
	@Override
	public void initialize() {

		descriptor.applyTo(positiveLoad);
		
		descriptor.applyTo(warmLoad);
		descriptor.applyTo(coolLoad);
		
		
		electricalPowerSourceProcess.setImax(descriptor.nominalP/descriptor.nominalU*4);

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
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
	}
}
