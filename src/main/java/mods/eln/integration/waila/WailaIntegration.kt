package mods.eln.integration.waila

import cpw.mods.fml.common.Optional
import mcp.mobius.waila.api.IWailaRegistrar

@Optional.Interface(iface = "mcp.mobius.waila.api.IWailaRegistrar", modid = "Waila")
object WailaIntegration {

    @JvmStatic
    fun callbackRegister(registrar: IWailaRegistrar) {
        val transparentNodeProvider = TransparentNodeHandler()
        val ghostNodeProvider = GhostNodeWailaProvider(transparentNodeProvider)
        val sixNodeProvider = SixNodeWailaProvider()

        registrar.registerBodyProvider(transparentNodeProvider, mods.eln.node.transparent.TransparentNodeBlock::class.java)

        registrar.registerHeadProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerBodyProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)
        registrar.registerStackProvider(ghostNodeProvider, mods.eln.ghost.GhostBlock::class.java)

        registrar.registerBodyProvider(sixNodeProvider, mods.eln.node.six.SixNodeBlock::class.java)
    }
}
