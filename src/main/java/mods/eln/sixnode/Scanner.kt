package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.block.BlockRedstoneComparator
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ISidedInventory
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

}


class ScannerElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(sixNode, side, descriptor) {

    val output = NbtElectricalGateOutput("signal")
    val outputProcess = NbtElectricalGateOutputProcess("signalP", output)

    val forgeSide: ForgeDirection = side.inverse.toForge()

    val updater = IProcess {
        val scannedCoord = Coordonate(coordonate).apply {
            move(side.applyLRDU(front))
        }
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        val out = if (te != null) {
            scanTileEntity(te)
        } else {
            scanBlock(scannedCoord)
        }
        outputProcess.outputNormalized = out
    }

    private fun scanBlock(scannedCoord: Coordonate): Double {
        val block = scannedCoord.block
        if (block.hasComparatorInputOverride()) {
            return block.getComparatorInputOverride(coordonate.world(), coordonate.x, coordonate.y, coordonate.z, forgeSide.ordinal) / 15.0
        } else if (block.isOpaqueCube) {
            return 1.0
        } else if (block.isAir(coordonate.world(), coordonate.x, coordonate.y, coordonate.z)) {
            return 0.0
        } else {
            return 1.0/3.0
        }
    }

    private fun scanTileEntity(te: TileEntity): Double {
        if (te is IFluidHandler) {
            val info = te.getTankInfo(forgeSide)
            return info.sumByDouble {
                (it.fluid?.amount ?: 0).toDouble() / it.capacity
            } / info.size
        } else if (te is ISidedInventory) {
            var sum = 0
            var limit = 0
            te.getAccessibleSlotsFromSide(forgeSide.ordinal).forEach {
                sum += te.getStackInSlot(it)?.stackSize ?: 0
                limit += te.inventoryStackLimit
            }
            return sum.toDouble() / limit
        } else if (te is IInventory) {
            val sum = (0..te.sizeInventory - 1).sumBy {
                te.getStackInSlot(it)?.stackSize ?: 0
            }
            return sum.toDouble() / te.inventoryStackLimit / te.sizeInventory
        } else {
            return 2.0/3.0
        }
    }

    init {
        electricalLoadList.add(output)
        electricalComponentList.add(outputProcess)
        slowProcessList.add(updater)
    }

    override fun getElectricalLoad(lrdu: LRDU?) = output
    override fun getThermalLoad(lrdu: LRDU?) = null

    override fun getConnectionMask(lrdu: LRDU) = when (lrdu) {
        front.inverse() -> NodeBase.maskElectricalOutputGate
        else -> 0
    }

    override fun multiMeterString() = Utils.plotPercent("", outputProcess.outputNormalized)

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
