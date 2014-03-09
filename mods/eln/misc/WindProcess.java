package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;
import mods.eln.INBTTReady;
import mods.eln.sim.IProcess;

public class WindProcess implements IProcess,INBTTReady{

	double windHit = 5;
	double windTarget = 5;
	double windVariation = 0;
	double windTargetNoose = 0;
	RcInterpolator windTargetFiltred = new RcInterpolator(60);
	@Override
	public void process(double time) {
		double varF = 0.01;
		windHit += windVariation*time;
		windVariation += (getTarget() - windHit)*varF*time + (Math.random()*2-1)*0.1*time;
		windVariation *= (1 - 0.01*time);
		
		if(Math.random() < time/400){
			newWindTarget();
		}
		if( Math.random() < time/40){
			windTargetNoose = (Math.random()*2-1)*1.2;
		}
		
		//windLast = windHit;
		
		//weather.setTarget(Utils.getWeather(world))
		windTargetFiltred.setTarget((float) windTarget);
		windTargetFiltred.step((float) time);
		
		//System.out.println("WIND : " + windHit + "  TARGET : " + getTarget());
		
	}

	public void newWindTarget() {
		// TODO Auto-generated method stub
		float next = (float) (Math.pow(Math.random(),3.0)*20);
		windTarget += (next - windTarget)*0.7;
	}

	public double getTarget(){
		return windTargetNoose + windTargetFiltred.get();
	}
	public double getTargetNotFiltred(){
		return windTargetNoose + windTargetFiltred.getTarget();
	}
	public double getWind(int y){
		y = Math.min(y, 100);
		return Math.max(0, windHit * y/100);		
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		windHit = nbt.getDouble(str + "windHit");
		windTarget = nbt.getDouble(str + "windTarget");
		windVariation = nbt.getDouble(str + "windVariation");
		windTargetFiltred.setValue(nbt.getFloat(str + "windTargetFiltred"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "windHit",windHit);
		nbt.setDouble(str + "windTarget",windTarget);
		nbt.setDouble(str + "windVariation",windVariation);
		nbt.setFloat(str + "windTargetFiltred",windTargetFiltred.get());
	}

}
