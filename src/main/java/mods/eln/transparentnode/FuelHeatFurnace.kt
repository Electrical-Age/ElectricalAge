package mods.eln.transparentnode

import mods.eln.generic.GenericItemUsingDamage
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.*
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N
import mods.eln.item.FuelBurnerDescriptor
import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType
import mods.eln.item.regulator.RegulatorSlot
import mods.eln.misc.*
import mods.eln.node.INodeContainer
import mods.eln.node.NodeBase
import mods.eln.node.Synchronizable
import mods.eln.node.published
import mods.eln.node.transparent.*
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.wiki.Data
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import java.io.DataInputStream
import java.io.DataOutputStream

class FuelHeatFurnaceDescriptor(name: String, model: Obj3D, val baselineNominalPower: Double) :
        TransparentNodeDescriptor(name, FuelHeatFurnaceElement::class.java, FuelHeatFurnaceRender::class.java) {
    private val main = model.getPart("Main")
    private val burners = arrayOf(model.getPart("BurnerA"), model.getPart("BurnerB"), model.getPart("BurnerC"))
    private val powerLED = model.getPart("PowerLED")
    private val heatLED = model.getPart("HeatLED")

    override fun use2DIcon() = false

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addThermal(newItemStack())
    }

    fun draw(installedBurner: Int? = null, on: Boolean = false, heating: Boolean = false) {
        main?.draw()
        if (installedBurner != null) {
            burners[installedBurner]?.draw()
        }

        if (on) {
            UtilsClient.drawLight(powerLED)
        } else {
            powerLED?.draw()
        }

        if (heating) {
            UtilsClient.drawLight(heatLED)
        } else {
            heatLED?.draw()
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
    companion object {
        val ExternalControlledToggleEvent: Byte = 0
        val MainSwitchToggleEvent: Byte = 1
        val SetManualControlValueEvent: Byte = 2
        val SetTemperatureEvent: Byte = 3
    }

    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val controlLoad = NbtElectricalGateInput("commandLoad")

    private val tank = TransparentNodeElementFluidHandler(200)

    private val inventory_ = TransparentNodeElementInventory(2, 1, this)

    private var externalControlled by published(false)
    private var mainSwitch by published(false)

    private var manualControl by published(0.0)
    private var setTemperature by published(0.0)

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

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(externalControlled)
        stream.writeBoolean(mainSwitch)
        stream.writeFloat(thermalLoad.Tc.toFloat())
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (super.networkUnserialize(stream)) {
            ExternalControlledToggleEvent -> externalControlled = !externalControlled
            MainSwitchToggleEvent -> mainSwitch = !mainSwitch
            SetManualControlValueEvent -> manualControl = stream.readFloat().toDouble()
            SetTemperatureEvent -> setTemperature = stream.readFloat().toDouble()
        }

        return TransparentNodeElement.unserializeNulldId
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)

        // TODO...
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)

        // TODO...
    }

    override fun hasGui() = true

    override fun getWaila(): MutableMap<String, String> {
        return super.getWaila()

        // TODO...
    }

    override fun getInventory() = inventory_

    override fun inventoryChange(inventory: IInventory?) {
        super.inventoryChange(inventory)
        mainSwitch = mainSwitch && inventory_.getStackInSlot(FuelHeatFurnaceContainer.FuelBurnerSlot) != null
    }

    override fun newContainer(side: Direction?, player: EntityPlayer) = FuelHeatFurnaceContainer(node, player, inventory_)
}

class FuelHeatFurnaceRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElementRender(tileEntity, descriptor) {
    private val inventory = TransparentNodeElementInventory(2, 1, this)

    var externalControlled = false
    var mainSwitch = false

    var manualControl = Synchronizable(0f)
    var setTemperature = Synchronizable(0f)

    var heatPower = 0f
    var actualTemperature = 0f

    override fun draw() {
        front.glRotateXnRef()

        val burnerItemStack = inventory.getStackInSlot(FuelHeatFurnaceContainer.FuelBurnerSlot)
        (transparentNodedescriptor as FuelHeatFurnaceDescriptor).draw(
                ((burnerItemStack?.item as? GenericItemUsingDamage<*>)?.getDescriptor(burnerItemStack) as? FuelBurnerDescriptor)?.type,
                mainSwitch, heatPower != 0f)
    }

