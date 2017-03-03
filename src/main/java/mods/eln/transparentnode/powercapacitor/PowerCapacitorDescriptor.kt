package mods.eln.transparentnode.powercapacitor

import mods.eln.Eln
import mods.eln.item.DielectricItem
import mods.eln.misc.Obj3D
import mods.eln.misc.series.ISerie
import mods.eln.node.transparent.TransparentNodeDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer

class PowerCapacitorDescriptor(
        name: String,
        private val obj: Obj3D?,
        internal var serie: ISerie,
        var dischargeTao: Double

) : TransparentNodeDescriptor(name, PowerCapacitorElement::class.java, PowerCapacitorRender::class.java) {

    init {
        if (obj != null) {

        }

    }

    override fun use2DIcon(): Boolean {
        return false
    }

    fun getCValue(cableCount: Int, nominalDielVoltage: Double): Double {
        if (cableCount == 0) return 0.0
        val uTemp = nominalDielVoltage / Eln.LVU
        return serie.getValue(cableCount - 1) / uTemp / uTemp
    }

    fun getCValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(PowerCapacitorContainer.redId)
        val diel = inventory.getStackInSlot(PowerCapacitorContainer.dielectricId)
        if (core == null || diel == null)
            return getCValue(0, 0.0)
        else {
            return getCValue(core.stackSize, getUNominalValue(inventory))
        }
    }

    fun getUNominalValue(inventory: IInventory): Double {
        val diel = inventory.getStackInSlot(PowerCapacitorContainer.dielectricId)
        if (diel == null)
            return 10000.0
        else {
            val desc = DielectricItem.getDescriptor(diel) as DielectricItem
            return desc.uNominal * diel.stackSize
        }
    }

    override fun setParent(item: net.minecraft.item.Item, damage: Int) {
        super.setParent(item, damage)
        //Data.addEnergy(newItemStack());
    }

    internal fun draw() {

    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack,
                                       helper: IItemRenderer.ItemRendererHelper): Boolean {
        return true
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        draw()
    }

}
