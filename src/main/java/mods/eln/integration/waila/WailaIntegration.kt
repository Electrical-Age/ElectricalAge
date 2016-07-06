package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaRegistrar

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaRegistrar", modid = "Waila")
object WailaIntegration {

    @JvmStatic
    fun callbackRegister(registrar: IWailaRegistrar){
        registrar.registerBodyProvider(TransparentNodeHandler(), mods.eln.node.transparent.TransparentNodeBlock::class.java)
    }
}