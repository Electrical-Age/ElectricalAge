package mods.eln.sixnode


import mods.eln.registry.ElnDeviceRegistry
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.BlockPos
import net.neoforged.neoforge.capabilities.Capabilities
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, null) {

    val main = obj.getPart("main")!!
    val leds = arrayOf("LED_0", "LED_1").map { obj.getPart(it) }.requireNoNulls()

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }
    fun draw(mode: ScanMode) {
        main.draw()
        leds[mode.value.toInt()].draw()
    }


    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
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
        val scannedCoord = Coordonate(coordonate).apply {
            move(appliedLRDU)
        }
        val targetSide: net.minecraft.core.Direction = appliedLRDU.inverse.toEnumFacing()
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        var out: Double? = null
        if (te != null) {
            out = scanTileEntity(scannedCoord, te, targetSide)
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

    private fun scanBlock(scannedCoord: Coordonate, targetSide: net.minecraft.core.Direction): Double {
        val world = scannedCoord.world()
        val pos = scannedCoord.pos()
        val state = world.getBlockState(pos)
        // 1.12: comparator input has no side parameter any more (1.7.10 passed targetSide)
        if (state.hasAnalogOutputSignal()) {
            return state.getAnalogOutputSignal(world, pos, targetSide) / 15.0
        } else if (state.isSolidRender) {
            return 1.0
        } else if (state.isAir) {
            return 0.0
        } else {
            return 1.0/3.0
        }
    }

    private fun scanTileEntity(scannedCoord: Coordonate, te: BlockEntity, targetSide: net.minecraft.core.Direction): Double? {
        val world = scannedCoord.world()
        val pos = scannedCoord.pos()
        // 1.12: the side's fluid handler capability; 26.x: the NeoForge transfer API
        val fluids = world.getCapability(Capabilities.Fluid.BLOCK, pos, targetSide)
        if (fluids != null && fluids.size() > 0) {
            return (0 until fluids.size()).sumOf {
                val cap = fluids.getCapacityAsLong(it, fluids.getResource(it))
                if (cap > 0) fluids.getAmountAsLong(it).toDouble() / cap else 0.0
            } / fluids.size()
        } else if (te is Container) {
            // 1.12 divided by inventoryStackLimit (64); 26.x containers allow 99, so use the vanilla comparator's
            // per-slot limit min(container, item) instead (the same as 1.12 for 64-stacking items).
            val slots = if (te is WorldlyContainer) te.getSlotsForFace(targetSide).toList() else (0 until te.containerSize).toList()
            if (slots.isEmpty()) return 0.0
            val sum = when (mode) {
                ScanMode.SIMPLE -> slots.sumOf {
                    val stack = te.getItem(it)
                    if (stack.isEmpty) 0.0 else stack.count.toDouble() / minOf(te.maxStackSize, stack.maxStackSize)
                }

                ScanMode.SLOTS -> slots.count { !te.getItem(it).isEmpty }.toDouble()
            }
            return sum / slots.size
        } else {
            return null
        }
    }

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
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
        return "Mode: ${tr(mode.name.lowercase().replaceFirstChar { it.uppercase() })}, Value: ${Utils.plotPercent("", outputProcess.outputNormalized)}"
    }

    override fun thermoMeterString() = ""

    override fun initialize() {
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeByte(mode.value.toInt())
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        nbt.putByte("mode", mode.value)
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        mode = ScanMode.fromByte(nbt.getByteOr("mode", 0.toByte()))!!
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

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor = ElnDeviceRegistry.signalCableDescriptor.render
}

