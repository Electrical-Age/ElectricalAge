package mods.eln.wirelesssignal;

import java.util.ArrayList;

import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import mods.eln.INBTTReady;
import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class WirelessSignalRxProcess implements IProcess,INBTTReady{

	private WirelessSignalRxElement rx;

	public WirelessSignalRxProcess(WirelessSignalRxElement rx) {
		this.rx = rx;
		lastCoordonate.invalidate();
	}
	
	int sleepTimer = 0;
	
	float lastPower= -1;
	Coordonate lastCoordonate = new Coordonate(); 
	int lastValidity = 0;
	
	@Override
	public void process(double time) {
		if(sleepTimer != 0){
			sleepTimer--;
			rx.generation = 1000;
			rx.outputGateProcess.setOutputNormalized(0.0);
			return;
		}
		
		IWirelessSignalTx bestTx = null;
		float bestPower = -1f;
		int bestGeneration = 1000;
		Coordonate rxC = rx.sixNode.coordonate;
	
		ArrayList<IWirelessSignalTx> txList = WirelessSignalTxElement.channelMap.get(rx.channel);
		if(txList != null) {
			int x = rxC.x;
			int y = rxC.y;
			int z = rxC.z;
			for(IWirelessSignalTx tx : txList){
				Coordonate txC = tx.getCoordonate();
				double distance = txC.trueDistanceTo(rxC);

				if(txC.dimention == rxC.dimention && distance <= tx.getRange() && tx.getGeneration() < 100){
					float power = 0f;
					if(lastCoordonate.isValid() && txC.equals(lastCoordonate)){
						if(Double.isNaN(distance = getVirtualDistance(distance, txC, rxC))){
							power = lastPower;
						}
						else{
							power = (float) (tx.getRange() - distance);
						}					
					}
					else {
						if(Double.isNaN(distance = getVirtualDistance(distance, txC, rxC))) continue;
						power = (float) (tx.getRange() - distance);
					}
					

					if(power < -0.1) continue;
					
						
					if(tx.getGeneration() < bestGeneration || (tx.getGeneration() == bestGeneration && power > bestPower)){
						bestPower = power;
						bestTx = tx;
						bestGeneration = tx.getGeneration();
					}
				}
			}
		}
		
		boolean connection = false;
		if(bestTx != null){
			if(bestTx == rx){
				sleepTimer = 10;
				rx.generation = 1000;
				rx.outputGateProcess.setOutputNormalized(0.0);
				lastCoordonate.invalidate();
			}
			else
			{
				connection = true;
				rx.outputGateProcess.setOutputNormalized(bestTx.getValue());
				rx.generation = bestGeneration + 1;
				lastCoordonate.copyFrom(bestTx.getCoordonate());
				lastPower = bestPower;
			}
		}
		else{
			lastCoordonate.invalidate();
			rx.generation = 1000;
			rx.outputGateProcess.setOutputNormalized(0.0);
		}
		
		rx.setConnection(connection);
		
	}

	
	static public double getVirtualDistance(double distance,Coordonate txC,Coordonate rxC)
	{
		double virtualDistance = distance;
		if(distance > 2){
			double vx,vy,vz;
			double dx,dy,dz;
			vx = rxC.x + 0.5;
			vy = rxC.y + 0.5;
			vz = rxC.z + 0.5;
			
			dx = (txC.x - rxC.x)/distance;
			dy = (txC.y - rxC.y)/distance;
			dz = (txC.z - rxC.z)/distance;
			Coordonate c = new Coordonate();
			c.setDimention(rxC.dimention);
			
			for(int idx = 0;idx < distance - 1;idx++){
				vx += dx;
				vy += dy;
				vz += dz;
				c.x = (int) vx;
				c.y = (int) vy;
				c.z = (int) vz;
				if(c.getBlockExist() == false) return Double.NaN;
				Block b = c.getBlock();
				if(b != null && b.isOpaqueCube()){
					virtualDistance += 2.0;
				}
				
			}
		}
		return virtualDistance;
	}
	

	public static ArrayList<WirelessSignalInfo> getTxList(Coordonate rxC)
	{
		ArrayList<WirelessSignalInfo> list = new ArrayList<WirelessSignalInfo>();
	
		for(ArrayList<IWirelessSignalTx> txList : WirelessSignalTxElement.channelMap.values()){
			IWirelessSignalTx bestTx = null;
			float bestPower = -1;
			int bestGeneration = 1000;
			
			if(txList != null) {
				int x = rxC.x;
				int y = rxC.y;
				int z = rxC.z;
				for(IWirelessSignalTx tx : txList){
					Coordonate txC = tx.getCoordonate();
					double distance = txC.trueDistanceTo(rxC);
	
					if(txC.dimention == rxC.dimention && distance <= tx.getRange() && tx.getGeneration() < 100){
						float power = 0f;
						
						if(Double.isNaN(distance = getVirtualDistance(distance, txC, rxC))) continue;
						power = (float) (tx.getRange() - distance);

						
	
						if(power < -0.1) continue;
						
							
						if(/*tx.getGeneration() < bestGeneration || (tx.getGeneration() == bestGeneration && */power > bestPower/*)*/){
							bestPower = power;
							bestTx = tx;
							bestGeneration = tx.getGeneration();
						}
					}
				}
			}
			
			
			if(bestTx != null){
				WirelessSignalInfo s = new WirelessSignalInfo(bestTx,bestGeneration,bestPower);
				list.add(s);
			}
		}		
		return list;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		lastPower = nbt.getFloat(str + "lastPower");
		lastCoordonate.readFromNBT(nbt, str + "lastCoordonate");
		lastValidity = nbt.getInteger(str + "lastValidity");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setFloat(str + "lastPower",lastPower);
		lastCoordonate.writeToNBT(nbt, str + "lastCoordonate");
		nbt.setInteger(str + "lastValidity",lastValidity);
	}

}





