package mods.eln.transparentnode

import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import java.io.DataInputStream
import java.io.DataOutputStream

class FuelHeatFurnaceDescriptor(name: String, val model: Obj3D, val baselineNominalPower: Double) :
        TransparentNodeDescriptor(name, FuelHeatFurnaceElement::class.java, FuelHeatFurnaceRender::class.java) {
    private val main = model.getPart("Main")
    private val burners = arrayOf(model.getPart("BurnerA"), model.getPart("BurnerB"), model.getPart("BurnerC"))
    private val powerLED = model.getPart("PowerLED")

    override fun use2DIcon() = false

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addThermal(newItemStack())
    }

    fun draw(installedBurner: Int? = null, on: Boolean = false) {
        main?.draw()
        if (installedBurner != null) {
            burners[installedBurner]?.draw()
        }

        if (on) {
            UtilsClient.drawLight(powerLED)
        } else {
            powerLED?.draw()
        }
    }

    override fun handleRenderType(item: ItemStack?, type: IItemRenderer.ItemRenderType?) = true

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        draw()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Generates heat when supplied with fuel."))
        //TODO list.add("  " + I18N.tr("Max. temperature: %1$°C", Utils.plotValue(thermal.warmLimit)))
    }
}

class FuelHeatFurnaceElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElement(transparentNode, descriptor) {
    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val controlLoad = NbtElectricalGateInput("commandLoad")
    private val tank = TransparentNodeElementFluidHandler(200)

    init {
        thermalLoadList.add(thermalLoad)
        electricalLoadList.add(controlLoad)
        tank.setFilter(fluidListToFluids(dieselList + gasolineList))
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?) = when {
        side != front.inverse && lrdu == LRDU.Down -> controlLoad
        else -> null
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = when {
        side == front.inverse && lrdu == LRDU.Down -> thermalLoad
        else -> null
    }

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?) = when (lrdu) {
        LRDU.Down -> when (side) {
            front.inverse -> NodeBase.maskThermal
            else -> NodeBase.maskElectricalInputGate
        }
        else -> 0
    }

    override fun getFluidHandler() = tank

    override fun multiMeterString(side: Direction?) = "" // TODO...

    override fun thermoMeterString(side: Direction?) = "" // TODO...

    override fun initialize() {
        connect()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)

        // TODO...
    }

    override fun networkUnserialize(stream: DataInputStream?): Byte {
        return super.networkUnserialize(stream)
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)

        // TODO...
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)

        // TODO...
    }

    override fun hasGui() = false // TODO: Indeed it has a GUI

    override fun getWaila(): MutableMap<String, String> {
        return super.getWaila()

        // TODO...
    }
}

class FuelHeatFurnaceRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElementRender(tileEntity, descriptor) {
    override fun draw() {
        front.glRotateXnRef()
        (transparentNodedescriptor as FuelHeatFurnaceDescriptor).draw()
    }
}
