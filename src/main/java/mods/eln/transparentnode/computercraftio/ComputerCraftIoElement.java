package mods.eln.transparentnode.computercraftio;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInputOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import net.minecraft.entity.player.EntityPlayer;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public class ComputerCraftIoElement extends TransparentNodeElement implements IPeripheral {
	
	public NbtElectricalGateInputOutput[] ioGate = new NbtElectricalGateInputOutput[4];
	public NbtElectricalGateOutputProcess[] ioGateProcess = new NbtElectricalGateOutputProcess[4];

	ComputerCraftIoDescriptor descriptor;
	
	public ComputerCraftIoElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		for (int idx = 0; idx < 4; idx++) {
			ioGate[idx] = new NbtElectricalGateInputOutput("ioGate" + idx);
			ioGateProcess[idx] = new NbtElectricalGateOutputProcess("ioGateProcess" + idx, ioGate[idx]);
			
			electricalLoadList.add(ioGate[idx]);
			electricalComponentList.add(ioGateProcess[idx]);
			
			ioGateProcess[idx].setHighImpedance(true);
		}
	   	this.descriptor = (ComputerCraftIoDescriptor) descriptor;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down || side.isY()) return null;
		return ioGate[side.getHorizontalIndex()];
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (lrdu == lrdu.Down && side.isNotY()) {
			return NodeBase.maskElectricalGate;	
		}
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return null;
		//Utils.plotUIP(powerLoad.Uc, powerLoad.getCurrent());
	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return  null;
	}

	@Override
	public void initialize() {
		connect();		
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}
	
	public String getType() {
		return "EAProbe";
	}

	@Override
	public String[] getMethodNames() {
		return new String[]{"writeDirection", "readDirection", "writeOutput", "readOutput", "readInput"};
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, ILuaContext context,
			int method, Object[] arguments) throws LuaException, InterruptedException {
		int id = -1;
		if (arguments.length < 1) return null;
		if (!(arguments[0] instanceof String)) return null;
		String arg0 = (String) arguments[0];
		if (arg0.length() < 2) return null;
		
		String sideStr = arg0.substring(0, 2);
		String remaineStr = arg0.substring(2, arg0.length());
		
		//Utils.println(sideStr + " " + remaineStr);
		
		if (sideStr.equals("XN")) id = 0;
		if (sideStr.equals("XP")) id = 1;
		if (sideStr.equals("ZN")) id = 2;
		if (sideStr.equals("ZP")) id = 3;
		if (id == -1) return null;
		
		if (remaineStr.length() != 0) {
			Coordonate c = new Coordonate(this.node.coordonate);
			Direction side = Direction.fromHorizontalIndex(id);	
			c.move(side);
			//Utils.println("SUB probe ! " + side + " " + c);
			NodeBase n = NodeManager.instance.getNodeFromCoordonate(c);
			if (n == null) return null;
			//Utils.println("  NodeBase");
			if (!(n instanceof TransparentNode)) return null;
			//Utils.println("  TransparentNode");
			TransparentNode tn = (TransparentNode)n;
			if (!(tn.element instanceof ComputerCraftIoElement)) return null;
			//Utils.println("  ComputerCraftIoElement");
			ComputerCraftIoElement e = (ComputerCraftIoElement) tn.element;
			Object[] argumentsCopy = arguments.clone();
			argumentsCopy[0] = remaineStr;
			return e.callMethod(computer, context, method, argumentsCopy);
		}
		
		switch (method) {
            case 0:
                if (arguments.length < 2) return null;
                ioGateProcess[id].setHighImpedance(arguments[1].equals("in"));
                break;
            case 1:
                return new Object[]{ioGateProcess[id].isHighImpedance() ? "in" : "out"};
            case 2:
                if (arguments.length < 2) return null;
                ioGateProcess[id].setOutputNormalized((Double) arguments[1]);
                break;
            case 3:
                return new Object[]{ioGateProcess[id].getOutputNormalized()};
            case 4:
                return new Object[]{ioGate[id].getInputNormalized()};
            default:
                break;
		}
		return null;
	}

	@Override
	public void attach(IComputerAccess computer) {
	}

	@Override
	public void detach(IComputerAccess computer) {
	}

	@Override
	public boolean equals(IPeripheral other) {
		return other == this;
	}    
}
