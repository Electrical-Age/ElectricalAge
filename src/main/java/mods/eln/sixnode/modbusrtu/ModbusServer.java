package mods.eln.sixnode.modbusrtu;

import com.serotonin.modbus4j.ProcessImage;
import com.serotonin.modbus4j.exception.ModbusInitException;
import mods.eln.Eln;

public class ModbusServer {

    private ModbusServerExtended slave;

    public ModbusServer() {
        if (Eln.modbusEnable) {
            try {
                slave = new ModbusServerExtended(false);
                (new Thread(new ServerThread(slave))).start();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public class ServerThread implements Runnable {
        ModbusServerExtended slave;

        public ServerThread(ModbusServerExtended slave) {
            this.slave = slave;
        }

        @Override
        public void run() {
            /*
			 * while (true){ int a = 0; a++; }
			 */
            try {
                slave.start();
            } catch (ModbusInitException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        if (Eln.modbusEnable) {
            try {
                if (slave != null)
                    slave.stop();
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }

    public boolean add(ProcessImage processImage) {
        if (Eln.modbusEnable) {
            try {
                if (slave == null) return false;
                if (slave.getProcessImage(processImage.getSlaveId()) != null) return false;
                slave.addProcessImage(processImage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
        return true;
    }

    public void remove(ProcessImage processImage) {
        if (Eln.modbusEnable) {
            try {
                if (slave == null) return;
                slave.remove(processImage);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    }
}
