package mods.eln.transparentnode

import mods.eln.cable.CableRenderType
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.PowerSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidRegistry
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream

class FuelGeneratorDescriptor(name: String, internal val obj: Obj3D?, internal val cable: ElectricalCableDescriptor,
                              internal val nominalPower: Double, internal val maxVoltage: Double,
                              tankCapacityInLiters: Double)
: TransparentNodeDescriptor(name, FuelGeneratorElement::class.java, FuelGeneratorRender::class.java) {
    companion object {
        val FuelCalororificValue = 30500.0;                         // Fuel has a calorific value of about 30.5 MJ/l
        val BucketFuelEnergyCapacity = 2.5 * FuelCalororificValue;  // A bucket is worth about 2.5 liters of liquid. TODO: About right?
    }
    internal val tankEnergyCapacity = tankCapacityInLiters * FuelCalororificValue;

    internal val main = obj?.getPart("main")
    internal val cableRenderDescriptor = cable.render

    init {
        voltageLevelColor = VoltageLevelColor.fromCable(cable)
    }

    override fun setParent(item: net.minecraft.item.Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    fun draw() {
        main?.draw()
    }

    override fun use2DIcon() = true

    override fun getFrontFromPlace(side: Direction,
                                   entityLiving: EntityLivingBase): Direction {
        return super.getFrontFromPlace(side.left(), entityLiving)
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun shouldUseRenderHelper(
            type: IItemRenderer.ItemRenderType, item: ItemStack,
            helper: IItemRenderer.ItemRendererHelper) = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            objItemScale(obj)
            GL11.glPushMatrix()
            Direction.ZP.glRotateXnRef()
            GL11.glTranslatef(0f, -1f, 0f)
            GL11.glScalef(0.6f, 0.6f, 0.6f)
            draw()
            GL11.glPopMatrix()
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer,
                                list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Produces electricity using fuel."))
        list.add("  " + tr("Nominal voltage: %1$ V", Utils.plotValue(cable.electricalNominalVoltage)))
        list.add("  " + tr("Nominal power: %1$ W", Utils.plotValue(nominalPower)))
    }
}

class FuelGeneratorElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor):
        TransparentNodeElement(transparentNode, descriptor) {
    internal var positiveLoad = NbtElectricalLoad("positiveLoad")
    internal var powerSource = PowerSource("powerSource", positiveLoad)
    internal var slowProcess = FuelGeneratorSlowProcess(this)
    internal var descriptor = descriptor as FuelGeneratorDescriptor
    internal val fuel = FluidRegistry.getFluid("fuel") ?: FluidRegistry.getFluid("lava")
    internal var tankLevel = 0.0

    init {
        electricalLoadList.add(positiveLoad)
        electricalComponentList.add(powerSource)
        slowProcessList.add(NodePeriodicPublishProcess(transparentNode, 2.0, 2.0))
        slowProcessList.add(slowProcess)
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        if (side == front) return positiveLoad
        return null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        if (side == front) return NodeBase.maskElectricalPower
        return 0
    }

    override fun multiMeterString(side: Direction) = Utils.plotVolt("U+:", positiveLoad.u) +
            Utils.plotAmpere("I+:", positiveLoad.current) +
            Utils.plotPercent("Fill level:", tankLevel)


    override fun thermoMeterString(side: Direction): String? = null

    override fun initialize() {
        descriptor.cable.applyTo(positiveLoad)
        powerSource.setUmax(descriptor.maxVoltage)
        powerSource.setImax(descriptor.nominalPower * 5 / descriptor.maxVoltage)
        connect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream)
    }

    override fun onBlockActivated(player: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        val deltaLevel = FuelGeneratorDescriptor.BucketFuelEnergyCapacity / descriptor.tankEnergyCapacity
        if (tankLevel <= 1.0 - deltaLevel) {
            val bucket = player?.currentEquippedItem
            if (FluidContainerRegistry.isBucket(bucket) && FluidContainerRegistry.isFilledContainer(bucket)) {
                val fluidStack = FluidContainerRegistry.getFluidForFilledItem(bucket)
                if (fluidStack != null && fluidStack.fluidID == fuel.id) {
                    tankLevel += deltaLevel;
                    if (player != null && !player.capabilities.isCreativeMode) {
                        FluidContainerRegistry.drainFluidContainer(bucket);
                        val slot = player.inventory.currentItem
                        player.inventory.decrStackSize(slot, 1);
                        player.inventory.setInventorySlotContents(slot, bucket)
                    }

                    return true;
                }
            }
        }

        return false;
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)
        tankLevel = nbt?.getDouble("tankLevel") ?: 0.0
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)
        nbt?.setDouble("tankLevel", tankLevel)
    }
}

class FuelGeneratorRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor):
        TransparentNodeElementRender(tileEntity, descriptor) {
    internal var descriptor: FuelGeneratorDescriptor
    private var renderPreProcess: CableRenderType? = null
    private val eConn = LRDUMask()

    init {
        this.descriptor = descriptor as FuelGeneratorDescriptor
    }

    override fun draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.cableRenderDescriptor, eConn, renderPreProcess)
        front.glRotateZnRef()
        descriptor.draw()
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        eConn.deserialize(stream)
        renderPreProcess = null
    }
}

class FuelGeneratorSlowProcess(internal val generator: FuelGeneratorElement): IProcess {
    override fun process(time: Double) {
        generator.tankLevel = Math.max(0.0, generator.tankLevel - time *
                generator.powerSource.effectiveP / generator.descriptor.tankEnergyCapacity)

        if (generator.tankLevel > 0) {
            generator.powerSource.p = generator.descriptor.nominalPower
        } else {
            generator.powerSource.p = 0.0
        }
    }
}
