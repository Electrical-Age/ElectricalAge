package mods.eln.mechanical

import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.process.destruct.Failure
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.reflect.KClass

abstract class SimpleShaftDescriptor(name: String, elm: KClass<out TransparentNodeElement>, render: KClass<*>?, tag: EntityMetaTag) :
    TransparentNodeDescriptor(name, elm.java, render?.java, tag) {

    abstract val obj: Obj3D
    abstract val static: Array<out Obj3D.Obj3DPart>
    abstract val rotating: Array<out Obj3D.Obj3DPart>
    // If you set this you should also set volumeSetting in render.
    // (Otherwise it'll stick to 100% volume.)
    internal open val sound: String? = null

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)
    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)
}

// 26.x port: class ShaftRender removed (client; redone in P3/P4)


abstract class SimpleShaftElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    TransparentNodeElement(node, desc_), ShaftElement {
    override val shaftMass = 5.0
    override var shaft = ShaftNetwork(this)

    init {
        val exp = Failure(this).rotating { shaft.energy }
        slowProcessList.add(createShaftWatchdog(this).set(exp));
    }

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(front.left(), front.right())

    override fun initialize() {
        reconnect()
        shaft.connectShaft(this)
    }

    override fun onBreakElement() {
        super.onBreakElement()
        shaft.disconnectShaft(this)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream);
        stream.writeFloat(shaft.rads.toFloat())
        // For cables.
        node.lrduCubeMask.getTranslate(front.down()).serialize(stream)
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        shaft.writeToNBT(nbt, "shaft")
    }

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        shaft.readFromNBT(nbt, "shaft")
    }

    override fun multiMeterString(side: Direction?): String {
        return Utils.plotER(shaft.energy, shaft.rads)
    }
}
