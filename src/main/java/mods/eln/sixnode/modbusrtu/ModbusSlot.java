package mods.eln.sixnode.modbusrtu;

public abstract class ModbusSlot implements IModbusSlot {

    int range, offset;

    public ModbusSlot(int offset, int range) {
		this.offset = offset;
		this.range = range;
	}

	@Override
	public int getSize() {
		return range;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	@Override
	public void writeCoil(int id, boolean value) {
		setCoil(id, value);
	}

	@Override
	public void writeHoldingRegister(int id, short value) {
		setHoldingRegister(id, value);
	}
}
