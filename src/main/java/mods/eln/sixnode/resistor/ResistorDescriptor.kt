package mods.eln.sixnode.resistor

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.misc.series.ISerie
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

/**
 * Created by svein on 05/08/15.
 */
class ResistorDescriptor(name: String,
                         obj: Obj3D,
                         internal var series: ISerie,
                         var tempCoef: Double,
                         val isRheostat: Boolean) : SixNodeDescriptor(name, ResistorElement::class.java, ResistorRender::class.java) {
    var thermalCoolLimit = -100.0
    var thermalWarmLimit = Eln.cableWarmLimit
    var thermalMaximalPowerDissipated = 1000.0
    var thermalNominalHeatTime = 120.0
    var thermalConductivityTao = Eln.cableThermalConductionTao

    internal var ResistorBaseExtension: Obj3D.Obj3DPart = obj.getPart("ResistorBaseExtention")
    internal var ResistorCore: Obj3D.Obj3DPart = obj.getPart("ResistorCore")
    internal var ResistorTrack: Obj3D.Obj3DPart = obj.getPart("ResistorTrack")
    internal var ResistorWiper: Obj3D.Obj3DPart = obj.getPart("ResistorWiper")
    internal var Base: Obj3D.Obj3DPart = obj.getPart("Base")
    internal var Cables: Obj3D.Obj3DPart = obj.getPart("CapacitorCables")

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun use2DIcon(): Boolean {
        return true
    }

    fun getRsValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(ResistorContainer.coreId) ?: return series.getValue(0)

        return series.getValue(core.stackSize)
    }

    override fun setParent(item: net.minecraft.item.Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    internal fun draw(wiperPos: Float) {
        Base.draw()
        ResistorBaseExtension.draw()
        ResistorCore.draw()
        Cables.draw()

        if (isRheostat) {
            val wiperSpread = 0.238f
            val pos = (wiperPos - 0.5f) * wiperSpread * 2f
            ResistorTrack.draw()
            GL11.glTranslatef(0f, 0f, pos)
            ResistorWiper.draw()
        }
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
            draw(0f)
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player).left()
    }
}
