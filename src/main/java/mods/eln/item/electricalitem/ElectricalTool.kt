package mods.eln.item.electricalitem


import mods.eln.registry.ElnDeviceRegistry
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level

open class ElectricalTool(name: String, private var strengthOn: Float, private var strengthOff: Float,
                          private var energyStorage: Double, private var energyPerBlock: Double, internal var chargePower: Double) : GenericItemUsingDamageDescriptor(name), IItemEnergyBattery {

    internal var light: Int = 0
    internal var range: Int = 0

    override fun onEntitySwing(entityLiving: LivingEntity, stack: ItemStack): Boolean {
        if (entityLiving.level().isClientSide()) return false

        mods.eln.registry.batch.Wp12bContent.itemEnergyInventoryProcess?.addExclusion(this, 2.0)
        return super.onEntitySwing(entityLiving, stack)
    }

    override fun onBlockDestroyed(stack: ItemStack, w: Level, block: Block, x: Int, y: Int, z: Int, entity: LivingEntity): Boolean {
        subtractEnergyForBlockBreak(stack, block)
        Utils.println("destroy")
        return true
    }

    fun subtractEnergyForBlockBreak(stack: ItemStack, block: Block) {
        if (getStrVsBlock(stack, block) == strengthOn) {
            var e = getEnergy(stack) - energyPerBlock
            if (e < 0) e = 0.0
            setEnergy(stack, e)
        }
    }

    fun getEnergyPerBlock() = energyPerBlock

    fun getStrength(stack: ItemStack): Float {
        return if (getEnergy(stack) >= energyPerBlock) strengthOn else strengthOff
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putDouble("energy", 0.0)
        nbt.putBoolean("powerOn", false)
        nbt.putInt("rand", (Math.random() * 0xFFFFFFF).toInt())
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (itemStack != null && !itemStack.isEmpty)
            list.add(tr("Stored energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDoubleOr("energy", 0.0)
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        editNbt(stack) { it.putDouble("energy", value) }
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return 0.0
    }

    override fun getPriority(stack: ItemStack): Int {
        return 0
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)


    // 26.x port: renderItem() removed (client rendering; redone in P3)


    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}

    val blocksEffectiveAgainst = arrayOf(Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.SAND, Blocks.GRAVEL, Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.CLAY, Blocks.FARMLAND, Blocks.SOUL_SAND, Blocks.MYCELIUM)
}
