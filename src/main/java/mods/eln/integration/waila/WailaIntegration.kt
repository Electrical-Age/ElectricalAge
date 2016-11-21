package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaRegistrar

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaRegistrar", modid = "Waila")
object WailaIntegration {

    @JvmStatic
    fun callbackRegister(registrar: IWailaRegistrar) {
        val transparentNodeHandler = TransparentNodeHandler()
        val ghostNodeHandler = GhostNodeWailaProvider(transparentNodeHandler)
        registrar.registerBodyProvider(transparentNodeHandler, mods.eln.node.transparent.TransparentNodeBlock::class.java)
        registrar.registerHeadProvider(ghostNodeHandler, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerBodyProvider(ghostNodeHandler, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerStackProvider(ghostNodeHandler, mods.eln.ghost.GhostBlock::class.java)
    }
}
