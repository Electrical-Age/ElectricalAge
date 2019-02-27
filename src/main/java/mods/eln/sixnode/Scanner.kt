package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.capability.IFluidHandler
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, ScannerRender::class.java) {

    val main = obj.getPart("main")!!
    val leds = arrayOf("LED_0", "LED_1").map { obj.getPart(it) }.requireNoNulls()

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    fun draw(mode: ScanMode) {
        main.draw()
        leds[mode.value.toInt()].draw()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Scans blocks to produce signals."))
        list.add(tr("- For tanks, outputs fill percentage."))
        list.add(tr("- For inventories, outputs either total fill or fraction of slots with any items."))
        list.add(tr("Right-click to change mode."))
        list.add(tr("Otherwise behaves as a vanilla comparator."))
    }
}

enum class ScanMode(val value: Byte) {
    SIMPLE(0), SLOTS(1);

    companion object {
        private val map = ScanMode.values().associateBy(ScanMode::value);
        fun fromByte(type: Byte) = map[type]
    }
}

class ScannerElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {
    val output = NbtElectricalGateOutput("signal")
    val outputProcess = NbtElectricalGateOutputProcess("signalP", output)

    var mode = ScanMode.SIMPLE

    val updater = IProcess {
        val appliedLRDU = side.applyLRDU(front)
        val scannedCoord = Coordinate(coordinate).apply {
            move(appliedLRDU)
        }
        val targetSide: EnumFacing = appliedLRDU.inverse.toForge()
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        var out: Double? = null
        if (te != null) {
            out = scanTileEntity(te, targetSide)
        }
        if (out == null) {
            out = scanBlock(scannedCoord, targetSide)
        }
        outputProcess.outputNormalized = out
    }

    init {
        electricalLoadList.add(output)
        electricalComponentList.add(outputProcess)
        slowProcessList.add(updater)
    }

    private fun scanBlock(scannedCoord: Coordinate, targetSide: EnumFacing): Double {
        val state = scannedCoord.blockState
        return when {
            state.hasComparatorInputOverride() -> state.getComparatorInputOverride(scannedCoord.world(), scannedCoord.pos) / 15.0
            state.isFullCube -> 1.0
            state.isOpaqueCube -> 0.8
            state.isBlockNormalCube -> 0.6
            state.isNormalCube -> 0.4
            state.isTranslucent -> 0.2
            else -> 0.0
        }
    }

    private fun scanTileEntity(te: TileEntity, targetSide: EnumFacing): Double? {
        when (te) {
            is IFluidHandler -> {
                val info = te.tankProperties
                return info.sumByDouble {
                    (it.contents?.amount ?: 0).toDouble() / it.capacity
                } / info.size
            }
            is ISidedInventory -> {
                var sum = 0
                var limit = 0
                val slots = te.getSlotsForFace(targetSide)
                when (mode) {
                    ScanMode.SIMPLE -> slots.forEach {
                        sum += te.getStackInSlot(it)?.stackSize ?: 0
                        limit += te.inventoryStackLimit
                    }

                    ScanMode.SLOTS -> slots.forEach {
                        sum += if ((te.getStackInSlot(it)?.stackSize ?: 0) > 0) 1 else 0
                        limit += 1
                    }
                }
                return sum.toDouble() / limit
            }
            is IInventory -> {
                val sum = when (mode) {
                    ScanMode.SIMPLE -> (0..te.sizeInventory - 1).sumBy {
                        te.getStackInSlot(it)?.stackSize ?: 0
                    }.toDouble()

                    ScanMode.SLOTS -> (0..te.sizeInventory - 1).count {
                        (te.getStackInSlot(it)?.stackSize ?: 0) > 0
                    }.toDouble() * te.inventoryStackLimit
                }
                return sum / te.inventoryStackLimit / te.sizeInventory
            }
            else -> return null
        }
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true
        if (entityPlayer.isHoldingMeter()) return false
        mode = when (mode) {
            ScanMode.SIMPLE -> ScanMode.SLOTS
            ScanMode.SLOTS -> ScanMode.SIMPLE
        }
        needPublish()
        return true
    }

    override fun getElectricalLoad(lrdu: LRDU?) = output
    override fun getThermalLoad(lrdu: LRDU?) = null

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.inverse() -> NodeBase.maskElectricalOutputGate
        else -> 0
    }

    override fun multiMeterString(): String {
        return "Mode: ${tr(mode.name.toLowerCase().capitalize())}, Value: ${Utils.plotPercent("", outputProcess.outputNormalized)}"
    }

    override fun thermoMeterString() = ""

    override fun initialize() {
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(mode.value.toInt())
    }

    override fun writeToNBT(nbt: NBTTagCompound): NBTTagCompound? {
        super.writeToNBT(nbt)
        nbt.setByte("mode", mode.value)
        return nbt;
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        mode = ScanMode.fromByte(nbt.getByte("mode"))!!
    }
}


class ScannerRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(entity, side, descriptor) {
    val desc = descriptor as ScannerDescriptor
    var mode = ScanMode.SIMPLE

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        desc.draw(mode)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        mode = ScanMode.fromByte(stream.readByte())!!
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor = Eln.instance.signalCableDescriptor.render
}
