package mods.eln.transparentnode

import mods.eln.gl.GL11

import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.series.SerieEE
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.Failure
import mods.eln.sim.process.heater.ResistorHeatThermalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.resistor.ResistorContainer
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor
import net.minecraft.world.entity.player.Player
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import java.io.DataInputStream
import java.io.DataOutputStream

// TODO: Make the whole thing brighter when it heats up, not just redder.

class LargeRheostatDescriptor(name: String, val dissipator: ThermalDissipatorPassiveDescriptor, val cable: ElectricalCableDescriptor, val series: SerieEE) :
    TransparentNodeDescriptor(name, LargeRheostatElement::class.java, null) {

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    fun getRsValue(inventory: Container): Double {
        val core = inventory.getItem(ResistorContainer.coreId)
        if (core.isEmpty) return series.getValue(0)

        return series.getValue(core.count)
    }

    fun draw(position: Float = 0f) {
        dissipator.draw()
        GL11.glRotatef((1f - position) * 300f, 0f, 1f, 0f)
        dissipator.obj.getPart("wiper")?.draw()
    }


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)
    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    override fun hasItemDraw() = true

    override fun drawItem(entity: Boolean) = draw()
}

class LargeRheostatElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    TransparentNodeElement(node, desc_) {
    val desc = desc_ as LargeRheostatDescriptor

    var nominalRs = 1.0
    private var inventory = TransparentNodeElementInventory(2, 64, this)

    val aLoad = NbtElectricalLoad("aLoad")
    val bLoad = NbtElectricalLoad("bLoad")
    val resistor = Resistor(aLoad, bLoad).apply { setR(nominalRs) }

    val control = NbtElectricalGateInput("control")
    val controlProcess = ControlProcess()

    val thermalLoad = NbtThermalLoad("thermalLoad")
    val heater = ResistorHeatThermalLoad(resistor, thermalLoad)
    val thermalWatchdog = ThermalLoadWatchDog()

    init {
        // Electrics
        grounded = false
        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(resistor)
        electricalLoadList.add(control)
        slowProcessList.add(controlProcess)
        // Heating
        thermalLoadList.add(thermalLoad)
        thermalFastProcessList.add(heater)
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog.set(thermalLoad).setTMax(desc.dissipator.warmLimit)
            .set(Failure(this).machine())
    }

    inner class ControlProcess() : IProcess {
        var lastC = -1000.0
        var lastH = -1000.0

        override fun process(time: Double) {
            val desiredRs = (control.normalized + 0.01) / 1.01 * nominalRs
            if (desiredRs > lastC * 1.01 || desiredRs < lastC * 0.99) {
                resistor.r = desiredRs
                lastC = desiredRs
                needPublish()
            }
            if (thermalLoad.Tc > lastH * 1.05 || thermalLoad.Tc < lastH * 0.95) {
                lastH = thermalLoad.Tc
                needPublish()
            }
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.right() -> aLoad
            front.left() -> bLoad
            front -> control
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        if (lrdu != LRDU.Down) return null
        // This one's insulated, since its max heat is 1000C.
        return when (side) {
            front.back() -> thermalLoad
            else -> null
        }
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front -> NodeBase.maskElectricalInputGate
            front.back() -> NodeBase.maskThermal
            else -> NodeBase.maskElectricalPower
        }
    }

    fun setupPhysical() {
        nominalRs = desc.getRsValue(inventory)
        controlProcess.process(0.0)
    }

    override fun inventoryChange(inventory: Container?) {
        super.inventoryChange(inventory)
        setupPhysical()
    }

    override fun multiMeterString(side: Direction): String {
        val u = -Math.abs(aLoad.u - bLoad.u)
        val i = Math.abs(resistor.i)
        return Utils.plotOhm(Utils.plotUIP(u, i), resistor.r) + Utils.plotPercent("C", control.normalized)
    }

    override fun thermoMeterString(side: Direction) =
        Utils.plotCelsius("T: ", thermalLoad.Tc) + Utils.plotPower("P: ", thermalLoad.power)

    override fun initialize() {
        desc.dissipator.applyTo(thermalLoad)
        aLoad.rs = MnaConst.noImpedance
        bLoad.rs = MnaConst.noImpedance
        setupPhysical()
        connect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(thermalLoad.Tc.toFloat())
        stream.writeFloat(control.normalized.toFloat())
    }

    override fun hasGui() = true
    override fun getInventory() = inventory
    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    override fun getWaila(): Map<String, String> = mutableMapOf(
        Pair(I18N.tr("Resistance"), Utils.plotOhm("", resistor.r)),
        Pair(I18N.tr("Temperature"), Utils.plotCelsius("", thermalLoad.t)),
        Pair(I18N.tr("Power loss"), Utils.plotPower("", resistor.p))
    )
}

class LargeRheostatRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) :
    TransparentNodeElementRender(entity, desc) {
    val desc = desc as LargeRheostatDescriptor


    override fun getInventory(): Container {
        return inventory
    }

    val baseColor = BlackBodyColor(1f, 1f, 1f)
    var color = BlackBodyTemperature(0f)
    val positionAnimator = SlewLimiter(0.3f)

    init {
        positionAnimator.target = -1f
    }

    override fun draw() {
        front.glRotateZnRef()
        // TODO: Get this thing *really* glowing.
        // glColor doesn't let me exceed 1.0, the way I'd quite like to do.
        GL11.glColor3f(color.red, color.green, color.blue)
        desc.draw(positionAnimator.position)
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        positionAnimator.step(deltaT)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        val temp = stream.readFloat() + 273
        val c = BlackBodyTemperature(temp)
        val p = BlackBodyPower(temp)
        var bbc = c * (p / desc.dissipator.nominalP.toFloat())
        color = (bbc + baseColor).normalize()

        if (positionAnimator.target == -1f) {
            positionAnimator.target = stream.readFloat()
            positionAnimator.position = positionAnimator.target
        } else {
            positionAnimator.target = stream.readFloat()
        }
    }

}


// 26.x port: class LargeRheostatGUI removed (client; redone in P3/P4)
