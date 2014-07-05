package mods.eln.sixnode.modbusrtu;

import mods.eln.Eln;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.ModbusInitException;

public class ModbusServer {
	ModbusServerExtended slave;
	public ModbusServer() {
		if(Eln.modbusEnable){
			slave = new ModbusServerExtended(false);
			(new Thread(new ServerThread(slave))).start();
		}
	}
	
	public class ServerThread implements Runnable {
		public ServerThread(ModbusServerExtended slave) {
			this.slave = slave;
		}
		ModbusServerExtended slave;
		@Override
	    public void run() {
			/*while(true){
				int a = 0;
				a++;				
			}*/
			try {
				slave.start();
			} catch (ModbusInitException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
	
	public void destroy(){
		if(Eln.modbusEnable){
			slave.stop();
		}
	}
	
	public boolean add(ProcessImage processImage){
		if(Eln.modbusEnable){
			if(slave.getProcessImage(processImage.getSlaveId()) != null) return false;
			slave.addProcessImage(processImage);
		}
		return true;
	}
	public void remove(ProcessImage processImage){
		if(Eln.modbusEnable){
			slave.remove(processImage);
		}
	}
}
