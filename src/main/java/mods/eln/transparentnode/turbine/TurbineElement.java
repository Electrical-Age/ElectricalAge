package mods.eln.transparentnode.turbine;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TurbineElement extends TransparentNodeElement{

	private static final double[]  voltageFunctionTable = {0.000,0.5,0.8,0.9,0.95,1.0,1.1,1.2};
	private static final FunctionTable voltageFunction = new FunctionTable(voltageFunctionTable,1.2);

	public NbtElectricalLoad inputLoad = new NbtElectricalLoad("inputLoad");
	public NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");

	public Resistor inputToTurbinResistor = new Resistor(inputLoad,positiveLoad);
	
	public NbtThermalLoad warmLoad = new NbtThermalLoad("warmLoad");
	public NbtThermalLoad coolLoad = new NbtThermalLoad("coolLoad");

	public VoltageSource electricalPowerSourceProcess = new VoltageSource("PowerSource",positiveLoad,null); 
	public TurbineThermalProcess turbineThermaltProcess = new TurbineThermalProcess(this);
	public TurbineElectricalProcess turbineElectricalProcess = new TurbineElectricalProcess(this);
	public TurbineSlowProcess turbineSlowProcess = new TurbineSlowProcess(this);
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 1, this);
	
	
	TurbineDescriptor descriptor;
	
	public TurbineElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (TurbineDescriptor) descriptor;
		
	   	electricalLoadList.add(inputLoad);
	   	electricalLoadList.add(positiveLoad);
	   	
	   	electricalComponentList.add(inputToTurbinResistor);
	   	
    	thermalLoadList.add(warmLoad);
    	thermalLoadList.add(coolLoad);
    	
    
    	//electricalProcessList.add(turbineElectricalProcess);
    	slowProcessList.add(turbineSlowProcess);
    	
    	electricalComponentList.add(electricalPowerSourceProcess);
    	thermalFastProcessList.add(turbineThermaltProcess);
    
    	
    	WorldExplosion exp = new WorldExplosion(this).machineExplosion();
    	
		slowProcessList.add(thermalWatchdog);
		
		thermalWatchdog
		 .set(warmLoad)
		 .setTMax(this.descriptor.nominalDeltaT*2)
		 .set(exp);

		
		slowProcessList.add(voltageWatchdog.set(positiveLoad).setUNominal(this.descriptor.nominalU).set(exp));

	}

	VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();
	ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
	
	
	@Override
	public void connectJob() {
		
		super.connectJob();
		Eln.simulator.mna.addProcess(turbineElectricalProcess);
	}
	@Override
	public void disconnectJob() {
		
		super.disconnectJob();
		Eln.simulator.mna.removeProcess(turbineElectricalProcess);
	}
	
	@Override
	public IInventory getInventory() {
		
		return inventory;
	}
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		if(side == front) return inputLoad;
		if(side == front.back()) return inputLoad;
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

		descriptor.applyTo(inputLoad);
		inputToTurbinResistor.setR(descriptor.electricalRs*30);
		
		descriptor.applyTo(warmLoad);
		descriptor.applyTo(coolLoad);
		
		
		//electricalPowerSourceProcess.setImax(descriptor.nominalP/descriptor.nominalU*4);

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
		
		return false;
	}
	
	
	@Override
	public boolean hasGui() {
		
		return false;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		
		return new TurbineContainer(node, player, inventory);
	}


	public float getLightOpacity() {
		
		return 1.0f;
	}
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		
		super.networkSerialize(stream);
		node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
	}

	@Override
	public Map<String, String> getWaila(){
		Map<String, String> info = new HashMap<String, String>();
		info.put(I18N.tr("Nominal") + " \u0394T",
			(warmLoad.Tc - coolLoad.Tc == descriptor.nominalDeltaT ? I18N.tr("Yes") : I18N.tr("No")));
		info.put(I18N.tr("Generated power"), Utils.plotPower("", electricalPowerSourceProcess.getP()));
		if(Eln.wailaEasyMode){
			info.put("\u0394T", Utils.plotCelsius("", warmLoad.Tc - coolLoad.Tc));
			info.put(I18N.tr("Voltage"), Utils.plotVolt("", electricalPowerSourceProcess.getU()));
		}
		return info;
	}
}
