package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.ElectricalFuseDescriptor
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.IDestructable
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.heater.ResistorHeatThermalLoad
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class ElectricalFuseHolderDescriptor(name: String, private val obj: Obj3D):
        SixNodeDescriptor(name, ElectricalFuseHolderElement::class.java, ElectricalFuseHolderRender::class.java) {
    private val case = obj.getPart("Case")
    private val fuse = obj.getPart("Fuse")
    private val fuseType = obj.getPart("FuseType")
    private val fuseOk = obj.getPart("FuseOk")

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    override fun use2DIcon() = true

    override fun handleRenderType(item: ItemStack?, type: IItemRenderer.ItemRenderType?) = true

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?,
                                       helper: IItemRenderer.ItemRendererHelper?) =
            type != IItemRenderer.ItemRenderType.INVENTORY

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType?, item: ItemStack?,
                                          helper: IItemRenderer.ItemRendererHelper?) =
            type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(0f)
        }
    }

    fun draw(maxCurrent: Float) {
        case?.draw()
        if (maxCurrent != 0f) {
            VoltageLevelColor.fromMaxCurrent(maxCurrent.toDouble()).setGLColor()
            fuseType?.draw()
            GL11.glColor3f(1f, 1f, 1f)
           if (maxCurrent > 0) {
                fuseOk?.draw()
            }
            fuse?.draw()
        }
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (list != null) {
            I18N.tr("Protects electrical components.\nFuse melts if current exceeds the\nfuse limit").split("\n").forEach { list.add(it) }
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer) =
            super.getFrontFromPlace(side, player).inverse()
}

class ElectricalFuseHolderElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElement(sixNode, side, descriptor), IDestructable {
    private val aLoad = NbtElectricalLoad("aLoad")
    private val bLoad = NbtElectricalLoad("bLoad")
    private val fuseResistor = Resistor(aLoad, bLoad)
    private val thermalLoad = NbtThermalLoad("thermalLoad")
    private val fuseMelting = ResistorHeatThermalLoad(fuseResistor, thermalLoad)
    private var thermalWatchdog = ThermalLoadWatchDog()

    var installedFuse: ElectricalFuseDescriptor? = null
        set(value) {
            if (value == field) return
            field = value
            thermalLoad.Tc = 0.0
            thermalWatchdog.reset()
            refreshSwitchResistor()
            needPublish()
        }

    internal var nbtBoot = false

    init {
        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(fuseResistor)
        electricalComponentList.add(Resistor(bLoad, null).pullDown())
        electricalComponentList.add(Resistor(aLoad, null).pullDown())
        thermalLoadList.add(thermalLoad)
        thermalProcessList.add(fuseMelting)
        thermalLoad.setAsFast()
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog.set(thermalLoad).setLimit(Eln.cableWarmLimit, -100.0).set(this)
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)
        if (nbt != null) {
            front = LRDU.readFromNBT(nbt, "front")

            val fuseCompound = nbt.getTag("fuse") as? NBTTagCompound
            if (fuseCompound != null) {
                val fuseStack = ItemStack.loadItemStackFromNBT(fuseCompound)
                if (fuseStack != null) {
                    installedFuse = GenericItemUsingDamageDescriptorUpgrade.getDescriptor(fuseStack) as? ElectricalFuseDescriptor
                }
            }
        }
        nbtBoot = true
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)
        if (nbt != null) {
            front.writeToNBT(nbt, "front")

            if (installedFuse != null) {
                val fuseCompaound = NBTTagCompound()
                installedFuse!!.newItemStack().writeToNBT(fuseCompaound)
                nbt.setTag("fuse", fuseCompaound)
            }
        }
    }

    override fun getElectricalLoad(lrdu: LRDU?): ElectricalLoad? = when(lrdu) {
        front -> aLoad
        front.inverse() -> bLoad
        else -> null
    }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU?): Int = when(lrdu) {
        front -> NodeBase.maskElectricalAll
        front.inverse() -> NodeBase.maskElectricalAll
        else -> 0
    }

    override fun multiMeterString(): String? = Utils.plotAmpere("I:", Math.abs(aLoad.current))

    override fun thermoMeterString(): String? = Utils.plotCelsius("T:", thermalLoad.t)

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        if (stream != null) {
            try {
                stream.writeFloat((installedFuse?.maxCurrent ?: 0.0).toFloat())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSwitchResistor() {
        val maxCurrent = installedFuse?.maxCurrent ?: 0.0
        if (maxCurrent <= 0.0) {
            fuseResistor.ultraImpedance()
        } else {
            val thermalMaximalPowerDissipated = maxCurrent * maxCurrent * 0.01
            thermalLoad.C = thermalMaximalPowerDissipated * (Eln.cableHeatingTime / 2.0) / Eln.cableWarmLimit
            thermalLoad.Rp = Eln.cableWarmLimit / thermalMaximalPowerDissipated
            fuseResistor.r = 0.01
        }
    }

    override fun initialize() {
        computeElectricalLoad()
    }

    fun computeElectricalLoad() {
        Eln.instance.veryHighVoltageCableDescriptor.applyTo(aLoad)
        Eln.instance.veryHighVoltageCableDescriptor.applyTo(bLoad)
        refreshSwitchResistor()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            sixNode.reconnect()
            return true
        }

        var takenOutFuse: ElectricalFuseDescriptor? = null
        val itemStack = entityPlayer?.currentEquippedItem
        val fuseDescriptor = GenericItemUsingDamageDescriptorUpgrade.getDescriptor(itemStack) as? ElectricalFuseDescriptor
        if (itemStack != null) {
            if (fuseDescriptor != null && itemStack.stackSize > 0) {
                // The player puts in a new lead fuse.
                itemStack.stackSize--
                takenOutFuse = installedFuse
                installedFuse = fuseDescriptor
            }
        } else {
            // The player takes out the fuse and does not install a new one at all.
            takenOutFuse = installedFuse
            installedFuse = null
        }

        // What do we do with the fuse just taken out?
        if (takenOutFuse != null) {
            // Not perfect, but works...
            entityPlayer?.entityDropItem(takenOutFuse.newItemStack(), 1f)
        }

        return takenOutFuse != null || fuseDescriptor != null
    }

    override fun destructImpl() {
        installedFuse = ElectricalFuseDescriptor.BlownFuse
    }

    override fun describe(): String {
        return "Fuse";
    }
}

class ElectricalFuseHolderRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElementRender(tileEntity, side, descriptor) {
    private val descriptor = descriptor as ElectricalFuseHolderDescriptor
    private var fuseMaxCurrent = 0f

    override fun draw() {
        front.right().glRotateOnX()
        descriptor.draw(fuseMaxCurrent)
    }

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        if (stream != null) {
            fuseMaxCurrent = stream.readFloat()
        }
    }
}
