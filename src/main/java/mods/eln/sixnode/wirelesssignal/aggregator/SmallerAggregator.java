package mods.eln.sixnode.wirelesssignal.aggregator;

import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;

import java.util.Collection;

public class SmallerAggregator implements IWirelessSignalAggregator {

    @Override
    public double aggregate(Collection<IWirelessSignalTx> txs) {
        double bestValue = 1;
        for (IWirelessSignalTx tx : txs) {
            double v = tx.getValue();
            if (v < bestValue) {
                bestValue = v;
            }
        }

        return bestValue;
    }
}
