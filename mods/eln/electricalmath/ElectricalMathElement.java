package mods.eln.electricalmath;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.network.Player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalGateOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.solver.Equation;
import mods.eln.solver.IOperator;
import mods.eln.solver.ISymbole;
import mods.eln.solver.IValue;
import mods.eln.solver.OperatorMapperFunc;

public class ElectricalMathElement extends SixNodeElement {

	NodeElectricalGateOutput gateOutput = new NodeElectricalGateOutput("gateOutput");
	NodeElectricalGateOutputProcess gateOutputProcess = new NodeElectricalGateOutputProcess("gateOutputProcess",gateOutput);
	
	NodeElectricalGateInput[] gateInput = new NodeElectricalGateInput[]{new NodeElectricalGateInput("gateA"),new NodeElectricalGateInput("gateB"),new NodeElectricalGateInput("gateC")};

	ArrayList<ISymbole> symboleList = new ArrayList<ISymbole>();
	
	ElectricalMathElectricalProcess electricalProcess = new ElectricalMathElectricalProcess(this);
	
	boolean firstBoot = true;
	
	public ElectricalMathElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		electricalLoadList.add(gateOutput);
		electricalLoadList.add(gateInput[0]);
		electricalLoadList.add(gateInput[1]);
		electricalLoadList.add(gateInput[2]);
		
		electricalProcessList.add(gateOutputProcess);
		
		slowProcessList.add(electricalProcess);
		
		
		symboleList.add(new GateInputSymbol("A", gateInput[0]));
		symboleList.add(new GateInputSymbol("B", gateInput[1]));
		symboleList.add(new GateInputSymbol("C", gateInput[2]));
		symboleList.add(new DayTime());
	}
	
	boolean sideConnectionEnable[] = new boolean[3];
	String expression = "";
	Equation equation;
	
	
	class GateInputSymbol implements ISymbole
	{
		private String name;
		private NodeElectricalGateInput gate;

		public GateInputSymbol(String name,NodeElectricalGateInput gate) {
			this.name = name;
			this.gate = gate;
		}

		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return gate.getNormalized();
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return name;
		}
	}
	
	class ElectricalMathElectricalProcess implements IProcess{
		private ElectricalMathElement e;

		ElectricalMathElectricalProcess(ElectricalMathElement e){
			this.e = e;
		}

		@Override
		public void process(double time) {
			
			if(e.redstoneReady)
				e.gateOutputProcess.setOutputNormalizedSafe(e.equation.getValue(time));
			else
				e.gateOutputProcess.setOutputNormalized(0.0);
		}
	}
	boolean equationIsValid;
	void preProcessEquation(String expression)
	{
		this.expression = expression;
		equation = new Equation();//expression, symboleList, 100);
		equation.setUpDefaultOperatorAndMapper();
		equation.setIterationLimit(100);
		equation.addSymbole(symboleList);
		equation.preProcess(expression);
		
		for(int idx = 0;idx<3;idx++){
			sideConnectionEnable[idx] = equation.isSymboleUsed(symboleList.get(idx));
		}
		this.expression = expression;
		
		redstoneRequired = 0;
		if(equationIsValid = equation.isValid()){
			redstoneRequired = equation.getOperatorCount();
		}
		
		checkRedstone();
		
	}

	
	public class DayTime implements ISymbole{
		@Override
		public double getValue() {
			return sixNode.coordonate.world().getWorldTime()/(24000.0-1.0);
		}

		@Override
		public String getName() {
			return "daytime";
		}	
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		if(lrdu == front) return gateOutput;
		if(lrdu == front.left() && sideConnectionEnable[2]) return gateInput[2];
		if(lrdu == front.inverse() && sideConnectionEnable[1]) return gateInput[1];
		if(lrdu == front.right() && sideConnectionEnable[0]) return gateInput[0];
		return null;
	}
	


	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		if(lrdu == front) return Node.maskElectricalOutputGate;
		if(lrdu == front.left() && sideConnectionEnable[2]) return Node.maskElectricalInputGate;
		if(lrdu == front.inverse() && sideConnectionEnable[1]) return Node.maskElectricalInputGate;
		if(lrdu == front.right() && sideConnectionEnable[0]) return Node.maskElectricalInputGate;
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		if(firstBoot)
			preProcessEquation(expression);
	}
	
	
	@Override
	protected void inventoryChanged() {
		// TODO Auto-generated method stub
		super.inventoryChanged();
		
		checkRedstone();
	}
	int redstoneRequired;
	void checkRedstone()
	{
		int redstoneInStack = 0;

		ItemStack stack = inventory.getStackInSlot(ElectricalMathContainer.restoneSlotId);
		if(stack != null) redstoneInStack = stack.stackSize;
		
		redstoneReady = redstoneRequired <= redstoneInStack;
		needPublish();
	}
	
	boolean redstoneReady = false;
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return onBlockActivatedRotate(entityPlayer);
	}

	SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalMathContainer(sixNode, player, inventory);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt, str);
		nbt.setString(str +  "expression",expression);
		equation.writeToNBT(nbt, str + "equation");
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt, str);
		expression = nbt.getString(str + "expression");
		preProcessEquation(expression);
		equation.readFromNBT(nbt, str + "equation");
		
		firstBoot = false;
	}
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeUTF(expression);
			stream.writeInt(redstoneRequired);
			stream.writeBoolean(equationIsValid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static final byte setExpressionId = 1;
	@Override
	public void networkUnserialize(DataInputStream stream, Player player) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream, player);
		try {
			switch(stream.readByte()){
			case setExpressionId:
				preProcessEquation(stream.readUTF());
				reconnect();
				break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
