package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.ElectricalFuseDescriptor
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
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
            draw()
        }
    }

    fun draw(type: VoltageLevelColor = VoltageLevelColor.None, ok: Boolean = false) {
        case?.draw()
        if (type != VoltageLevelColor.None) {
            fuseType?.draw()
            if (ok) {
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
        SixNodeElement(sixNode, side, descriptor) {
    val descriptor = descriptor as ElectricalFuseHolderDescriptor
    val aLoad = NbtElectricalLoad("aLoad")
    val bLoad = NbtElectricalLoad("bLoad")
    val fuseResistor = Resistor(aLoad, bLoad)

    var fuseMaxCurrent = 0.0
        set(value) {
            if (value == field) return
            field = value
            refreshSwitchResistor()
            needPublish()
        }

    var fuseOk = false
        set(ok) {
            if (ok == field) return
            field = ok
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
        electricalProcessList.add(IProcess {
            // TODO: This algorithm is pretty lame, but will work for the  first tests...
            if (fuseMaxCurrent != 0.0 && fuseOk && Math.abs(fuseResistor.i) > fuseMaxCurrent) {
                fuseOk = false
                refreshSwitchResistor()
                needPublish()
            }
        });
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)
        if (nbt != null) {
            front = LRDU.readFromNBT(nbt, "front")
            fuseMaxCurrent = nbt.getDouble("fuseMaxCurrent")
            fuseOk = nbt.getBoolean("fuseOk")
        }
        nbtBoot = true
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)
        if (nbt != null) {
            front.writeToNBT(nbt, "front")
            nbt.setDouble("fuseMaxCurrent", fuseMaxCurrent)
            nbt.setBoolean("fuseOk", fuseOk)
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

    override fun multiMeterString(): String = Utils.plotAmpere("I:", aLoad.current)

    override fun thermoMeterString(): String? = ""

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        if (stream != null) {
            try {
                stream.writeFloat(fuseMaxCurrent.toFloat())
                stream.writeBoolean(fuseOk)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSwitchResistor() {
        if (fuseMaxCurrent == 0.0 || !fuseOk) {
            fuseResistor.ultraImpedance()
        } else {
            fuseResistor.r = 0.01 // TODO: What resistance should we use here?
        }
    }

    override fun initialize() {
        computeElectricalLoad()
    }

    fun computeElectricalLoad() {
        if (!nbtBoot) fuseOk = false
        nbtBoot = false

        Eln.instance.veryHighVoltageCableDescriptor.applyTo(aLoad)
        Eln.instance.veryHighVoltageCableDescriptor.applyTo(bLoad)
        refreshSwitchResistor()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        val currentItemStack = entityPlayer?.currentEquippedItem

        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            sixNode.reconnect()
            return true
        }

        val itemstack = entityPlayer?.currentEquippedItem
        val fuseDescritor = GenericItemUsingDamageDescriptorUpgrade.getDescriptor(itemstack) as? ElectricalFuseDescriptor
        if (fuseDescritor != null && itemstack?.stackSize ?: 0 > 0) {
            fuseMaxCurrent = fuseDescritor.maxCurrent
            fuseOk = true
            refreshSwitchResistor()
            return true;
        }

        return false
    }
}

class ElectricalFuseHolderRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor):
        SixNodeElementRender(tileEntity, side, descriptor) {
    private val descriptor = descriptor as ElectricalFuseHolderDescriptor
    private var fuseMaxCurrent = 0f
    private var fuseOk = false

    override fun draw() {
        front.right().glRotateOnX()
        descriptor.draw(VoltageLevelColor.fromMaxCurrent(fuseMaxCurrent.toDouble()), fuseOk)
    }

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        if (stream != null) {
            fuseMaxCurrent = stream.readFloat()
            fuseOk = stream.readBoolean()
        }
    }
}
