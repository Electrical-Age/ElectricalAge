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

/**
 * The shaft Steam Turbine (ShaftSteamTurbineElement): steam by EA pipe in on top and out of its back, work onto the
 * shaft (Baughn 2026-10-29). Models: tools/models/shaftturbines.py (`main`, `rotor`, `valve`: the governor valve).
 */
class SteamTurbineDescriptor(baseName: String, obj: Obj3D, val spec: SteamTurbineSpec) :
    SimpleShaftDescriptor(baseName, ShaftSteamTurbineElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("main"))
    override val rotating = arrayOf(obj.getPart("rotor"))

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
        list.add("Placed with its shaft along your view; it turns its exhaust toward a condenser or pipe beside it.")
        if (!spec.wetBraking) list.add("Might want a boiler with that.")   // (Baughn)
    }
}

/**
 * Its render: the shaft, the governor valve's stem by its opening, and its sounds (MachineSounds.turbine: steam hiss by
 * flow, rotor tones by speed, a whistle over 105 %); the controls for the screen (SteamGeneratorScreen).
 */
class SteamTurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc),
    mods.eln.oil.SteamValveControl.Rendered {
    override val cableRender = Eln.stdCableRenderSignal
    val controls = mods.eln.oil.SteamValveControl.Render(this, (desc as SteamTurbineDescriptor).spec.targetMaxBar)
    private val sdesc = desc as SteamTurbineDescriptor
    val valve = SlewLimiter(0.5f)
    var wet = false

    init {
        mods.eln.sound.MachineSounds.turbine(coordonate(), { rads / ShaftSteamTurbineElement.RATED_RADS }, { volumeSetting.position.toDouble() }, false, 1f)
            .forEach { addLoopedSound(it) }
    }

    override fun controls() = controls

    override fun draw() {
        draw { sdesc.drawValve(valve.position) }
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        valve.step(deltaT)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
        controls.read(stream)
        valve.target = stream.readFloat()
        wet = stream.readBoolean()
    }
}

/**
 * The Gas Turbine (GasTurbineElement; Baughn 2026-10-29, the shaft redo): a light, fast peaker. Model:
 * tools/models/shaftgas.py (`main`, `rotor`: its compressor behind the intake, `valve`: the fuel valve).
 */
class GasTurbineDescriptor(basename: String, obj: Obj3D) :
    SimpleShaftDescriptor(basename, GasTurbineElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("main"))
    override val rotating = arrayOf(obj.getPart("rotor"))

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, par4: Boolean) {
        list.add(Utils.plotPower("A fast peaker: up to ", GasTurbineElement.NOMINAL_P) + String.format(" onto the shaft at %.0f%% of its fuel's heat.",
            GasTurbineElement.ETA * 100))
        list.add("Fuel (gasoline, natural gas, diesel, ...) by any pipe or fluid capability: no pressure needed.")
        list.add(String.format("It can't start itself: spin it over %.0f rad/s (a Shaft Motor on its shaft), then it lights.",
            GasTurbineElement.CUTOFF * GasTurbineElement.RATED_RADS))
        list.add(String.format("Hot exhaust (%.0f C) out of its back: pipe it to a heat exchanger, or it vents.", GasTurbineElement.EXHAUST_T - 273.15))
        list.add(Utils.plotRads("Automatic: governor full open to ", GasTurbineElement.RATED_RADS) + ", shut at +5%; trips over +10%.")
        list.add("A signal on its front sets the fuel valve by hand: no governor, no trip: your safeties then.")
        list.add("Placed with its shaft along your view; it turns its exhaust toward an exchanger or pipe beside it.")
    }
}

/** Its render: the shaft, the fuel valve's stem, its roar by power and pitch by speed (MachineSounds.turbine). */
class GasTurbineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : ShaftRender(entity, desc) {
    override val cableRender = Eln.stdCableRenderSignal
    private val gdesc = desc as GasTurbineDescriptor
    val valve = SlewLimiter(0.5f)
    var burning = false

    init {
        mods.eln.sound.MachineSounds.turbine(coordonate(), { rads / GasTurbineElement.RATED_RADS },
            { if (burning) Math.max(0.15, volumeSetting.position.toDouble()) else 0.0 }, true, 1f)
            .forEach { addLoopedSound(it) }
    }

    override fun draw() {
        draw { gdesc.drawValve(valve.position) }
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        valve.step(deltaT)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        volumeSetting.target = stream.readFloat()
        valve.target = stream.readFloat()
        burning = stream.readBoolean()
    }
}
