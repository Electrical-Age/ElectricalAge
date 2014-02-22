package mods.eln.modbusrtu;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusSlaveSet;
import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.ModbusInitException;

public class ModbusServer {
	ModbusServerExtended slave;
	public ModbusServer() {
		slave = new ModbusServerExtended(false);
		//new ServerThread(slave).run();
		(new Thread(new ServerThread(slave))).start();
		int a = 0;
		a++;
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
		slave.stop();
	}
	
	public boolean add(ProcessImage processImage){
		if(slave.getProcessImage(processImage.getSlaveId()) != null) return false;
		slave.addProcessImage(processImage);
		return true;
	}
	public void remove(ProcessImage processImage){
		slave.remove(processImage);
	}
}
