package mods.eln.mechanical

import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sound.LoopedSound
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.reflect.KClass

abstract class SimpleShaftDescriptor(name: String, elm: KClass<out TransparentNodeElement>, render: KClass<out TransparentNodeElementRender>, tag: EntityMetaTag) :
    TransparentNodeDescriptor(name, elm.java, render.java, tag) {

    abstract val obj: Obj3D
    abstract val static: Array<out Obj3D.Obj3DPart>
    abstract val rotating: Array<out Obj3D.Obj3DPart>
    // If you set this you should also set volumeSetting in render.
    // (Otherwise it'll stick to 100% volume.)
    internal open val sound: String? = null

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

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
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            objItemScale(obj)
            preserveMatrix {
                Direction.ZN.glRotateXnRef()
                GL11.glTranslatef(0f, -1f, 0f)
                GL11.glScalef(0.6f, 0.6f, 0.6f)
                draw(0.0)
            }
        }
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true
    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper) =
        type != IItemRenderer.ItemRenderType.INVENTORY
}

open class ShaftRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, desc) {
    private val desc = desc as SimpleShaftDescriptor
    var rads = 0.0
    var logRads = 0.0
    var angle = 0.0
    // Cable drawing:
    val eConn = LRDUMask()
    val mask = LRDUMask()
    var connectionType: CableRenderType? = null
    open val cableRender: CableRenderDescriptor? = null
    var cableRefresh = true
    // Sound:
    private var soundLooper: ShaftSoundLooper? = null
    val volumeSetting = SlewLimiter(0.5f)

    inner private class ShaftSoundLooper(sound: String, coord: Coordonate) : LoopedSound(sound, coord) {
        override fun getPitch() = Math.max(0.05, rads / absoluteMaximumShaftSpeed).toFloat()
        override fun getVolume() = volumeSetting.position
    }

    open fun initSound(desc: SimpleShaftDescriptor) {
        volumeSetting.target = 1f
        volumeSetting.position = 0f
        val sound = desc.sound
        if (sound != null) {
            soundLooper = ShaftSoundLooper(sound, coordonate())
            addLoopedSound(soundLooper)
        } else {
            soundLooper = null
        }
    }

    init {
        initSound(this.desc)
        mask.set(LRDU.Down, true)
    }

    /**
     * By default, call the descriptor's draw function and nothing else.
     */
    override fun draw() {
        draw {}
    }

    /**
     * But, optionally, do some more drawing in the block context.
     */
    fun draw(extra: () -> Unit) {
        preserveMatrix {
            front.glRotateXnRef()
            if (front == Direction.XP || front == Direction.ZP)
                desc.draw(angle)
            else
                desc.draw(-angle);

            extra()
        }

        if (cableRender != null) {
            preserveMatrix {
                if (cableRefresh) {
                    cableRefresh = false;
                    connectionType = CableRender.connectionType(tileEntity, eConn, front.down())
                }

                glCableTransforme(front.down());
                cableRender!!.bindCableTexture();

                for (lrdu in LRDU.values()) {
                    Utils.setGlColorFromDye(connectionType!!.otherdry[lrdu.toInt()])
                    if (!eConn.get(lrdu)) continue
                    if (lrdu != front.down().getLRDUGoingTo(front) && lrdu.inverse() != front.down().getLRDUGoingTo(front)) continue
                    mask.set(1.shl(lrdu.ordinal))
                    CableRender.drawCable(cableRender, mask, connectionType)
                }
            }
        }
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        angle += logRads * deltaT
        volumeSetting.step(deltaT)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        rads = stream.readFloat().toDouble()
        logRads = Math.log(rads + 1) / Math.log(1.2)
        eConn.deserialize(stream)
        cableRefresh = true
    }
}

abstract class SimpleShaftElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    TransparentNodeElement(node, desc_), ShaftElement {
    override val shaftMass = 5.0
    open var shaft: ShaftNetwork = ShaftNetwork()
    override fun getShaft(dir: Direction): ShaftNetwork? = shaft
    override fun setShaft(dir: Direction, net: ShaftNetwork?) {
        if(net != null) shaft = net
    }
    var destructing = false
    override fun isDestructing() = destructing

    init {
        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(createShaftWatchdog(this).set(exp));
    }

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(front.left(), front.right())

    override fun initialize() {
        reconnect()
        val rads = shaft.rads  // Carry over loaded rads, if any
        shaft = ShaftNetwork(this, shaftConnectivity.iterator())
        shaft.rads = rads
        // Utils.println(String.format("SS.i: new %s r=%f", shaft, shaft.rads))
        shaftConnectivity.forEach {
            // These calls can still change the speed via mergeShaft
            shaft.connectShaft(this, it)
        }
    }

    override fun onBreakElement() {
        super.onBreakElement()
        destructing = true
        shaftConnectivity.forEach {
            shaft.disconnectShaft(this)
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(shaft.rads.toFloat())
        // For cables.
        node.lrduCubeMask.getTranslate(front.down()).serialize(stream)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        shaft.writeToNBT(nbt, "shaft")
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        shaft.readFromNBT(nbt, "shaft")
        // Utils.println(String.format("SS.rFN: %s r=%f", shaft, shaft.rads))
    }

    override fun multiMeterString(side: Direction?): String {
        return Utils.plotER(shaft.energy, shaft.rads)
    }
}
