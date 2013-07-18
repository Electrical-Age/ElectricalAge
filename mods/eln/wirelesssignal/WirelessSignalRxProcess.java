package mods.eln.wirelesssignal;

import java.util.ArrayList;

import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class WirelessSignalRxProcess implements IProcess{

	private WirelessSignalRxElement rx;

	public WirelessSignalRxProcess(WirelessSignalRxElement rx) {
		this.rx = rx;
	}
	
	int sleepTimer = 0;
	
	@Override
	public void process(double time) {
		if(sleepTimer != 0){
			sleepTimer--;
			rx.generation = 1000;
			rx.outputGateProcess.setOutputNormalized(0.0);
			return;
		}
		
		IWirelessSignalTx bestTx = null;
		float bestPower = 2f;
		int bestGeneration = 1000;
		
		ArrayList<IWirelessSignalTx> txList = WirelessSignalTxElement.channelMap.get(rx.channel);
		if(txList != null) {
			int x = rx.sixNode.coordonate.x;
			int y = rx.sixNode.coordonate.y;
			int z = rx.sixNode.coordonate.z;
			for(IWirelessSignalTx tx : txList){
				Coordonate c = tx.getCoordonate();
				int distance = Math.abs(x - c.x) + Math.abs(y - c.y) + Math.abs(z - c.z);
				
				if(c.dimention == rx.sixNode.coordonate.dimention && distance <= tx.getRange() && tx.getGeneration() < 100){
					float power = distance / tx.getRange();
					if(tx.getGeneration() < bestGeneration || (tx.getGeneration() == bestGeneration && power < bestPower)){
						bestPower = power;
						bestTx = tx;
						bestGeneration = tx.getGeneration();
					}
				}
			}
		}
		
		if(bestTx != null){
			if(bestTx == rx){
				sleepTimer = 10;
				rx.generation = 1000;
				rx.outputGateProcess.setOutputNormalized(0.0);
			}
			else
			{
				rx.outputGateProcess.setOutputNormalized(bestTx.getValue());
				rx.generation = bestGeneration + 1;
			}
		}
		else{
			rx.generation = 1000;
			rx.outputGateProcess.setOutputNormalized(0.0);
		}
		
	}

}
