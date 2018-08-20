package mods.eln.misc

import mods.eln.node.GhostNode
import mods.eln.node.NodeBase
import mods.eln.sim.ElectricalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

class GhostPowerNode(origin: Coordonate, front: Direction, offset: Coordonate, val load: ElectricalLoad): GhostNode() {

    val coord = Coordonate(offset).apply { applyTransformation(front, origin) }

    fun initialize() {
        onBlockPlacedBy(coord, Direction.XN, null, null)
    }

    override fun initializeFromThat(front: Direction?, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        connect()
    }

    override fun initializeFromNBT() {
    }

    override fun getSideConnectionMask(directionA: Direction?, lrduA: LRDU?) = NodeBase.maskElectricalPower

    override fun getThermalLoad(directionA: Direction?, lrduA: LRDU?) = null

    override fun getElectricalLoad(directionB: Direction?, lrduB: LRDU?) = load
}
