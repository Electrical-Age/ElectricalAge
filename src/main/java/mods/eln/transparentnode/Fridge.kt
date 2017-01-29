package mods.eln.transparentnode

import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkin
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtResistor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot

class FridgeDescriptor(name: String, obj: Obj3D) :
        TransparentNodeDescriptor(name, FridgeElement::class.java, FridgeRender::class.java) {
    val main: Obj3D.Obj3DPart = obj.getPart("core")
    val fridgeDoor: Obj3D.Obj3DPart = obj.getPart("doorfridge")
    val freezerDoor: Obj3D.Obj3DPart = obj.getPart("doorfreezer")

    internal fun draw(open: Float = 0f) {
        main.draw()
        fridgeDoor.draw(open * -90, 0f, 1f, 0f)
        freezerDoor.draw(open * -90, 0f, 1f, 0f)
    }
}

class FridgeElement(node: TransparentNode, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElement(node, descriptor) {
    private val load = NbtElectricalLoad("load")
    private val resistor = NbtResistor("resistor", load, null)

    private val inventory = TransparentNodeElementInventory(36, 1, this)

    init {
        electricalLoadList.add(load)
        electricalComponentList.add(resistor)
    }

    override fun initialize() {
        connect()
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU) = when (side) {
        front.inverse, front.left(), front.right() -> NodeBase.maskElectricalPower
        else -> 0
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? = load

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun multiMeterString(side: Direction) = ""

    override fun thermoMeterString(side: Direction) = ""

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction, vx: Float, vy: Float, vz: Float) = false

    override fun hasGui() = true

    override fun getInventory() = inventory

    override fun newContainer(side: Direction, player: EntityPlayer) = FridgeContainer(player, inventory)
}

class FridgeRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
        TransparentNodeElementRender(entity, descriptor) {
    private val descriptor = descriptor as FridgeDescriptor

    private val coord = Coordonate(entity)

    private val inventory = TransparentNodeElementInventory(36, 1, this)

    private var open = PhysicalInterpolator(0.4f, 8f, 0.4f, 0f)

    override fun draw() {
        front.left().glRotateZnRef()
        descriptor.draw(open.get())
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        open.target = if (Utils.isPlayerAround(tileEntity.worldObj, coord.moved(front).getAxisAlignedBB(0))) 1f else 0f
        open.step(deltaT)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer) = FridgeGui(player, inventory, this)
}

class FridgeGui(player: EntityPlayer, inventory: IInventory, render: FridgeRender) : GuiContainerEln(FridgeContainer(player, inventory)) {
    override fun newHelper(): GuiHelperContainer = GuiHelperContainer(this, 176, 186, 8, 104)
}

class FridgeContainer(player: EntityPlayer, inventory: IInventory)
    : BasicContainer(player, inventory, Array<Slot>(32, {
    SlotWithSkin(inventory, it, 26 + it.mod(8) * 18, 8 + (if (it >= 8) 10 else 0) + (it / 8) * 18, SlotSkin.medium)
})) {}
