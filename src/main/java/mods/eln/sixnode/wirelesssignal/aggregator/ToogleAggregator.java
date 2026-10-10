package mods.eln.sixnode.wirelesssignal.aggregator;

import mods.eln.misc.INBTTReady;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import net.minecraft.nbt.CompoundTag;

import java.util.Collection;

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
    public void readFromNBT(CompoundTag nbt, String str) {
        state = nbt.getBooleanOr(str + "state", false);
        oldValue = nbt.getDoubleOr(str + "oldValue", 0.0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putBoolean(str + "state", state);
        nbt.putDouble(str + "oldValue", oldValue);
    }
}
