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
 * shaft (Baughn 2026-10-29). Models: mods.eln.cad.models.ShaftTurbines (`main`, `rotor`, `valve`: the governor valve).
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
            list.add(Utils.plotPower("Low-pressure shaft steam turbine: up to ", spec.nominalP).trim() + String.format(" at %.1f bar.", p))
            list.add(String.format("Casing bursts over %.0f bar. Dry steam only.", spec.burstPa / 1e5))
            mods.eln.client.ItemTooltips.detail(list,
                "Requires vacuum exhaust (Plant Steam Condenser); wet steam brakes and damages blades.",
                Utils.plotRads("Governor: rated ", ShaftSteamTurbineElement.RATED_RADS).trim() + " (shuts at +5%, trips over +10%).",
                "Screen: throttle or inlet pressure regulator. Front signal overrides governor & trip.")
        } else {
            list.add(Utils.plotPower("High-pressure shaft steam turbine: up to ", spec.nominalP).trim() + String.format(" at %.0f bar.", p))
            list.add(String.format("Casing bursts over %.0f bar. Dry steam only.", spec.burstPa / 1e5))
            list.add("Might want a boiler with that.")   // (Baughn)
            mods.eln.client.ItemTooltips.detail(list,
                Utils.plotRads("Governor: rated ", ShaftSteamTurbineElement.RATED_RADS).trim() + " (shuts at +5%, trips over +10%).",
                "Screen: throttle or inlet pressure regulator. Front signal overrides governor & trip.")
        }
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
 * mods.eln.cad.models.ShaftGas (`main`, `rotor`: its compressor behind the intake, `valve`: the fuel valve).
 */
class GasTurbineDescriptor(basename: String, obj: Obj3D) :
    SimpleShaftDescriptor(basename, GasTurbineElement::class, null) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("main"))
    override val rotating = arrayOf(obj.getPart("rotor"))

    override fun addInformation(stack: ItemStack, player: Player?, list: MutableList<String>, par4: Boolean) {
        list.add(Utils.plotPower("Fast-response shaft turbine: up to ", GasTurbineElement.NOMINAL_P).trim() + String.format(" (%.0f%% efficiency).", GasTurbineElement.ETA * 100))
        list.add(String.format("Burns gasoline, natural gas, or diesel. Needs spin-up over %.0f rad/s to ignite.",
            GasTurbineElement.CUTOFF * GasTurbineElement.RATED_RADS))
        list.add(String.format("Exhaust: %.0f °C.", GasTurbineElement.EXHAUST_T - 273.15))
        mods.eln.client.ItemTooltips.detail(list,
            Utils.plotRads("Governor: rated ", GasTurbineElement.RATED_RADS).trim() + " (shuts at +5%, trips over +10%).",
            "Front signal: manual fuel valve (disables governor & trip).",
            "Heavy or crude fuels foul or choke it.")
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
