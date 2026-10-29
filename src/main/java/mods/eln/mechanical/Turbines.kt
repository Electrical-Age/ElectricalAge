package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.fluid.FuelRegistry
import mods.eln.fluid.PreciseElementFluidHandler
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.published
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.IProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream

abstract class TurbineDescriptor(baseName: String, obj: Obj3D) :
    SimpleShaftDescriptor(baseName, TurbineElement::class, null) {
    // Overall time for steam input changes to take effect, in seconds.
    abstract val inertia: Float
    // Optimal fluid consumed per second, mB.
    // Computed to equal a single 36LP Railcraft boiler, or half of a 36HP.
    abstract val fluidConsumption: Float
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
    // Power stats
    val power: List<Double> by lazy {
        fluidTypes.map { FuelRegistry.heatEnergyPerMilliBucket(it) * fluidConsumption }
    }
    val maxFluidPower: Double by lazy {
        power.max() ?: 0.0
    }
    val minFluidPower: Double by lazy {
        power.min() ?: 0.0
    }

    override val obj = obj
    override val static = arrayOf(
        obj.getPart("Cowl"),
        obj.getPart("Stand")
    )
    override val rotating = arrayOf(
        obj.getPart("Shaft"),
        obj.getPart("Fan")
    )

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, par4: Boolean) {
        list.add("Converts ${fluidDescription} into mechanical energy.")
        list.add("Nominal usage ->")
        list.add("  ${fluidDescription.capitalize()} input: ${fluidConsumption} mB/s")
        if (power.isEmpty()) {
            list.add("  No valid fluids for this turbine!")
        } else if (power.size == 1) {
            list.add(Utils.plotPower("  Power out: ", power[0]))
        } else {
            list.add("  Power out: ${Utils.plotPower(minFluidPower)}- ${Utils.plotPower(maxFluidPower)}")
        }
        list.add(Utils.plotRads("  Optimal rads: ", optimalRads))
        list.add(Utils.plotRads("Max rads:  ", absoluteMaximumShaftSpeed))
    }
}

/**
 * The shaft Steam Turbine (ShaftSteamTurbineElement): steam by EA pipe in on top and out of its back, work onto the
 * shaft (Baughn 2026-10-29). The 1.12 model until its own.
 */
class SteamTurbineDescriptor(baseName: String, obj: Obj3D, val spec: SteamTurbineSpec) :
    SimpleShaftDescriptor(baseName, ShaftSteamTurbineElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Cowl"), obj.getPart("Stand"))
    override val rotating = arrayOf(obj.getPart("Shaft"), obj.getPart("Fan"))
    override val sound = "eln:steam_turbine"

    fun spec() = spec

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, par4: Boolean) {
        val p = spec.nominalPa / 1e5
        if (spec.wetBraking) {
            list.add("The low-pressure stage: an HP Steam Turbine's exhaust in by pipe on top, out of its back")
            list.add("into a Plant Steam Condenser: useless venting to air.")
            list.add(Utils.plotPower("Up to ", spec.nominalP) + String.format(" onto the shaft from %.0f g/s of %.1f bar steam into a %.1f bar vacuum.",
                spec.nominalKgPerS * 1000, p, spec.ratedExhaustPa / 1e5))
            list.add("Wet steam brakes it and knocks; if it lasts at speed, it strips its blades.")
        } else {
            list.add("Steam in by pipe on top, out of its back: piped on (an LP turbine, a heater) or into the air.")
            list.add(Utils.plotPower("Up to ", spec.nominalP) + String.format(" onto the shaft from %.0f g/s of %.0f bar steam venting to air;",
                spec.nominalKgPerS * 1000, p))
            list.add("more with a vacuum behind it. Generators on the shaft take the power.")
            list.add("Water in the steam strips its blades.")
        }
        list.add(Utils.plotRads("Automatic: governor full open to ", ShaftSteamTurbineElement.RATED_RADS) + ", shut at +5%; trips over +10%.")
        list.add("Manual or external control turns the governor and the overspeed trip off: your own safeties then.")
        list.add("Its screen: throttle, or a regulator holding the inlet pressure; a signal on its front for external control.")
        list.add(String.format("Its casing bursts over %.0f bar.", spec.burstPa / 1e5))
        if (!spec.wetBraking) list.add("Might want a boiler with that.")   // (Baughn)
    }
}

