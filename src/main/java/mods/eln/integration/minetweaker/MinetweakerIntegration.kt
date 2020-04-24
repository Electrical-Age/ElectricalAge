package mods.eln.integration.minetweaker

import minetweaker.MineTweakerAPI
import mods.eln.integration.minetweaker.machines.Compressor
import mods.eln.integration.minetweaker.machines.Macerator
import mods.eln.integration.minetweaker.machines.Magnetizer
import mods.eln.integration.minetweaker.machines.PlateMachine

object MinetweakerIntegration {

    fun initialize() {
        MineTweakerAPI.registerClass(Macerator::class.java)
        MineTweakerAPI.registerClass(Compressor::class.java)
        MineTweakerAPI.registerClass(Magnetizer::class.java)
        MineTweakerAPI.registerClass(PlateMachine::class.java)
    }

}
