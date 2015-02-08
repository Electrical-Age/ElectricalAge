package mods.eln.sixnode.modbusrtu;

public interface IModbusSlot {

	int getOffset();
	int getSize();

	public boolean getCoil(int id);
	public short getHoldingRegister(int id);
	public boolean getInput(int id);
	public short getInputRegister(int id);
	public void setCoil(int id, boolean value);
	public void setHoldingRegister(int id, short value);
	public void setInput(int id, boolean value);
	public void setInputRegister(int id, short value);
	public void writeCoil(int id, boolean value);
	public void writeHoldingRegister(int id, short value);
}
