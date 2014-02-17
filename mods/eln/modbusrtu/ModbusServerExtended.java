package mods.eln.modbusrtu;

import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.ip.tcp.TcpSlave;

public class ModbusServerExtended extends TcpSlave{

	public ModbusServerExtended(boolean encapsulated) {
		super(encapsulated);
		
	}

	
	void remove(ProcessImage processImage){
		processImages.remove(processImage.getSlaveId());
	}
}
