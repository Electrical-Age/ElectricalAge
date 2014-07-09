package mods.eln.sixnode.wirelesssignal.aggregator;

import java.util.Collection;

import mods.eln.sixnode.wirelesssignal.tx.IWirelessSignalTx;

public class BiggerAggregator implements IWirelessSignalAggregator{

	@Override
	public IWirelessSignalTx aggregate(Collection<IWirelessSignalTx> txs) {
		IWirelessSignalTx best = null;
		double bestValue = -1000000000;
		for(IWirelessSignalTx tx : txs){
			double v = tx.getValue();
			if(v > bestValue){
				bestValue = v;
				best = tx;
			}
		}
		return best;
	}

}
