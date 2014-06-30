package mods.eln.solarpannel;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class SolarPannelSlowProcess implements IProcess {
	SolarPannelElement solarPannel;
	public SolarPannelSlowProcess(SolarPannelElement solarPannel) {
		this.solarPannel = solarPannel;
	}

	double timeCounter = 0;
	final double timeCounterRefreshMax = 0.2; 
	@Override
	public void process(double time) {
		timeCounter -= time;
		if(timeCounter < 0)
		{
			//Utils.println("Solar Light : " + getSolarLight());
			/*if(solarPannel.descriptor.basicModel == false)
			{
				solarPannel.currentSource.setI(solarPannel.descriptor.solarIfS.getValue(getSolarLight()));
			}
			else*/
			{
				solarPannel.powerSource.setP(solarPannel.descriptor.electricalPmax*getSolarLight());
			}
			timeCounter += Math.random()*timeCounterRefreshMax/2 + timeCounterRefreshMax/2;
		}
		
	}
	
	public double getSolarLight()
	{
		double solarAlpha = getSolarAlpha();
	//	Utils.print("solarAlpha : " + solarAlpha + "  ");
		if(solarAlpha >= Math.PI) return 0.0;
		
		if(solarPannel.inventory.getStackInSlot(SolarPannelContainer.trackerSlotId) != null)
		{
			solarPannel.pannelAlpha = solarPannel.descriptor.alphaTrunk(solarAlpha);
		}
		
		
		

		
		Coordonate coordonate = solarPannel.node.coordonate;
		Vec3 v = Utils.getVec05(coordonate);
		double x = v.xCoord +  solarPannel.descriptor.solarOffsetX,y = v.yCoord + solarPannel.descriptor.solarOffsetY,z = v.zCoord + solarPannel.descriptor.solarOffsetZ;
		World world = coordonate.world();

		double lightAlpha = solarPannel.pannelAlpha - solarAlpha;
		double light = Math.cos(lightAlpha);
		
		if(world.getWorldInfo().isRaining()) light *= 0.5;
		if(world.getWorldInfo().isThundering()) light *= 0.5;
		
		
		if(light < 0.0) light = 0.0;
		
		
		
		double xD = Math.cos(solarAlpha)   , yD = Math.sin(solarAlpha) ;
		
		if(Math.abs(xD) > yD)
		{
			xD = Math.signum(xD);
			yD /= Math.abs(xD);
		}
		else
		{
			yD = 1.0;
			xD /= yD;			
		}
		int count = 0;
		///world.getChunkProvider().chunkExists(var1, var2)
		while(world.getChunkProvider().chunkExists(((int)x)>>4, ((int)z)>>4))
		{
			double opacity = world.getBlockLightOpacity((int)x, (int)y, (int)z);
			light *=  (255 - opacity)/255;
			if(light == 0.0)
			{
				break;
			}
			
			x += xD;
			y += yD;
			count++;
			if(y > 256.0) break;
		}
//		Utils.print("count : " + count + "   ");
		return light;
	}
	public static double getSolarAlpha(World world)
	{
		double alpha = world.getCelestialAngleRadians(0f);
		if(alpha < Math.PI/2*3)
		{
			alpha += Math.PI/2;
		}
		else
		{
			alpha -= Math.PI/2*3;
		}
		//return ((((world.getWorldTime()%24000)*12.0/13.0 +500 ) / 24000.0) )% 1.0 * Math.PI*2;
		return  alpha;
	}
	public double getSolarAlpha()
	{
		return getSolarAlpha(solarPannel.node.coordonate.world());
	}
}
