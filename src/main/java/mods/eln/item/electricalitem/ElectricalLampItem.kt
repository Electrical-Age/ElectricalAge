package mods.eln.item.electricalitem

import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.world.level.Level

import mods.eln.i18n.I18N.tr

class ElectricalLampItem(name: String, private var lightMin: Int, private var rangeMin: Int, private var dischargeMin: Double, private var lightMax: Int,
                         private var rangeMax: Int, internal var dischargeMax: Double, internal var energyStorage: Double, internal var chargePower: Double) : LampItem(name), IItemEnergyBattery {

    internal var on: Identifier = Identifier.fromNamespaceAndPath("eln", "textures/items/" + name.replace(" ", "").lowercase() + "on.png")
    internal var off: Identifier = Identifier.fromNamespaceAndPath("eln", "textures/items/" + name.replace(" ", "").lowercase() + "off.png")

    init {
        setDefaultIcon(name + "off")
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
        Data.addLight(newItemStack())
    }

    internal override fun getRange(stack: ItemStack): Int {
        return if (getLightState(stack) == 1) rangeMin else rangeMax
    }

    internal override fun getLight(stack: ItemStack): Int {
        val energy = getEnergy(stack)
        val state = getLightState(stack)
        var power = 0.0

        when (state) {
            1 -> power = dischargeMin
            2 -> power = dischargeMax
        }

        return if (energy > power) {
            getLightLevel(stack)
        } else {
            0
        }
    }

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putDouble("energy", 0.0)
        nbt.putBoolean("powerOn", false)
        nbt.putInt("rand", (Math.random() * 0xFFFFFFF).toInt())
        return nbt
    }

    internal override fun getLightState(stack: ItemStack): Int {
        return getNbt(stack).getIntOr("LightState", 0)
    }

    private fun setLightState(stack: ItemStack, value: Int) {
        editNbt(stack) { it.putInt("LightState", value) }
    }

    private fun getLightLevel(stack: ItemStack): Int {
        return if (getLightState(stack) == 1) lightMin else lightMax
    }

    override fun onItemRightClick(s: ItemStack, w: Level, p: Player): ItemStack {
        if (!w.isClientSide()) {
            var lightState = getLightState(s) + 1
            if (lightState > 2) lightState = 0
            //((Player) entity).sendMessage("Flashlight !!!");
            when (lightState) {
                0 -> Utils.sendMessage(p as ServerPlayer, "Flashlight OFF")
                1 -> Utils.sendMessage(p as ServerPlayer, "Flashlight ON")
                2 -> Utils.sendMessage(p as ServerPlayer, "Flashlight BOOSTED")
                else -> {
                }
            }
            setLightState(s, lightState)
        }
        return s
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Discharge power: %1\$W", Utils.plotValue(dischargeMin)))
        if (itemStack != null && !itemStack.isEmpty) {
            list.add(tr("Stored Energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
            list.add(tr("State:") + " " + if (getLightState(itemStack) != 0) tr("On") else tr("Off"))
        }
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


    override fun electricalItemUpdate(stack: ItemStack, time: Double) {
        val energy = getEnergy(stack)
        val state = getLightState(stack)
        var power = 0.0

        when (state) {
            1 -> power = dischargeMin * time
            2 -> power = dischargeMax * time
        }

        if (energy > power)
            setEnergy(stack, energy - power)
        else
            setEnergy(stack, 0.0)
    }
}
