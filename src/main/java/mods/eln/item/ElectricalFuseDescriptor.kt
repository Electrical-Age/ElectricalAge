package mods.eln.item

import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.wiki.Data
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class ElectricalFuseDescriptor(name: String, val cableDescriptor: ElectricalCableDescriptor?, obj: Obj3D?) :
    GenericItemUsingDamageDescriptorUpgrade(name) {

    companion object {
        var BlownFuse: ElectricalFuseDescriptor? = null
    }

    private val fuseType = obj?.getPart("FuseType")
    private val fuseOk = obj?.getPart("FuseOk")
    private val fuse = obj?.getPart("Fuse")

    init {
        if (cableDescriptor != null) {
            setDefaultIcon("electricalfuse")
            voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cableDescriptor)
        } else {
            setDefaultIcon("blownelectricalfuse")
            voltageLevelColor = VoltageLevelColor.Neutral
        }
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)


    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }
}
