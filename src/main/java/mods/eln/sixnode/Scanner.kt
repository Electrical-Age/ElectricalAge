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
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.IFluidHandler

/**
 * A comparator-alike. It doesn't "compare" anything, though.
 */
class ScannerDescriptor(name: String, obj: Obj3D) : SixNodeDescriptor(name, ScannerElement::class.java, ScannerRender::class.java) {

    val main = obj.getPart("main")!!

    fun draw() {
        main.draw()
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

enum class ScanMode {
    SIMPLE, SLOTS
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
        val targetSide: ForgeDirection = appliedLRDU.inverse.toForge()
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        var out = -1.0
        if (te != null) {
            out = scanTileEntity(te, targetSide)
        }
        if (out == -1.0) {
            out = scanBlock(scannedCoord, targetSide)
        }
        outputProcess.outputNormalized = out
    }

    init {
        electricalLoadList.add(output)
        electricalComponentList.add(outputProcess)
        slowProcessList.add(updater)
    }

    private fun scanBlock(scannedCoord: Coordonate, targetSide: ForgeDirection): Double {
        val block = scannedCoord.block
        if (block.hasComparatorInputOverride()) {
            return block.getComparatorInputOverride(scannedCoord.world(), scannedCoord.x, scannedCoord.y, scannedCoord.z, targetSide.ordinal) / 15.0
        } else if (block.isOpaqueCube) {
            return 1.0
        } else if (block.isAir(scannedCoord.world(), scannedCoord.x, scannedCoord.y, scannedCoord.z)) {
            return 0.0
        } else {
            return 1.0/3.0
        }
    }

    private fun scanTileEntity(te: TileEntity, targetSide: ForgeDirection): Double {
        if (te is IFluidHandler) {
            val info = te.getTankInfo(targetSide)
            return info.sumByDouble {
                (it.fluid?.amount ?: 0).toDouble() / it.capacity
            } / info.size
        } else if (te is ISidedInventory) {
            var sum = 0
            var limit = 0
            val slots = te.getAccessibleSlotsFromSide(targetSide.ordinal)
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
        } else if (te is IInventory) {
            val sum = when (mode) {
                ScanMode.SIMPLE -> (0..te.sizeInventory - 1).sumBy {
                    te.getStackInSlot(it)?.stackSize ?: 0
                }.toDouble()

                ScanMode.SLOTS -> (0..te.sizeInventory - 1).count {
                    (te.getStackInSlot(it)?.stackSize ?: 0) > 0
                }.toDouble() * te.inventoryStackLimit
            }
            return sum / te.inventoryStackLimit / te.sizeInventory
        } else {
            return -1.0
        }
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (super.onBlockActivated(entityPlayer, side, vx, vy, vz)) return true
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

    override fun multiMeterString() = "Mode: ${tr(mode.name.toLowerCase().capitalize())}, Value: ${Utils.plotPercent("", outputProcess.outputNormalized)}"

    override fun thermoMeterString() = null

    override fun initialize() {
    }
}


class ScannerRender(entity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(entity, side, descriptor) {
    val desc = descriptor as ScannerDescriptor

    override fun draw() {
        super.draw()
        front.glRotateOnX()
        desc.draw()
    }

    override fun getCableRender(lrdu: LRDU?): CableRenderDescriptor = Eln.instance.signalCableDescriptor.render
}
