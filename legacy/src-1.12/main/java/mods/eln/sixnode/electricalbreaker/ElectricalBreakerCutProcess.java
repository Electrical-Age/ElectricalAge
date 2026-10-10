package mods.eln.sixnode.electricalbreaker;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalBreakerCutProcess implements IProcess, INBTTReady {

    ElectricalBreakerElement breaker;

    double T = 0;

    public ElectricalBreakerCutProcess(ElectricalBreakerElement breaker) {
        this.breaker = breaker;
    }

    @Override
    public void process(double time) {
        double U = breaker.aLoad.getU();
        double I = breaker.aLoad.getCurrent();
        double Tmax = 0;
        ElectricalCableDescriptor cable = breaker.cableDescriptor;
        if (cable == null) {
            T = 0;
        } else {
            Math.min(I, cable.electricalNominalPower / cable.electricalMaximalVoltage * 10);
            double P = I * I * cable.electricalRs * 2 - T / cable.thermalRp * 0.9;
            /*if (P > 200) {
				int i = 0;
				i++;
				Utils.println(P);
			}*/
            //double pMax = Eln.electricalCableDeltaTMax * cable.thermalC;
            if (I > 1) {
                int idx = 0;
                idx++;
            }
            T += P / cable.thermalC * time;
            // Baughn 2026-10-05: trip at ~1.05 x nominal (1.7.10 0.8 -> ~1.02 x). Steady T = warm (I / 1.2 Inom)^2 / 0.9,
            // so factor f trips at I = 1.2 sqrt(0.9 f) Inom; 0.85 -> 1.0496 x. (A breaker should trip before the fuse, 1.08 x.)
            Tmax = cable.thermalWarmLimit * 0.85;
        }
        //Utils.println(T);

        if (U >= breaker.voltageMax || U < breaker.voltageMin || T > Tmax) {
            breaker.setSwitchState(false);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        T = nbt.getFloat(str + "T");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setFloat(str + "T", (float) T);
    }
}
