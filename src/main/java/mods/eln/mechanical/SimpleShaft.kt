package mods.eln.mechanical

import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.misc.preserveMatrix
import mods.eln.node.transparent.*
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.reflect.KClass

abstract class SimpleShaftDescriptor(name: String, elm: KClass<out TransparentNodeElement>, render: KClass<out TransparentNodeElementRender>, tag: EntityMetaTag):
        TransparentNodeDescriptor(name, elm.java, render.java, tag) {

    abstract val obj: Obj3D
    abstract val static: Array<out Obj3D.Obj3DPart>
    abstract val rotating: Array<out Obj3D.Obj3DPart>

    open fun draw(angle: Double) {
        for (part in static) {
            part.draw()
        }
        preserveMatrix {
            assert(rotating.size > 0)
            val bb = rotating[0].boundingBox()
            val centre = bb.centre()
            val ox = centre.xCoord
            val oy = centre.yCoord
            val oz = centre.zCoord
            GL11.glTranslated(ox, oy, oz)
            GL11.glRotatef(((angle * 360).toDouble() / 2.0 / Math.PI).toFloat(), 0f, 0f, 1f)
            GL11.glTranslated(-ox, -oy, -oz)
            for (part in rotating) {
                part.draw()
            }
        }
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        objItemScale(obj)
        Direction.ZN.glRotateXnRef();
        GL11.glPushMatrix();
        GL11.glTranslatef(0f, -1f, 0f);
        GL11.glScalef(0.6f, 0.6f, 0.6f);
        draw(0.0);
        GL11.glPopMatrix();
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true
    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper) = true
    override fun use2DIcon() = false
}

open class ShaftRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): TransparentNodeElementRender(entity, desc) {
    private val desc = desc as SimpleShaftDescriptor
    var rads = 0.0
    var logRads = 0.0
    var angle = 0.0

    override fun draw() {
        front.glRotateXnRef()
        if (front == Direction.XP || front == Direction.ZP)
            desc.draw(angle)
        else
            desc.draw(-angle);
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        angle += logRads * deltaT
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        rads = stream.readFloat().toDouble()
        logRads = Math.log(rads + 1) / Math.log(1.2)
    }
}

abstract class SimpleShaftElement(node : TransparentNode, desc_: TransparentNodeDescriptor) :
        TransparentNodeElement(node, desc_), ShaftElement {
    override val shaftMass = 5.0
    override var shaft = ShaftNetwork(this)

    init {
        val exp = WorldExplosion(this).machineExplosion()
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
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        shaft.writeToNBT(nbt, "shaft")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        shaft.readFromNBT(nbt, "shaft")
    }

    override fun multiMeterString(side: Direction?): String {
        return Utils.plotER(shaft.energy, shaft.rads)
    }
}
