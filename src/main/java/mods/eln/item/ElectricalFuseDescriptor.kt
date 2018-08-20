package mods.eln.item

import mods.eln.misc.Obj3D
import mods.eln.misc.VoltageLevelColor
import mods.eln.misc.preserveMatrix
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.wiki.Data
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

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
            voltageLevelColor = VoltageLevelColor.fromCable(cableDescriptor)
        } else {
            setDefaultIcon("blownelectricalfuse")
            voltageLevelColor = VoltageLevelColor.Neutral
        }
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?,
                                       helper: IItemRenderer.ItemRendererHelper?) = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        when (type) {
            IItemRenderer.ItemRenderType.INVENTORY -> super.renderItem(type, item, *data)
            else -> {
                preserveMatrix {
                    GL11.glTranslatef(0.6f, 0.4f, 0.8f)
                    GL11.glRotatef(150f, 0.6f, 1f, 0f)
                    GL11.glScalef(1.5f, 1.5f, 1.5f)
                    if (fuseType != null) {
                        voltageLevelColor.setGLColor()
                        fuseType.draw()
                        GL11.glColor3f(1f, 1f, 1f)
                    }
                    if (cableDescriptor != null) {
                        fuseOk?.draw()
                    }
                    fuse?.draw()
                }
            }
        }
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }
}
