package mods.eln.gridnode

import mods.eln.Eln
import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.UserError
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import java.util.HashSet
import java.util.Optional

/**
 * Created by svein on 23/08/15.
 */
class GridLink : INBTTReady {

    internal var a = Coordonate()
    internal var b = Coordonate()

    internal var connected = false
    // Drop this if the link is broken.
    lateinit internal var cable: ItemStack
    lateinit private var `as`: Direction
    lateinit private var bs: Direction
    private var ae = Optional.empty<GridElement>()
    private var be = Optional.empty<GridElement>()
    private var ab: ElectricalConnection? = null
    private var rs = MnaConst.highImpedance

    constructor(a: Coordonate, b: Coordonate, `as`: Direction, bs: Direction, cable: ItemStack, rs: Double) {
        this.rs = rs
        this.a = a
        this.b = b
        this.`as` = `as`
        this.bs = bs
        this.cable = cable
    }

    constructor(nbt: NBTTagCompound, str: String) {
        readFromNBT(nbt, str)
    }

    fun elementA(): GridElement {
        if (!ae.isPresent) {
            ae = Optional.of(getElementFromCoordinate(a)!!)
        }
        return ae.get()
    }

    fun elementB(): GridElement {
        if (!be.isPresent) {
            be = Optional.of(getElementFromCoordinate(b)!!)
        }
        return be.get()
    }

    fun connect(): Boolean {
        val a = getElementFromCoordinate(this.a)
        val b = getElementFromCoordinate(this.b)

        if (a == null || b == null || connected) {
            return false
        }

        // Add link to simulator.
        val aLoad = a.getGridElectricalLoad(`as`)
        val bLoad = b.getGridElectricalLoad(bs)
        if (aLoad == null || bLoad == null) {
            throw UserError("Invalid connection side")
        }
        assert(ab == null)
        ab = ElectricalConnection(aLoad, bLoad)
        Eln.simulator.addElectricalComponent(ab)
        ab!!.r = rs

        // Add link to link lists.
        a.gridLinkList.add(this)
        b.gridLinkList.add(this)
        updateElement(a)
        updateElement(b)

        connected = true
        return true
    }

    private fun updateElement(e: GridElement) {
        e.updateIdealRenderAngle()
        // Need to also publish everything connected to this.
        val s = HashSet<GridElement>()
        s.add(e)
        for (link in e.gridLinkList) {
            s.add(link.elementA())
            s.add(link.elementB())
        }
        for (element in s) {
            element.needPublish()
        }
    }

    fun disconnect() {
        if (!connected)
            return

        val a = getElementFromCoordinate(this.a)
        val b = getElementFromCoordinate(this.b)

        Eln.simulator.removeElectricalComponent(ab)
        ab = null

        a?.let { updateElement(it) }
        b?.let { updateElement(it) }

        connected = false
    }

    private fun links(a: GridElement, b: GridElement): Boolean {
        if (this.a == a.coordonate()) {
            return this.b == b.coordonate()
        }
        if (this.a == b.coordonate()) {
            return this.b == a.coordonate()
        }
        return false
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        a.readFromNBT(nbt, str + "a")
        b.readFromNBT(nbt, str + "b")
        `as` = Direction.readFromNBT(nbt, str + "as")
        bs = Direction.readFromNBT(nbt, str + "bs")
        rs = nbt.getDouble(str + "rs")
        cable = ItemStack.loadItemStackFromNBT(nbt)
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        a.writeToNBT(nbt, str + "a")
        b.writeToNBT(nbt, str + "b")
        `as`.writeToNBT(nbt, str + "as")
        bs.writeToNBT(nbt, str + "bs")
        nbt.setDouble(str + "rs", rs)
        cable.writeToNBT(nbt)
    }

    fun selfDestroy() {
        onBreakElement()
    }

    fun onBreakElement(): ItemStack {
        val a = getElementFromCoordinate(this.a)
        val b = getElementFromCoordinate(this.b)
        a!!.gridLinkList.remove(this)
        b!!.gridLinkList.remove(this)
        disconnect()
        return cable
    }

    fun getSide(gridElement: GridElement): Direction {
        if (gridElement === elementA()) {
            return `as`
        } else {
            return bs
        }
    }

    fun getOtherElement(gridElement: GridElement): GridElement {
        if (gridElement === elementA()) {
            return elementB()
        } else {
            return elementA()
        }
    }

    companion object {

        fun getElementFromCoordinate(coord: Coordonate?): GridElement? {
            if (coord == null) return null
            val element = NodeManager.instance.getTransparentNodeFromCoordinate(coord)
            if (element is GridElement) {
                return element
            } else {
                return null
            }
        }

        fun addLink(a: GridElement, b: GridElement, `as`: Direction, bs: Direction, cable: ElectricalCableDescriptor, cableLength: Int) {
            // Check if these two nodes are already linked.
            (a.gridLinkList + b.gridLinkList)
                .filter { it.links(a, b) }
                .forEach { throw UserError("Already Connected") }

            // Makin' a Link. Where'd Zelda go?
            val link = GridLink(
                    a.coordonate(), b.coordonate(), `as`, bs, cable.newItemStack(cableLength),
                    cable.electricalRs * cableLength)
            link.connect()
        }
    }
}
