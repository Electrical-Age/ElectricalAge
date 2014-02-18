package mods.eln.modbusrtu;

public class ModbusNullSlot implements IModbusSlot{

	@Override
	public int getOffset() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getCoil(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public short getHoldingRegister(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getInput(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public short getInputRegister(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCoil(int id, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHoldingRegister(int id, short value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInput(int id, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInputRegister(int id, short value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeCoil(int id, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeHoldingRegister(int id, short value) {
		// TODO Auto-generated method stub
		
	}

}
