package mods.eln.mechanical

import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidRegistry

open class SteamTurbineDescriptor(baseName: String, obj: Obj3D) :
        SimpleShaftDescriptor(baseName, TurbineElement::class, TurbineRender::class, EntityMetaTag.Fluid) {
    // Overall time for steam input changes to take effect, in seconds.
    open val inertia = 20f
    // Optimal steam consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    open val fluidConsumption = 7200f
    // Joules per mB, at optimal turbine speed.
    // Computed to equal what you'd get from Railcraft steam engines, plus a small
    // bonus because you're using Electrical Age you crazy person you.
    // This pretty much fills up a VHV line. The generator drag gives us a bit of leeway.
    // TODO: This should be tied into the config options.
    open val fluidPower = 2.2f
    // Well, this is a *steam* turbine.
    // TODO: Factor out into an abstract turbine descriptor.
    open val fluidDescription = "Steam"
    open val fluidType = "steam"
    // Width of the efficiency curve.
    // <1 means "Can't be started without power".
    open val efficiencyCurve = 1.1

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
        list.add("Converts steam into mechanical energy.")
        list.add("Nominal usage ->")
        list.add("  ${fluidDescription} input: ${fluidConsumption} mB/s")
        list.add(Utils.plotPower("  Power out: ", (fluidConsumption * fluidPower).toDouble()))
        list.add(Utils.plotRads("  Optimal rads: ", optimalRads))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

class GasTurbineDescriptor(basename: String, obj: Obj3D) :
        SteamTurbineDescriptor(basename, obj) {
    // The main benefit of gas turbines.
    override val inertia = 5f;
    // 1B in 3 minutes at max power.
    override val fluidConsumption = 1000f / 180f
    // Computed to equal a single 27LP Railcraft boiler. This makes gas turbines slightly less
    // efficient than going through steam, though at some point in the future you'll be able
    // to recover the rest by using the heat output. Which doesn't exist right now.
    // In short, it's 8.1kW.
    override val fluidPower = 8100f / fluidConsumption
    override val fluidDescription = "Gasoline"
    override val fluidType = "fuel"
    override val efficiencyCurve = 0.9
}

class TurbineElement(node : TransparentNode, desc_ : TransparentNodeDescriptor) :
        SimpleShaftElement(node, desc_) {
    val desc = desc_ as SteamTurbineDescriptor
    val fluid = FluidRegistry.getFluid(desc.fluidType) ?: FluidRegistry.getFluid("lava")

    val tank = TransparentNodeElementFluidHandler(1000)
    var steamRate = 0f
    var efficiency = 0.0
    val turbineSlowProcess = TurbineSlowProcess()

    internal val throttle = NbtElectricalGateInput("throttle")

    inner class TurbineSlowProcess: IProcess, INBTTReady {
        val rc = RcInterpolator(desc.inertia)

        // Fixup for only being able to grab 1 mB at a time.
        // This represents fluid that was drained in a previous tick, but not used.
        var consumptionFixup = 0f

        override fun process(time: Double) {
            val target = (desc.fluidConsumption * time * throttle.normalized).toFloat()
            val drain = Math.ceil((target - consumptionFixup).toDouble()).toFloat()
            val drained = tank.drain(ForgeDirection.DOWN, drain.toInt(), true)?.amount?.toFloat() ?: 0f
            val usable = drained + consumptionFixup
            val actual = if (usable < target) usable else target
            consumptionFixup = Math.max(0f, usable - target)

            rc.target = actual / time.toFloat()
            rc.step(time.toFloat())
            steamRate = rc.get()
            efficiency = Math.pow(Math.cos((shaft.rads - desc.optimalRads) / (desc.optimalRads * desc.efficiencyCurve) * Math.PI / 2), 3.0)
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
        slowProcessList.add(turbineSlowProcess)
        tank.setFilter(fluid)

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

    override fun thermoMeterString(side: Direction?) =  Utils.plotPercent(" Eff:", efficiency) + steamRate.toString() + "mB/s"

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
