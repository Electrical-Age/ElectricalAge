package mods.eln.misc

import mods.eln.node.GhostNode
import mods.eln.node.NodeBase
import mods.eln.sim.ElectricalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

class GhostPowerNode(origin: Coordinate, front: Direction, offset: Coordinate, val load: ElectricalLoad): GhostNode() {

    val coord = Coordinate(offset).apply {
        applyTransformation(front, origin)
        dimension = origin.dimension
    }

    fun initialize() {
        onBlockPlacedBy(coord, Direction.XN, null, null)
    }

    override fun initializeFromThat(front: Direction?, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        connect()
    }

    override fun initializeFromNBT() {
    }

    override fun getSideConnectionMask(directionA: Direction?, lrduA: LRDU?) = NodeBase.MASK_ELECTRICAL_POWER

    override fun getThermalLoad(directionA: Direction?, lrduA: LRDU?) = null

    override fun getElectricalLoad(directionB: Direction?, lrduB: LRDU?) = load
}
