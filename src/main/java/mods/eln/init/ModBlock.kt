package mods.eln.init

import com.teamwizardry.librarianlib.features.base.block.BlockMod
import com.teamwizardry.librarianlib.features.base.block.BlockModVariant
import net.minecraft.block.material.Material

object ModBlock {
    @JvmField
    val oreBlock = ElnOreBlock(
        "copperOre",
        "leadOre")

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

class ElnOreBlock(vararg variants: String) : BlockModVariant("oreBlock", Material.ROCK, *variants) {
    init {
        setHardness(3.0f)
        setResistance(5.0f)
    }
}

class ElnBlockMod(name: String, material: Material, val uuid: String) : BlockMod(name, material)