    override fun newGuiDraw(side: Direction?, player: EntityPlayer) = FuelHeatFurnaceGui(player, inventory, this)

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        externalControlled = stream.readBoolean()
        mainSwitch = stream.readBoolean()
        actualTemperature = stream.readFloat()
    }
}

class FuelHeatFurnaceContainer(val base: NodeBase?, player: EntityPlayer, inventory: IInventory) :
        BasicContainer(player, inventory,
                arrayOf(GenericItemUsingDamageSlot(inventory, FuelBurnerSlot, 26, 58, 1, FuelBurnerDescriptor::class.java,
                        SlotSkin.medium, arrayOf(I18N.tr("Fuel burner slot"))),
                        RegulatorSlot(inventory, RegulatorSlot, 8, 58, 1, arrayOf(RegulatorType.OnOff, RegulatorType.Analog),
                                SlotSkin.medium))), INodeContainer {
    companion object {
        val FuelBurnerSlot = 0
        val RegulatorSlot = 1
    }
    override fun getNode() = base

    override fun getRefreshRateDivider() = 1
}

class FuelHeatFurnaceGui(player: EntityPlayer, val inventory: IInventory, val render: FuelHeatFurnaceRender) :
        GuiContainerEln(FuelHeatFurnaceContainer(null, player, inventory)) {
    lateinit var externalControlled: GuiButton
    lateinit var mainSwitch: GuiButton

    lateinit var manualControl: GuiVerticalTrackBar
    lateinit var setTemperature: GuiVerticalTrackBarHeat

    override fun initGui() {
        super.initGui()

        externalControlled = newGuiButton(6, 6, 100, "")
        mainSwitch = newGuiButton(6, 30, 100, "")

        manualControl = newGuiVerticalTrackBar(144, 8, 20, 69)
        manualControl.setStepIdMax((0.9f / 0.01f).toInt())
        manualControl.setRange(0.1f, 1f)
        manualControl.value = render.manualControl.value

        setTemperature = newGuiVerticalTrackBarHeat(116, 8, 20, 69)
        setTemperature.setStepIdMax(98)
        setTemperature.setRange(0f, 900f)
        setTemperature.value = render.setTemperature.value
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (!render.externalControlled)
            externalControlled.displayString = I18N.tr("Internal control")
        else
            externalControlled.displayString = I18N.tr("External control")

        if (render.mainSwitch)
            mainSwitch.displayString = I18N.tr("Furnace is on")
        else
            mainSwitch.displayString = I18N.tr("Furnace is off")
        mainSwitch.enabled = inventory.getStackInSlot(FuelHeatFurnaceContainer.FuelBurnerSlot) != null

        if (render.manualControl.pending) {
            manualControl.value = render.manualControl.value
        }
        manualControl.setEnable(inventory.getStackInSlot(FuelHeatFurnaceContainer.RegulatorSlot) == null &&
                !render.externalControlled)
        manualControl.setComment(0, I18N.tr("Control value at %1$", Utils.plotPercent("", manualControl.value.toDouble())))
        manualControl.setComment(1, I18N.tr("Heat Power: %1$", Utils.plotPower("", render.heatPower.toDouble())))

        if (render.setTemperature.pending) {
            setTemperature.value = render.setTemperature.value
        }
        setTemperature.setEnable(inventory.getStackInSlot(FuelHeatFurnaceContainer.RegulatorSlot) != null &&
                !render.externalControlled)
        setTemperature.temperatureHit = render.actualTemperature
        setTemperature.setComment(0, I18N.tr("Temperature"))
        setTemperature.setComment(1, I18N.tr("Actual: %1$", Utils.plotCelsius("", render.actualTemperature.toDouble())))
        if (!render.externalControlled)
            setTemperature.setComment(2, I18N.tr("Set point: %1$", Utils.plotCelsius("", setTemperature.value.toDouble())))
    }

    override fun guiObjectEvent(sender: IGuiObject?) {
        super.guiObjectEvent(sender)

        when (sender) {
            externalControlled -> render.clientSendId(FuelHeatFurnaceElement.ExternalControlledToggleEvent)
            mainSwitch -> render.clientSendId(FuelHeatFurnaceElement.MainSwitchToggleEvent)
            manualControl -> render.clientSendFloat(FuelHeatFurnaceElement.SetManualControlValueEvent, manualControl.value)
            setTemperature -> render.clientSendFloat(FuelHeatFurnaceElement.SetTemperatureEvent, setTemperature.value)
        }
    }

    override fun newHelper() = HelperStdContainer(this)
}
