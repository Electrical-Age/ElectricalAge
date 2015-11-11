package mods.eln.integration.minetweaker;

import minetweaker.MineTweakerAPI;
import mods.eln.integration.minetweaker.machines.Compressor;
import mods.eln.integration.minetweaker.machines.Macerator;
import mods.eln.integration.minetweaker.machines.Magnetizer;
import mods.eln.integration.minetweaker.machines.PlateMachine;

public class MinetweakerIntegration {
	public static MinetweakerIntegration instance = new MinetweakerIntegration();

	
	public void initialize(){
		MineTweakerAPI.registerClass(Macerator.class);
		MineTweakerAPI.registerClass(Compressor.class);
		MineTweakerAPI.registerClass(Magnetizer.class);
		MineTweakerAPI.registerClass(PlateMachine.class);
	}
	
}
