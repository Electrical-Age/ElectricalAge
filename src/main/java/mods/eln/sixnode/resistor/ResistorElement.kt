package mods.eln.sixnode.resistor

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ResistorProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
import mods.eln.sim.process.destruct.ThermalLoadWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sim.process.heater.ResistorHeatThermalLoad
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import java.io.DataOutputStream
import java.io.IOException
import java.util.HashMap

class ResistorElement(SixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(SixNode, side, descriptor) {

    internal var descriptor: ResistorDescriptor
    internal var aLoad = NbtElectricalLoad("aLoad")
    internal var bLoad = NbtElectricalLoad("bLoad")
    internal var r = Resistor(aLoad, bLoad)

    var control: NbtElectricalGateInput? = null

    internal var thermalWatchdog = ThermalLoadWatchDog()
    internal var thermalLoad = NbtThermalLoad("thermalLoad")
    internal var heater = ResistorHeatThermalLoad(r, thermalLoad)
    internal var resistorProcess: ResistorProcess

    var nominalRs = 1.0

    internal var inventory = SixNodeElementInventory(2, 64, this)

    init {
        this.descriptor = descriptor as ResistorDescriptor

        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        aLoad.rs = MnaConst.noImpedance
        bLoad.rs = MnaConst.noImpedance
        electricalComponentList.add(r)
        if (this.descriptor.isRheostat) {
            control = NbtElectricalGateInput("control")
            electricalLoadList.add(control)
        }

        thermalLoadList.add(thermalLoad)
        thermalSlowProcessList.add(heater)
        thermalLoad.setAsSlow()
        val thermalC = this.descriptor.thermalMaximalPowerDissipated * this.descriptor.thermalNominalHeatTime / this.descriptor.thermalWarmLimit
        val thermalRp = this.descriptor.thermalWarmLimit / this.descriptor.thermalMaximalPowerDissipated
        val thermalRs = this.descriptor.thermalConductivityTao / thermalC / 2.0
        thermalLoad.set(thermalRs, thermalRp, thermalC)
        slowProcessList.add(thermalWatchdog)
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
                .set(WorldExplosion(this).cableExplosion())

        resistorProcess = ResistorProcess(this, r, thermalLoad, this.descriptor)
        if (this.descriptor.tempCoef != 0.0 || this.descriptor.isRheostat) {
            slowProcessList.add(resistorProcess)
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            if (descriptor.isRheostat)
                stream.writeFloat(control!!.normalized.toFloat())
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun getElectricalLoad(lrdu: LRDU): ElectricalLoad? {
        if (lrdu == front.right()) return aLoad
        if (lrdu == front.left()) return bLoad
        if (lrdu == front) return control
        return null
    }

    override fun getThermalLoad(lrdu: LRDU): ThermalLoad {
        return thermalLoad
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (lrdu == front.right() || lrdu == front.left()) return NodeBase.maskElectricalPower
        if (lrdu == front && descriptor.isRheostat) return NodeBase.maskElectricalInputGate
        return 0
    }

    override fun multiMeterString(): String {
        val u = -Math.abs(aLoad.u - bLoad.u)
        val i = Math.abs(r.i)
        return Utils.plotOhm(Utils.plotUIP(u, i), r.r) + if (control != null) Utils.plotPercent("C", control!!.normalized) else ""
    }

    override fun getWaila(): Map<String, String>? {
        val info = HashMap<String, String>()
        info.put(I18N.tr("Resistance"), Utils.plotValue(r.r, "Ω"))
        info.put(I18N.tr("Voltage drop"), Utils.plotVolt("", Math.abs(r.u)))
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Current"), Utils.plotAmpere("", Math.abs(r.i)))

        }
        return info
    }

    override fun thermoMeterString(): String {
        return Utils.plotCelsius("T", thermalLoad.Tc)
    }

    override fun initialize() {
        setupPhysical()
    }

    public override fun inventoryChanged() {
        super.inventoryChanged()
        setupPhysical()
    }

    fun setupPhysical() {
        nominalRs = descriptor.getRsValue(inventory)
        resistorProcess.process(0.0)
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return onBlockActivatedRotate(entityPlayer)
    }

    override fun getInventory(): IInventory {
        return inventory
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return ResistorContainer(player, inventory)
    }
}
