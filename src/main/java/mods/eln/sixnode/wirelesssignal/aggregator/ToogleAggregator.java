package mods.eln.sixnode.wirelesssignal.aggregator;

import java.util.Collection;

import net.minecraft.nbt.NBTTagCompound;

import mods.eln.misc.INBTTReady;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;

public class ToogleAggregator extends BiggerAggregator implements INBTTReady {
    
	double oldValue = 1;

    boolean state = false;
    
	@Override
	public double aggregate(Collection<IWirelessSignalTx> txs) {
		double value = super.aggregate(txs);
		if (value > 0.5 && oldValue <= 0.5) {
			state = !state;
		}
		oldValue = value;
		return state ? 1.0 : 0.0;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		state = nbt.getBoolean(str + "state");
		oldValue = nbt.getDouble(str + "oldValue");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setBoolean(str + "state", state);
		nbt.setDouble(str + "oldValue", oldValue);
	}
}
