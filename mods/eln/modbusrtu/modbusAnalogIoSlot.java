package mods.eln.modbusrtu;

import com.google.common.base.CaseFormat;

import mods.eln.misc.Utils;
import mods.eln.node.NodeElectricalGateInputOutput;
import mods.eln.node.NodeElectricalGateOutputProcess;

public class modbusAnalogIoSlot extends ModbusSlot{

	
	
	public modbusAnalogIoSlot(int offset, int range,NodeElectricalGateInputOutput gate,NodeElectricalGateOutputProcess gateProcess) {
		super(offset, range);
		this.gate = gate;
		this.gateProcess = gateProcess;
	}

	NodeElectricalGateInputOutput gate;
	NodeElectricalGateOutputProcess gateProcess;
	
	
	@Override
	public boolean getCoil(int id) {
		// TODO Auto-generated method stub
		switch (id) {
		case 0:
			return gateProcess.isHighImpedance();

		}
		return false;
	}

	short getHoldingRegister_2;
	@Override
	public short getHoldingRegister(int id) {
		switch (id) {
		case 1:
			double f = gateProcess.getOutputNormalized();
			getHoldingRegister_2 = Utils.modbusToShort(f, 1);
			return Utils.modbusToShort(f, 0);
		case 2:
			return getHoldingRegister_2;
		case 3:
			return (short)(65535.0 * gateProcess.getOutputNormalized());
		}
		return 0;
	}

	@Override
	public boolean getInput(int id) {
		switch (id) {
		case 1:
			return gate.isInputHigh();
		}
		return false;
	}

	short getInputRegister_2;
	@Override
	public short getInputRegister(int id) {
		switch (id) {
		case 1:
			double f = gate.getInputNormalized();
			getInputRegister_2 = Utils.modbusToShort(f, 1);
			return Utils.modbusToShort(f, 0);
		case 2:
			return getInputRegister_2;
		case 3:
			return (short)(65535.0 * gate.getInputNormalized());
		}
		return 0;
	}

	@Override
	public void setCoil(int id, boolean value) {
		switch (id) {
		case 0:
			gateProcess.setHighImpedance(value);
			break;
		case 1:
			gateProcess.state(value);
			break;
		}
	}

	short setHoldingRegister_1;
	@Override
	public void setHoldingRegister(int id, short value) {
		switch (id) {
		case 1:
			setHoldingRegister_1 = value;
			break;
		case 2:
			gateProcess.setOutputNormalized(Utils.modbusToFloat(setHoldingRegister_1, value));
			break;
		case 3:
			gateProcess.setOutputNormalized((double)((int)value & 0xFFFF) / 65535.0);
			break;
		}		
	}

	@Override
	public void setInput(int id, boolean value) {
		
	}

	@Override
	public void setInputRegister(int id, short value) {
		
	}



}
