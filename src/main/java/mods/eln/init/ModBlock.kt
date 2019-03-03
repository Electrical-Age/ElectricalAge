package mods.eln.init

import com.teamwizardry.librarianlib.features.base.block.BlockMod
import com.teamwizardry.librarianlib.features.base.block.BlockModVariant
import com.teamwizardry.librarianlib.features.kotlin.get
import com.teamwizardry.librarianlib.features.kotlin.setVelocityAndUpdate
import net.minecraft.block.material.Material
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.lang.Math.abs

object ModBlock {
    @JvmField
    val oreBlock = ElnOreBlock(
        "copper_ore",
        "lead_ore")

    val rubberBlock = RubberBlock()

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

class RubberBlock : BlockMod("rubber", Material.WOOD) {
    override fun onLanded(worldIn: World, entityIn: Entity) {
        if (abs(entityIn.motionY) > 0.1) {
            entityIn.motionY = abs(entityIn.motionY * 0.75)
        } else {
            entityIn.motionY = 0.0
        }
    }

    override fun onFallenUpon(worldIn: World, pos: BlockPos, entityIn: Entity, fallDistance: Float) {
        super.onFallenUpon(worldIn, pos, entityIn, fallDistance / 8.0f)
    }
}

class ElnBlockMod(name: String, material: Material, val uuid: String) : BlockMod(name, material)
