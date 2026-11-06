package mods.eln.sixnode

import mods.eln.gl.GL11


import mods.eln.registry.ElnDeviceRegistry
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.item.ElectricalFuseDescriptor
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class ElectricalFuseHolderDescriptor(name: String, obj: Obj3D) :
    SixNodeDescriptor(name, ElectricalFuseHolderElement::class.java, null) {
    private val case = obj.getPart("Case")
    private val fuse = obj.getPart("Fuse")
    private val fuseType = obj.getPart("FuseType")
    private val fuseOk = obj.getPart("FuseOk")

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    override fun hasItemDraw() = true

    override fun drawItem(entity: Boolean) {
        draw(null)

    }


    /** Small: placed anywhere on a face of a voxel block, in line on a cable, its holder shortened (Baughn, batch 3). */
    override fun placeAnywhere() = true

    override fun modelTurn(side: mods.eln.misc.Direction, front: mods.eln.misc.LRDU): mods.eln.misc.LRDU = front.right()

    /** Across: the holder; along it (y): 4 px each way, the holder drawn shortened to its pins (SHORT_LEADS). */
    override fun bodyBounds(): FloatArray? = boundsOf(case)?.also {
        it[0] = -4f
        it[1] = 4f
    }

    override fun drawPlaced(side: mods.eln.misc.Direction, front: mods.eln.misc.LRDU) {
        modelTurn(side, front).glRotateOnX()
        draw(null, true)
    }

    fun draw(installedFuse: ElectricalFuseDescriptor?) = draw(installedFuse, false)

    /** short: off centre, the holder shortened to its pins (the fuse in it as it is). */
    fun draw(installedFuse: ElectricalFuseDescriptor?, short: Boolean) {
        if (short) drawStretched(1, SHORT_LEADS, case) else case?.draw()
        if (installedFuse != null) {
            mods.eln.sixnode.electricalcable.ElectricalCableDescriptor.voltageLevelColorOf(installedFuse.cableDescriptor).setGLColor()
            fuseType?.draw()
            GL11.glColor3f(1f, 1f, 1f)
            if (installedFuse.cableDescriptor != null) {
                fuseOk?.draw()
            }
            fuse?.draw()
        }
    }


    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (list != null) {
            I18N.tr("Protects electrical components.\nFuse melts if current exceeds the\nfuse limit").split("\n").forEach { list.add(it) }
        }
    }

    override fun getFrontFromPlace(side: Direction, player: Player) =
        super.getFrontFromPlace(side, player).inverse()
}

class ElectricalFuseHolderElement(sixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, descriptor) {
    private val aLoad = NbtElectricalLoad("aLoad")
    private val bLoad = NbtElectricalLoad("bLoad")
    private val fuseResistor = Resistor(aLoad, bLoad)

    var installedFuse: ElectricalFuseDescriptor? = null
        set(value) {
            if (value == field) return
            field = value
            T = 0.0   // (a new fuse starts cold: the blown one's heat stayed, and the next blew at once; AdventurAgent)
            refreshSwitchResistor()
            needPublish()
        }

    private var T = 0.0

    /**
     * Its rating, A (Baughn 2026-11-06: set in its screen, one fuse item for every cable); 0: the installed fuse's own
     * cable's nominal current, as the four 1.12 fuses had. It blows at about 1.08 x its rating.
     */
    var ratingAmps = 0.0

    /** The rating now, A: the set one, else the working fuse's own, else a Lead Fuse's (shown with no fuse in). */
    fun ratingNow(): Double {
        if (ratingAmps > 0) return ratingAmps
        val c = installedFuse?.cableDescriptor ?: ElnDeviceRegistry.lowVoltageCableDescriptor
        return c.electricalNominalPower / c.electricalNominalVoltage
    }

    private val fuseProcess = IProcess { time ->
        val cable = installedFuse?.cableDescriptor
        // the fuse's cable's heat model, its current scaled so it blows at 1.08 x the rating
        var I = aLoad.current
        if (cable != null && ratingAmps > 0) I *= cable.electricalNominalPower / cable.electricalNominalVoltage / ratingAmps
        if (cable == null) {
            T = 0.0
        } else {
            val P = I * I * cable.electricalRs * 2.0 - T / cable.thermalRp * 0.9

            T += P / cable.thermalC * time
        }
        // WP16: was `?: 0.0 * 0.8` (precedence: the factor never applied). WP16b (Baughn: 0.8 = 1.02 x Inom is too
        // twitchy): steady T = warm (I / 1.2 Inom)^2 / 0.9, so factor f trips at I = 1.2 sqrt(0.9 f) Inom; 0.9 -> 1.08 x.
        if (T > (cable?.thermalWarmLimit ?: 0.0) * 0.9) {
            installedFuse = ElectricalFuseDescriptor.BlownFuse
        }
    }

    init {
        electricalLoadList.add(aLoad)
        electricalLoadList.add(bLoad)
        electricalComponentList.add(fuseResistor)
        electricalComponentList.add(Resistor(bLoad, null).pullDown())
        electricalComponentList.add(Resistor(aLoad, null).pullDown())
        electricalProcessList.add(fuseProcess)
    }

