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
import mods.eln.wiki.Data
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import mods.eln.compat.BucketCompat
import java.io.DataInputStream
import java.io.DataOutputStream

class FuelGeneratorDescriptor(name: String, internal val obj: Obj3D?, internal val cable: ElectricalCableDescriptor,
                              internal val nominalPower: Double, internal val maxVoltage: Double,
                              tankCapacityInSecondsAtNominalPower: Double)
    : TransparentNodeDescriptor(name, FuelGeneratorElement::class.java, null) {
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
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable)
    }

    override fun setParent(item: net.minecraft.world.item.Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    // 26.x port: draw() removed (client rendering; redone in P3)


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    override fun addInformation(itemStack: ItemStack, entityPlayer: Player?,
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
    internal val fuels = FuelRegistry.fluidListToFluids(descriptor.fuels).map { FuelRegistry.fluidName(it) } // 1.12: fluids by name (no ids)
    internal var tankLevel = 0.0
    internal var tankFluid = "lava"
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

    override fun onBlockActivated(player: Player?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        if (!(player?.level()?.isClientSide() ?: true)) {
            val bucket = player?.mainHandItem ?: ItemStack.EMPTY
            val fluidStack = BucketCompat.filledBucketFluid(bucket) // 1.7.10 isBucket && isFilledContainer
            if (fluidStack != null) {
                val deltaLevel = 1.0 / FuelGeneratorDescriptor.TankCapacityInBuckets;
                if (tankLevel <= 1.0 - deltaLevel) {
                    if ((FuelRegistry.fluidName(fluidStack.fluid) == tankFluid || tankLevel <= 0.0) &&
                        FuelRegistry.fluidName(fluidStack.fluid) in fuels) {
                        tankFluid = FuelRegistry.fluidName(fluidStack.fluid)
                        tankLevel += deltaLevel
                        if (player != null && !player.isCreative) {
                            val emptyBucket = BucketCompat.emptyBucket();
                            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, emptyBucket)
                        }

                        return true;
                    }
                }
            } else {
                if (player.isHoldingMeter()) {
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

    override fun readFromNBT(nbt: CompoundTag?) {
        super.readFromNBT(nbt)
        tankLevel = nbt?.getDoubleOr("tankLevel", 0.0) ?: 0.0
        // WP16: the fluid in the tank is saved too (1.7.10 reloaded every tank as lava)
        val savedFluid = nbt?.getStringOr("tankFluid", "") ?: ""
        if (savedFluid in fuels) tankFluid = savedFluid
        on = nbt?.getBooleanOr("on", false) ?: false
    }

    override fun writeToNBT(nbt: CompoundTag?) {
        super.writeToNBT(nbt)
        nbt?.putDouble("tankLevel", tankLevel)
        nbt?.putString("tankFluid", tankFluid)
        nbt?.putBoolean("on", on)
    }

    override fun getWaila(): Map<String, String> = mutableMapOf(
        Pair(I18N.tr("State"), if (on) I18N.tr("ON") else I18N.tr("OFF")),
        Pair(I18N.tr("Fuel level"), Utils.plotPercent("", tankLevel)),
        Pair(I18N.tr("Generated power"), Utils.plotPower("", powerSource.effectiveP)),
        Pair(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.u))
    )
}

// 26.x port: class FuelGeneratorRender removed (client; redone in P3/P4)


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
