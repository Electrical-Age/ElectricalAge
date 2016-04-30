package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.common.ForgeChunkManager
import org.apache.commons.math3.analysis.function.Logistic
import java.io.DataInputStream
import java.io.DataOutputStream

class RealityPylonDescriptor(name: String, obj: Obj3D) :
        TransparentNodeDescriptor(name, RealityPylonElement::class.java, RealityPylonRender::class.java) {

    val nominalU = 3200.0
    val minimumU = nominalU * 0.8
    val baseLoad = 100.0 // Watts, drawn even during login

    val main = obj.getPart("main")
    val marble = obj.getPart("marble")

    fun draw(angle: Float, xrot: Float, yrot: Float, zrot: Float) {
        main.draw();
        marble.draw(angle, xrot, yrot, zrot, angle, xrot, yrot, zrot)
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true
    override fun use2DIcon() = false
    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack,
                                       helper: IItemRenderer.ItemRendererHelper) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) = draw(0f, 0f, 0f, 0f)
}

object RealityPylonChunkManager : ForgeChunkManager.LoadingCallback {

    override fun ticketsLoaded(tickets: MutableList<ForgeChunkManager.Ticket>, world: World) {
        for (ticket in tickets) {
            for (chunk in ticket.chunkList) {
                System.out.println("EARP: Refreshing ticket for ${ticket.playerName} at $chunk")
                ForgeChunkManager.forceChunk(ticket, chunk)
            }
        }
    }

    fun acquire(e: RealityPylonElement): ForgeChunkManager.Ticket? {
        val ticket = ForgeChunkManager.requestPlayerTicket(Eln.instance, e.owner, e.world(), ForgeChunkManager.Type.NORMAL)
        if (ticket == null) {
            System.out.println("EARP: No ticket available for ${e.owner}")
            return null
        }
        System.out.println("EARP: Got ticket for ${e.owner}")
        val origin = e.node.coordonate
        // TODO: Configurable CL sizes.
        for (x in -2..2) {
            for (z in -2..2) {
                val chunk = ChunkCoordIntPair(origin.x / 16 + x, origin.z / 16 + z)
                ForgeChunkManager.forceChunk(ticket, chunk)
            }
        }
        return ticket
    }

    fun release(e: RealityPylonElement) {
        ForgeChunkManager.releaseTicket(e.chunkLoadingTicket)
    }
}

class RealityPylonElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
        TransparentNodeElement(node, desc_) {
    val desc = desc_ as RealityPylonDescriptor

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?)= null
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float) = false

    val load = NbtElectricalLoad("load")
    val resistor = Resistor(load, null)

    // Player who placed this pylon
    var owner = ""
    // Time since that player was last online, in seconds
    var loadTime = 0.0
    // Set to true if it ever gets insufficient power. Cleared only during player login.
    var faulted = true
    // The chunkloading ticket.
    var chunkLoadingTicket: ForgeChunkManager.Ticket? = null

    val updateProcess = Update()

    init {
        electricalComponentList.add(resistor)
        resistor.highImpedance()
        load.rs = 1.0
        electricalLoadList.add(load)
        slowProcessList.add(updateProcess)
    }

    inner class Update: IProcess {
        val curve = Logistic(Eln.instance.realityPylonMaxPower.toDouble() - desc.baseLoad,
                Eln.instance.realityPylonDays / 2.0,
                2.5, 1.0, 0.0, 0.5)

        var lastR = 1.0
        var ticks = 0
        var watts = desc.baseLoad

        override fun process(time: Double) {
            loadTime += time
            ticks++

            if (ticks % 100 == 13) {
                for (player in world().playerEntities) {
                    if (player is EntityPlayer && player.displayName == owner) {
                        loadTime = 0.0
                        needPublish()
                        break
                    }
                }
            }

            watts = curve.value(loadTime / 86400.0) + desc.baseLoad
            val u = load.u
            if (u < desc.minimumU && !faulted) {
                faulted = true
                if (chunkLoadingTicket != null) RealityPylonChunkManager.release(this@RealityPylonElement)
                needPublish()
            } else if (u > desc.minimumU && faulted) {
                faulted = false
                chunkLoadingTicket = RealityPylonChunkManager.acquire(this@RealityPylonElement)
                needPublish()
            }

            if (faulted) return

            val newR = u * u / watts
            if (newR > lastR * 1.01 || newR < lastR * 0.99) {
                lastR = newR
                resistor.r = newR
                needPublish()
            }
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setString("owner", owner)
        nbt.setFloat("loadTime", loadTime.toFloat())
        nbt.setBoolean("faulted", faulted)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        owner = nbt.getString("owner")
        loadTime = nbt.getFloat("loadTime").toDouble()
        faulted = nbt.getBoolean("faulted")
    }

    override fun initializeFromThat(front: Direction?, entityLiving: EntityLivingBase?, itemStackNbt: NBTTagCompound?) {
        super.initializeFromThat(front, entityLiving, itemStackNbt)
        owner = (entityLiving as EntityPlayer).displayName
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front -> load
            else -> null
        }
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front -> NodeBase.maskElectricalPower
            else -> 0
        }
    }

    override fun multiMeterString(side: Direction) = Utils.plotPower("W", updateProcess.watts) + " F: ${faulted}"

    override fun thermoMeterString(side: Direction) = ""

    override fun initialize() {
        connect();
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeBoolean(faulted)
    }
}

class RealityPylonRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor) :
        TransparentNodeElementRender(entity, desc) {
    val desc = desc as RealityPylonDescriptor

    var faulted = false
    var xdelta = 0f
    var ydelta = 0f
    var zdelta = 0f
    var xrot = 1f
    var yrot = 0f
    var zrot = 0f
    var rot = 0f
    var rotD = 0f

    private fun randoff(x: Float) = Math.min(Math.max((Math.random() - 0.5) * 0.1 + x, -1.0), 1.0).toFloat()

    override fun draw() {
        if (faulted) {
            xdelta *= 0.95f
            ydelta *= 0.95f
            zdelta *= 0.95f
            rotD = 0f
            rot *= 0.9f
        } else {
            xdelta = randoff(xdelta)
            ydelta = randoff(ydelta)
            zdelta = randoff(zdelta)
            xrot *= 0.9f
            yrot *= 0.9f
            zrot *= 0.9f
            rotD = 1f
        }
        xrot += xdelta
        yrot += ydelta
        zrot += zdelta
        rot += rotD

        front.glRotateZnRef()
        desc.draw(rot, xrot, yrot, zrot)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        faulted = stream.readBoolean()
    }
}
