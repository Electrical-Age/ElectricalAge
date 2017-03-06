package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.cable.CableRenderType
import mods.eln.fluid.FuelRegistry
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.published
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.PowerSource
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sound.LoopedSound
import mods.eln.wiki.Data
import net.minecraft.client.audio.ISound
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
                              tankCapacityInSecondsAtNominalPower: Double)
    : TransparentNodeDescriptor(name, FuelGeneratorElement::class.java, FuelGeneratorRender::class.java) {
    companion object {
        internal fun EfficiencyFactorVsLoadFactor(loadFactor: Double) = when (Utils.limit(loadFactor, 0.0, 1.5)) {
            in 0.0..0.1 -> 1.375
            in 0.1..0.2 -> 1.125
            in 0.2..0.3 -> 1.050
            in 0.3..0.4 -> 1.025
            in 0.4..0.5 -> 1.010
            in 0.5..1.1 -> 1.000
            in 1.1..1.2 -> 1.050
            in 1.2..1.3 -> 1.100
            in 1.3..1.4 -> 1.150
            in 1.4..1.5 -> 1.5
            else -> 1.5
        }

        val TankCapacityInBuckets = 2
        val GeneratorBailOutVoltageRatio = 0.5
        val MinimalLoadFractionOfNominalPower = 0.1
        val VoltageStabilizationGracePeriod = 1.0
    }

    internal val tankEnergyCapacity = tankCapacityInSecondsAtNominalPower * nominalPower;

    internal val main = obj?.getPart("main")
    internal val switch = obj?.getPart("switch")
    internal val cableRenderDescriptor = cable.render

    internal val fuels = FuelRegistry.gasolineList

    init {
        voltageLevelColor = VoltageLevelColor.fromCable(cable)
    }

    override fun setParent(item: net.minecraft.item.Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    fun draw(on: Boolean = false) {
        main?.draw()
        if (on) {
            UtilsClient.drawLight(switch)
        } else {
            switch?.draw()
        }
    }

    override fun use2DIcon() = true

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun shouldUseRenderHelper(
        type: IItemRenderer.ItemRenderType, item: ItemStack,
        helper: IItemRenderer.ItemRendererHelper) = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) = when (type) {
        IItemRenderer.ItemRenderType.INVENTORY -> super.renderItem(type, item, *data)
        else -> {
            objItemScale(obj)
            preserveMatrix {
                Direction.ZP.glRotateXnRef()
                GL11.glTranslatef(0f, -1f, 0f)
                GL11.glScalef(0.6f, 0.6f, 0.6f)
                draw()
            }
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer,
                                list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Produces electricity using gasoline."))
        list.add("  " + tr("Nominal voltage: %1$ V", Utils.plotValue(cable.electricalNominalVoltage)))
        list.add("  " + tr("Nominal power: %1$ W", Utils.plotValue(nominalPower)))
    }
}

class FuelGeneratorElement(transparentNode: TransparentNode, descriptor_: TransparentNodeDescriptor) :
    TransparentNodeElement(transparentNode, descriptor_) {
    internal var positiveLoad = NbtElectricalLoad("positiveLoad")
    internal var powerSource = PowerSource("powerSource", positiveLoad)
    internal var slowProcess = FuelGeneratorSlowProcess(this)
    internal var descriptor = descriptor_ as FuelGeneratorDescriptor
    internal val fuels = FuelRegistry.fluidListToFluids(descriptor.fuels).map { it.id }
    internal var tankLevel = 0.0
    internal var tankFluid = FluidRegistry.getFluid("lava").id
    internal var on by published(false)
    internal var voltageGracePeriod = 0.0

    init {
        electricalLoadList.add(positiveLoad)
        electricalComponentList.add(powerSource)
        slowProcessList.add(NodePeriodicPublishProcess(transparentNode, 1.0, 0.5))
        slowProcessList.add(slowProcess)
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? = when (lrdu) {
        LRDU.Down -> when (side) {
            front, front.inverse -> positiveLoad
            else -> null
        }
        else -> null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int = when (lrdu) {
        LRDU.Down -> when (side) {
            front, front.inverse -> NodeBase.maskElectricalPower
            else -> 0
        }
        else -> 0
    }

    override fun multiMeterString(side: Direction) = Utils.plotVolt("U+:", positiveLoad.u) +
        Utils.plotAmpere("I+:", positiveLoad.current) +
        Utils.plotPercent("Fuel level:", tankLevel)


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
        stream.writeBoolean(on)
        stream.writeFloat((positiveLoad.u / descriptor.maxVoltage).toFloat())
    }

    override fun onBlockActivated(player: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (!(player?.worldObj?.isRemote ?: true)) {
            val bucket = player?.currentEquippedItem
            if (FluidContainerRegistry.isBucket(bucket) && FluidContainerRegistry.isFilledContainer(bucket)) {
                val deltaLevel = 1.0 / FuelGeneratorDescriptor.TankCapacityInBuckets;
                if (tankLevel <= 1.0 - deltaLevel) {
                    val fluidStack = FluidContainerRegistry.getFluidForFilledItem(bucket)
                    if (fluidStack != null && (fluidStack.fluidID == tankFluid || tankLevel <= 0.0) &&
                        fluidStack.fluidID in fuels) {
                        tankFluid = fluidStack.fluidID
                        tankLevel += deltaLevel
                        if (player != null && !player.capabilities.isCreativeMode) {
                            val emptyBucket = FluidContainerRegistry.drainFluidContainer(bucket);
                            val slot = player.inventory.currentItem
                            player.inventory.setInventorySlotContents(slot, emptyBucket)
                        }

                        return true;
                    }
                }
            } else {
                if (Eln.multiMeterElement.checkSameItemStack(player?.currentEquippedItem) ||
                    Eln.thermometerElement.checkSameItemStack(player?.currentEquippedItem) ||
                    Eln.allMeterElement.checkSameItemStack(player?.currentEquippedItem)) {
                    return false
                }

                if (on) {
                    on = false
                } else {
                    if (tankLevel > 0) {
                        on = true
                        voltageGracePeriod = FuelGeneratorDescriptor.VoltageStabilizationGracePeriod
                    }
                }
                return true
            }
        }

        return false
    }

    override fun readFromNBT(nbt: NBTTagCompound?) {
        super.readFromNBT(nbt)
        tankLevel = nbt?.getDouble("tankLevel") ?: 0.0
        on = nbt?.getBoolean("on") ?: false
    }

    override fun writeToNBT(nbt: NBTTagCompound?) {
        super.writeToNBT(nbt)
        nbt?.setDouble("tankLevel", tankLevel)
        nbt?.setBoolean("on", on)
    }

    override fun getWaila(): Map<String, String> = mutableMapOf(
        Pair(I18N.tr("State"), if (on) I18N.tr("ON") else I18N.tr("OFF")),
        Pair(I18N.tr("Fuel level"), Utils.plotPercent("", tankLevel)),
        Pair(I18N.tr("Generated power"), Utils.plotPower("", powerSource.effectiveP)),
        Pair(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.u))
    )
}

class FuelGeneratorRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(tileEntity, descriptor) {
    internal var descriptor: FuelGeneratorDescriptor
    private var renderPreProcess: CableRenderType? = null
    private val eConn = LRDUMask()
    private var on = false
    private var voltageRatio = SlewLimiter(1f)
    private val sound = object : LoopedSound("eln:FuelGenerator", coordonate(), ISound.AttenuationType.LINEAR) {
        override fun getVolume() = if (on) 0.2f else 0f
        override fun getPitch() = 0.75f + 1f * voltageRatio.position
    }

    init {
        this.descriptor = descriptor as FuelGeneratorDescriptor
        addLoopedSound(sound)
    }

    override fun draw() {
        renderPreProcess = drawCable(Direction.YN, descriptor.cableRenderDescriptor, eConn, renderPreProcess)
        front.glRotateZnRef()
        descriptor.draw(on)
    }

    override fun cameraDrawOptimisation(): Boolean = false

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        eConn.deserialize(stream)
        val on_ = stream.readBoolean()
        voltageRatio.target = stream.readFloat()
        if (on_ != on) {
            voltageRatio.position = voltageRatio.target
        }
        on = on_
        renderPreProcess = null
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        voltageRatio.step(deltaT)
    }
}

class FuelGeneratorSlowProcess(internal val generator: FuelGeneratorElement) : IProcess {
    override fun process(time: Double) {
        if (generator.on) {
            val power = Math.max(generator.powerSource.effectiveP,
                generator.descriptor.nominalPower * FuelGeneratorDescriptor.MinimalLoadFractionOfNominalPower)
            generator.tankLevel = Math.max(0.0, generator.tankLevel - time *
                FuelGeneratorDescriptor.EfficiencyFactorVsLoadFactor(power / generator.descriptor.nominalPower) *
                power / generator.descriptor.tankEnergyCapacity)

            if (generator.tankLevel <= 0) {
                generator.on = false;
            }

            if (generator.voltageGracePeriod > 0) {
                generator.voltageGracePeriod -= time;
            } else if (generator.positiveLoad.u <
                FuelGeneratorDescriptor.GeneratorBailOutVoltageRatio * generator.descriptor.maxVoltage) {
                generator.on = false;
            }
        }

        if (generator.on) {
            generator.powerSource.p = generator.descriptor.nominalPower
        } else {
            generator.powerSource.p = 0.0
        }
    }
}
