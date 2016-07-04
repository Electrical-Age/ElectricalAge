package mods.eln.mechanical

import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry

abstract class TurbineDescriptor(baseName: String, obj: Obj3D) :
        SimpleShaftDescriptor(baseName, TurbineElement::class, TurbineRender::class, EntityMetaTag.Fluid) {
    // Overall time for steam input changes to take effect, in seconds.
    abstract val inertia: Float
    // Optimal steam consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    abstract val fluidConsumption: Float
    // Joules per mB, at optimal turbine speed.
    // Computed to equal what you'd get from Railcraft steam engines, plus a small
    // bonus because you're using Electrical Age you crazy person you.
    // This pretty much fills up a VHV line. The generator drag gives us a bit of leeway.
    abstract val fluidPower: Float
    // How we describe the fluid in the tooltip.
    abstract val fluidDescription: String
    // The fluids actually accepted.
    abstract val fluidTypes: Array<String>
    // Width of the efficiency curve.
    // <1 means "Can't be started without power".
    abstract val efficiencyCurve: Float
    // If efficiency is below this fraction, do nothing.
    open val efficiencyCutoff = 0f
    val optimalRads = absoluteMaximumShaftSpeed * 0.8f

    override val obj = obj
    override val static = arrayOf(
            obj.getPart("Cowl"),
            obj.getPart("Stand")
    )
    override val rotating = arrayOf(
            obj.getPart("Shaft"),
            obj.getPart("Fan")
    )

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add("Converts ${fluidDescription} into mechanical energy.")
        list.add("Nominal usage ->")
        list.add("  ${fluidDescription.capitalize()} input: ${fluidConsumption} mB/s")
        list.add(Utils.plotPower("  Power out: ", (fluidConsumption * fluidPower).toDouble()))
        list.add(Utils.plotRads("  Optimal rads: ", optimalRads))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class SteamTurbineDescriptor(baseName: String, obj: Obj3D) :
        TurbineDescriptor(baseName, obj) {
    // Steam turbines are for baseload.
    override val inertia = 20f
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    override val fluidConsumption = 7200f
    // Computed to equal what you'd get from Railcraft steam engines, plus a small
    // bonus because you're using Electrical Age you crazy person you.
    // This pretty much fills up a VHV line. The generator drag gives us a bit of leeway.
    // TODO: This should be tied into the config options.
    override val fluidPower = 2.2f
    override val fluidDescription = "steam"
    override val fluidTypes = arrayOf("steam")
    // Steam turbines can, just barely, be started without power.
    override val efficiencyCurve = 1.1f
}

class GasTurbineDescriptor(basename: String, obj: Obj3D) :
        TurbineDescriptor(basename, obj) {
    // The main benefit of gas turbines.
    override val inertia = 5f;
    // Going with 8kW for this thing.
    val targetPower = 8000f
    // Computed to equal a single 18LP Railcraft boiler. This makes it 80% as efficient
    // as the steam turbine. At some point in the future you'll be able to recover the rest
    // by using the heat output, which doesn't currently exist.
    override val fluidPower = 1280f  // J/mB
    override val fluidConsumption = targetPower / fluidPower
    // Computed to equal a single 18LP Railcraft boiler. This makes gas turbines slightly less
    // efficient than going through steam, though at some point in the future you'll be able
    // to recover the rest by using the heat output. Which doesn't exist right now. To be
    // precise, 80% as efficient.
    override val fluidDescription = "gasoline"
    override val fluidTypes = gasolineList + gasList
    // Gas turbines have a fairly wide efficiency range.
    override val efficiencyCurve = 2.0f
    // But need to be spun up before working.
    override val efficiencyCutoff = 0.5f
}

class TurbineElement(node : TransparentNode, desc_ : TransparentNodeDescriptor) :
        SimpleShaftElement(node, desc_) {
    val desc = desc_ as TurbineDescriptor

    val tank = TransparentNodeElementFluidHandler(1000)
    var steamRate = 0f
    var efficiency = 0f
    val turbineSlowProcess = TurbineSlowProcess()

    internal val throttle = NbtElectricalGateInput("throttle")

    inner class TurbineSlowProcess: IProcess, INBTTReady {
        val rc = RcInterpolator(desc.inertia)

        // Fixup for only being able to grab 1 mB at a time.
        // This represents fluid that was drained in a previous tick, but not used.
        var consumptionFixup = 0f

        override fun process(time: Double) {
            // Do anything at all?
            val target: Float
            val computedEfficiency = Math.pow(Math.cos((shaft.rads - desc.optimalRads) / (desc.optimalRads * desc.efficiencyCurve) * Math.PI / 2), 3.0)
            if (computedEfficiency >= desc.efficiencyCutoff) {
                efficiency = computedEfficiency.toFloat()
                val th = if (throttle.connectedComponents.count() > 0) throttle.normalized else 1.0
                target = (desc.fluidConsumption * time * th).toFloat()
            } else {
                efficiency = 0f
                target = 0f
            }

            val drain = Math.ceil((target - consumptionFixup).toDouble()).toFloat()
            val drained = tank.drain(ForgeDirection.DOWN, drain.toInt(), true)?.amount?.toFloat() ?: 0f
            val usable = drained + consumptionFixup
            val actual = if (usable < target) usable else target
            consumptionFixup = Math.max(0f, usable - target)

            rc.target = actual / time.toFloat()
            rc.step(time.toFloat())
            steamRate = rc.get()

            val power = steamRate * desc.fluidPower * efficiency
            shaft.energy += power * time.toFloat()
        }

        override fun readFromNBT(nbt: NBTTagCompound?, str: String?) {
            rc.readFromNBT(nbt, str)
        }

        override fun writeToNBT(nbt: NBTTagCompound?, str: String?) {
            rc.writeToNBT(nbt, str)
        }
    }

    init {
        val fluids = fluidListToFluids(if (desc.fluidTypes.isEmpty()) arrayOf("lava") else desc.fluidTypes)
        tank.setFilter(fluids)
        slowProcessList.add(turbineSlowProcess)
        electricalLoadList.add(throttle)
    }

    override fun getFluidHandler() = tank

    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = throttle
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?) = null
    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu == LRDU.Down && (side == front || side == front.back())) return NodeBase.maskElectricalGate
        return 0
    }
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun thermoMeterString(side: Direction?) =  Utils.plotPercent(" Eff:", efficiency.toDouble()) + steamRate.toString() + "mB/s"

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
        turbineSlowProcess.writeToNBT(nbt, "proc")
    }
    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
        turbineSlowProcess.readFromNBT(nbt, "proc")
    }
}

class TurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): ShaftRender(entity, desc) {
    override fun draw() {
        super.draw()
        // TODO: Actually, no, do wire drawing in the base class. And stuff.
    }
}
