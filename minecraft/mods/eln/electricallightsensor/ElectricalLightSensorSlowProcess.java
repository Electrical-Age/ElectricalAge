package mods.eln.electricallightsensor;

import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class ElectricalLightSensorSlowProcess implements IProcess {
	ElectricalLightSensorElement element;
	public ElectricalLightSensorSlowProcess(ElectricalLightSensorElement element) {
		this.element = element;
	}
	double timeCounter = 0, soundTimeTimeout = 0;
	static final double refreshPeriode = 0.2;
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		timeCounter += time;
		if(timeCounter > refreshPeriode)
		{
			timeCounter -= refreshPeriode;
			Coordonate coord = element.sixNode.coordonate;
			//int light = coord.world().getSavedLightValue(EnumSkyBlock.Sky, coord.x, coord.y, coord.z) - coord.world().skylightSubtracted;
		//	System.out.println("Light : " + light);
			World world = coord.world();
			int light = 0;
	        if (!world.provider.hasNoSky)
	        {
	            int i1 = world.getSavedLightValue(EnumSkyBlock.Sky, coord.x, coord.y, coord.z) - world.skylightSubtracted;
	            float f = world.getCelestialAngleRadians(1.0F);

	            if (f < (float)Math.PI)
	            {
	                f += (0.0F - f) * 0.2F;
	            }
	            else
	            {
	                f += (((float)Math.PI * 2F) - f) * 0.2F;
	            }

	            i1 = Math.round((float)i1 * MathHelper.cos(f));

	            if (i1 < 0)
	            {
	                i1 = 0;
	            }

	            if (i1 > 15)
	            {
	                i1 = 15;
	            }

	            light = i1;
	        }
			element.outputGateProcess.setOutputNormalized(light/15.0);
		}
		soundTimeTimeout -= time;
		if(soundTimeTimeout < 0) soundTimeTimeout = 0;
	}

}
