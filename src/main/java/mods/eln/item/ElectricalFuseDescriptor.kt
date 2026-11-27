package mods.eln.item

import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
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


    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        if (cableDescriptor != null) {
            list.add(mods.eln.i18n.I18N.tr("Fits in an Electrical Fuse Holder; blows on over-current."))
        } else {
            list.add(mods.eln.i18n.I18N.tr("Spent fuse. Replace in an Electrical Fuse Holder."))
        }
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }
}
