package mods.eln.init

import com.teamwizardry.librarianlib.features.base.block.BlockMod
import com.teamwizardry.librarianlib.features.base.block.BlockModVariant
import net.minecraft.block.material.Material

object ModBlock {
    @JvmField
    val oreBlock = ElnOreBlock(
        "copper_ore",
        "lead_ore")

    // TODO(1.12): These are obviously not done.
    @JvmField
    val ghostBlock = ElnBlockMod("ghost", Material.ROCK, "g")

    @JvmField
    val sixNodeBlock = ElnBlockMod("sixnode", Material.ROCK, "s")

    @JvmField
    val transparentNodeBlock = ElnBlockMod("transparentnode", Material.ROCK, "t")

    @JvmField
    val lightBlock = BlockMod("light", Material.AIR)
}

class ElnOreBlock(vararg variants: String) : BlockModVariant("ore", Material.ROCK, *variants) {
    init {
        setHardness(3.0f)
        setResistance(5.0f)
    }
}

class ElnBlockMod(name: String, material: Material, val uuid: String) : BlockMod(name, material)
