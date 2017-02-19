package mods.eln.sixnode.powercapacitorsix

import mods.eln.Eln
import mods.eln.item.DielectricItem
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.VoltageLevelColor
import mods.eln.misc.series.ISerie
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

class PowerCapacitorSixDescriptor(name: String,
                                  private val obj: Obj3D?,
                                  internal var serie: ISerie,
                                  var dischargeTao: Double) : SixNodeDescriptor(name, PowerCapacitorSixElement::class.java, PowerCapacitorSixRender::class.java) {

    private var CapacitorCore: Obj3DPart? = null
    private var CapacitorCables: Obj3DPart? = null
    private var Base: Obj3DPart? = null

    init {
        if (obj != null) {
            CapacitorCables = obj.getPart("CapacitorCables")
            CapacitorCore = obj.getPart("CapacitorCore")
            Base = obj.getPart("Base")
        }

        voltageLevelColor = VoltageLevelColor.Neutral
    }

    fun getCValue(cableCount: Int, nominalDielVoltage: Double): Double {
        if (cableCount == 0) return 1e-6
        val uTemp = nominalDielVoltage / Eln.LVU
        return serie.getValue(cableCount - 1) / uTemp / uTemp
    }

    override fun use2DIcon(): Boolean {
        return true
    }

    fun getCValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(PowerCapacitorSixContainer.redId)
        val diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId)
        if (core == null || diel == null)
            return getCValue(0, 0.0)
        else {
            return getCValue(core.stackSize, getUNominalValue(inventory))
        }
    }

    fun getUNominalValue(inventory: IInventory): Double {
        val diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId)
        if (diel == null)
            return 10000.0
        else {
            val desc = DielectricItem.getDescriptor(diel) as DielectricItem
            return desc.uNominal * diel.stackSize
        }
    }

    override fun setParent(item: net.minecraft.item.Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    internal fun draw() {
        if (null != Base) Base!!.draw()
        if (null != CapacitorCables) CapacitorCables!!.draw()
        if (null != CapacitorCore) CapacitorCore!!.draw()
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type != IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90f, 0f, 1f, 0f)
            draw()
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player).left()
    }
}