    override fun readFromNBT(nbt: CompoundTag?) {
        super.readFromNBT(nbt)
        if (nbt != null) {
            front = LRDU.readFromNBT(nbt, "front")

            val fuseTag = nbt.get("fuse")
            if (fuseTag != null) {
                val fuseStack = Utils.loadItem(fuseTag)
                if (!fuseStack.isEmpty) {
                    installedFuse = GenericItemUsingDamageDescriptorUpgrade.getDescriptor(fuseStack) as? ElectricalFuseDescriptor
                }
            }

            T = nbt.getDoubleOr("T", 0.0)
            ratingAmps = nbt.getDoubleOr("rating", 0.0)
        }
    }

    override fun writeToNBT(nbt: CompoundTag?) {
        super.writeToNBT(nbt)
        if (nbt != null) {
            front.writeToNBT(nbt, "front")

            if (installedFuse != null) {
                Utils.saveItem(installedFuse!!.newItemStack())?.let { nbt.put("fuse", it) }
            }

            nbt.putDouble("T", T)
            nbt.putDouble("rating", ratingAmps)
        }
    }

    override fun getElectricalLoad(lrdu: LRDU?): ElectricalLoad? = when (lrdu) {
        front -> aLoad
        front.inverse() -> bLoad
        else -> null
    }

    override fun getThermalLoad(lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(lrdu: LRDU?): Int = when (lrdu) {
        front -> NodeBase.maskElectricalAll
        front.inverse() -> NodeBase.maskElectricalAll
        else -> 0
    }

    override fun multiMeterString() = Utils.plotAmpere("I:", Math.abs(aLoad.current))

    override fun getWaila(): MutableMap<String, String> {
        val infos = mutableMapOf(Pair(I18N.tr("Current"), Utils.plotAmpere("", Math.abs(aLoad.current))))
        infos[I18N.tr("Rating")] = Utils.plotAmpere("", ratingNow())
        return infos
    }

    override fun thermoMeterString(): String? = null

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        if (stream != null) {
            try {
                Utils.serialiseItemStack(stream, installedFuse?.newItemStack())
                stream.writeFloat(ratingNow().toFloat())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSwitchResistor() {
        installedFuse?.cableDescriptor?.applyTo(fuseResistor) ?: fuseResistor.ultraImpedance()
    }

    override fun initialize() {
        computeElectricalLoad()
    }

    override fun hasGui() = true

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)
        try {
            when (stream?.readByte()?.toInt()) {
                SET_RATING -> {
                    val a = stream.readFloat().toDouble()
                    ratingAmps = if (a.isFinite() && a > 0) Math.min(a, 100000.0) else 0.0
                    needPublish()
                }
                TAKE_OUT -> installedFuse?.let {
                    installedFuse = null
                    sixNode.dropItem(it.newItemStack())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val SET_RATING = 1
        const val TAKE_OUT = 2
    }

    fun computeElectricalLoad() {
        ElnDeviceRegistry.veryHighVoltageCableDescriptor.applyTo(aLoad)
        ElnDeviceRegistry.veryHighVoltageCableDescriptor.applyTo(bLoad)
        refreshSwitchResistor()
    }

    override fun onBlockActivated(entityPlayer: Player?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true

        var takenOutFuse: ElectricalFuseDescriptor? = null
        val itemStack = entityPlayer?.mainHandItem
        val fuseDescriptor = GenericItemUsingDamageDescriptorUpgrade.getDescriptor(itemStack) as? ElectricalFuseDescriptor
        if (itemStack != null && !itemStack.isEmpty) { // 1.12: an empty hand is ItemStack.EMPTY, not null
            if (fuseDescriptor != null && itemStack.count > 0) {
                // The player puts in a new lead fuse.
                itemStack.count--
                takenOutFuse = installedFuse
                installedFuse = fuseDescriptor
            }
        } else return false   // (an empty hand opens its screen: the rating, and taking the fuse out)

        // What do we do with the fuse just taken out?
        takenOutFuse?.let {
            sixNode.dropItem(it.newItemStack())
        }

        return takenOutFuse != null || fuseDescriptor != null
    }
}

class ElectricalFuseHolderRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {
    private val descriptor = descriptor as ElectricalFuseHolderDescriptor
    var installedFuse: ElectricalFuseDescriptor? = null
    var rating = 0f

    override fun draw() {
        drawPins()   // (off centre)
        front.right().glRotateOnX()
        descriptor.draw(installedFuse, slot >= 6)
    }

    override fun publishUnserialize(stream: DataInputStream?) {
        super.publishUnserialize(stream)
        if (stream != null) {
            installedFuse = GenericItemUsingDamageDescriptor.getDescriptor(Utils.unserialiseItemStack(stream)) as? ElectricalFuseDescriptor
            rating = try { stream.readFloat() } catch (e: IOException) { 0f }
        }
    }
}

