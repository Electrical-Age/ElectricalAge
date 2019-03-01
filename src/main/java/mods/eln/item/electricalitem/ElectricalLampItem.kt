package mods.eln.item.electricalitem

import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

class ElectricalLampItem(name: String, private var lightMin: Int, private var rangeMin: Int, private var dischargeMin: Double, private var lightMax: Int,
                         private var rangeMax: Int, internal var dischargeMax: Double, internal var energyStorage: Double, internal var chargePower: Double) : LampItem(name), IItemEnergyBattery {

    internal var on: ResourceLocation = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "on.png")
    internal var off: ResourceLocation = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").toLowerCase() + "off.png")

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

    override fun getDefaultNBT(): NBTTagCompound? {
        val nbt = NBTTagCompound()
        nbt.setDouble("energy", 0.0)
        nbt.setBoolean("powerOn", false)
        nbt.setInteger("rand", (Math.random() * 0xFFFFFFF).toInt())
        return nbt
    }

    override fun getLightState(stack: ItemStack): Int {
        return getNbt(stack).getInteger("LightState")
    }

    private fun setLightState(stack: ItemStack, value: Int) {
        getNbt(stack).setInteger("LightState", value)
    }

    private fun getLightLevel(stack: ItemStack): Int {
        return if (getLightState(stack) == 1) lightMin else lightMax
    }

    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ActionResult<ItemStack> {
        if (!w.isRemote) {
            var lightState = getLightState(s) + 1
            if (lightState > 1) lightState = 0
            //((EntityPlayer) entity).sendMessage("Flashlight !!!");
            when (lightState) {
                0 -> Utils.sendMessage(p as EntityPlayerMP, "Flashlight OFF")
                1 -> Utils.sendMessage(p as EntityPlayerMP, "Flashlight ON")
                2 -> Utils.sendMessage(p as EntityPlayerMP, "Flashlight ON-2")
                else -> {
                }
            }
            setLightState(s, lightState)
        }
        return ActionResult(EnumActionResult.SUCCESS, s)
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Discharge power: %sW", Utils.plotValue(dischargeMin)))
        if (itemStack != null) {
            list.add(tr("Stored Energy: %sJ (%s)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
            list.add(tr("State:") + " " + if (getLightState(itemStack) != 0) tr("On") else tr("Off"))
        }
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).setDouble("energy", value)
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

    // TODO(!.10): Fix rendering
//    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
//        return type != ItemRenderType.INVENTORY
//    }
//
//    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
//        return true
//    }
//
//    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
//        UtilsClient.drawIcon(type, if (getLight(item) != 0 && getLightState(item) != 0) on else off)
//        if (type == ItemRenderType.INVENTORY) {
//            UtilsClient.drawEnergyBare(type, (getEnergy(item) / getEnergyMax(item)).toFloat())
//        }
//    }

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
