package mods.eln.sixnode.modbusrtu;

public class ModbusNullSlot implements IModbusSlot {

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public boolean getCoil(int id) {
		return false;
	}

	@Override
	public short getHoldingRegister(int id) {
		return 0;
	}

	@Override
	public boolean getInput(int id) {
		return false;
	}

	@Override
	public short getInputRegister(int id) {
		return 0;
	}

	@Override
	public void setCoil(int id, boolean value) {
	}

	@Override
	public void setHoldingRegister(int id, short value) {
	}

	@Override
	public void setInput(int id, boolean value) {
	}

	@Override
	public void setInputRegister(int id, short value) {
	}

	@Override
	public void writeCoil(int id, boolean value) {
	}

	@Override
	public void writeHoldingRegister(int id, short value) {
	}
}
