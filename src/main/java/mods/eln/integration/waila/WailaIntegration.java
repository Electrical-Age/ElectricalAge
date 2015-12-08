package mods.eln.integration.waila;

import mcp.mobius.waila.api.IWailaRegistrar;
public class WailaIntegration {
	
	public static void callbackRegister(IWailaRegistrar registrar){
		registrar.registerBodyProvider(new WailaTransparentNodeHandler(), mods.eln.node.transparent.TransparentNodeBlock.class);
	}
}
