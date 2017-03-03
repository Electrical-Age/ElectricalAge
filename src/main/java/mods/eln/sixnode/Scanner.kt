package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
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

    val scannedCoord = Coordonate(1, 0, 0, 0).apply {
        applyTransformation(side.down(), coordonate)
    }

    val updater = IProcess {
        val te = scannedCoord.tileEntity
        // TODO: Throttling.
        val out: Double
        if (te is IFluidHandler) {
            val info = te.getTankInfo(side.inverse.toForge())
            out = info.sumByDouble {
                (it.fluid?.amount ?: 0).toDouble() / it.capacity
            } / info.size
        } else if (te is IInventory) {
            val sum = (0..te.sizeInventory - 1).sumBy { te.getStackInSlot(it)?.stackSize ?: 0 }
            out = sum.toDouble() / te.inventoryStackLimit / te.sizeInventory
        } else {
            out = scannedCoord.block.lightValue / 15.0
        }
        outputProcess.outputNormalized = out
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

    override fun multiMeterString() = ""

    override fun thermoMeterString() = ""

    override fun initialize() {
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false
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
