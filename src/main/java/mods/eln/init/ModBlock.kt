package mods.eln.init

import com.teamwizardry.librarianlib.features.base.block.BlockMod
import jdk.nashorn.internal.ir.Block
import net.minecraft.block.material.Material

object ModBlock {
    @JvmField
    val oreBlock = BlockMod("oreBlock", Material.ROCK,
        "Copper oreBlock",
        "Lead oreBlock")

    // TODO(1.12): These are obviously not done.
    @JvmField
    val ghostBlock = ElnBlockMod("ghostBlock", Material.ROCK, "g")

    @JvmField
    val sixNodeBlock = ElnBlockMod("sixNodeBlock", Material.ROCK, "s")

    @JvmField
    val transparentNodeBlock = ElnBlockMod("transparentNodeBlock", Material.ROCK, "t")

    @JvmField
    val lightBlock = BlockMod("Light", Material.AIR)
}

class ElnBlockMod(name: String, material: Material, val uuid: String) : BlockMod(name, material)