/** Its render: the shaft, the steam's roar by flow, and the controls for the screen (SteamGeneratorScreen). */
class SteamTurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc),
    mods.eln.oil.SteamValveControl.Rendered {
    override val cableRender = Eln.stdCableRenderSignal
    val controls = mods.eln.oil.SteamValveControl.Render(this, (desc as SteamTurbineDescriptor).spec.targetMaxBar)

    override fun controls() = controls

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
        controls.read(stream)
    }
}

class GasTurbineDescriptor(basename: String, obj: Obj3D) :
    TurbineDescriptor(basename, obj) {
    // The main benefit of gas turbines.
    override val inertia = 5f
    // Provides about 8kW of power, given gasoline.
    // Less dense fuels will be proportionally less effective.
    override val fluidConsumption = 4f
    override val fluidDescription = "gasoline"
    // It runs on puns.
    override val fluidTypes = FuelRegistry.gasolineList + FuelRegistry.gasList
    // Gas turbines are finicky about turbine speed.
    override val efficiencyCurve = 0.5f
    // And need to be spun up before working.
    override val efficiencyCutoff = 0.5f
    override val sound = "eln:gas_turbine"
}

class TurbineElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as TurbineDescriptor
    /** H = 1 s of its best power at 800 rad/s: a gas turbine is light (fast to start, quick to answer). */
    override val shaftInertia = inertiaOf(1.0, desc.maxFluidPower)

    val tank = PreciseElementFluidHandler(desc.fluidConsumption.toInt())
    var fluidRate = 0f
    var efficiency = 0f
    val turbineSlowProcess = TurbineSlowProcess()

    internal val throttle = NbtElectricalGateInput("throttle")

    internal var volume: Float by published(0f)

    inner class TurbineSlowProcess() : IProcess, INBTTReady {
        val rc = RcInterpolator(desc.inertia)

        override fun process(time: Double) {
            // Do anything at all?
            val target: Float
            val computedEfficiency = Math.pow(Math.cos((shaft.rads - desc.optimalRads) / (desc.optimalRads * desc.efficiencyCurve) * Math.PI / 2), 3.0)
            if (computedEfficiency >= desc.efficiencyCutoff) {
                efficiency = computedEfficiency.toFloat()
                val th = if (throttle.connectedComponents.count() > 0) throttle.normalized else 1.0
                target = (desc.fluidConsumption * th).toFloat()
            } else {
                efficiency = 0f
                target = 0f
            }

            val drained = tank.drain(target * time).toFloat()

            rc.target = (drained / time).toFloat()
            rc.step(time.toFloat())
            fluidRate = rc.get()

            val power = fluidRate * tank.heatEnergyPerMilliBucket * efficiency
            // (with a torque limit: twice its best power at its optimal speed)
            shaft.drive(power.toDouble(), time, 2 * desc.maxFluidPower / desc.optimalRads)

            volume = power / desc.maxFluidPower.toFloat()
        }

        override fun readFromNBT(nbt: CompoundTag?, str: String?) {
            rc.readFromNBT(nbt, str)
        }

        override fun writeToNBT(nbt: CompoundTag?, str: String?) {
            rc.writeToNBT(nbt, str)
        }
    }

    init {
        tank.setFilter(FuelRegistry.fluidListToFluids(desc.fluidTypes))
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

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun thermoMeterString(side: Direction?) = Utils.plotPercent(" Eff:", efficiency.toDouble()) + fluidRate.toString() + "mB/s"

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        tank.writeToNBT(nbt, "tank")
        turbineSlowProcess.writeToNBT(nbt, "proc")
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        tank.readFromNBT(nbt, "tank")
        turbineSlowProcess.readFromNBT(nbt, "proc")
    }

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put("Speed", Utils.plotRads("", shaft.rads))
        info.put("Energy", Utils.plotEnergy("", shaft.energy))
        if (Eln.wailaEasyMode) {
            info.put("Efficency", Utils.plotPercent("", efficiency.toDouble()))
            info.put("Fuel usage", Utils.plotBuckets("", fluidRate / 1000.0) + "/s")
        }
        return info
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(volume)
    }
}

class TurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender = Eln.stdCableRenderSignal

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
    }
}

