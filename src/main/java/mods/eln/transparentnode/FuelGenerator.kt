package mods.eln.transparentnode

import mods.eln.gl.GL11
import mods.eln.sound.LoopedSound

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

        const val TankCapacityInBuckets = 2
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

    fun draw(on: Boolean = false) {
        main?.draw()
        mods.eln.anim.Led.draw(switch, on, 1f, 1f, 1f, 1f, 1f, 1f)
    }


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    override fun hasItemDraw() = true

    override fun drawItem(entity: Boolean) {
        objItemScale(obj)
        preserveMatrix {
            Direction.ZP.glRotateXnRef()
            GL11.glTranslatef(0f, -1f, 0f)
            GL11.glScalef(0.6f, 0.6f, 0.6f)
            draw()
        }
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: Player?,
                                list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Produces electricity using gasoline."))
        list.add("  " + tr("Nominal power: %1$ W", Utils.plotValue(nominalPower)))
        list.add(tr("Fuel by pipe on its top, or by bucket. 2% less power per 1% of its fuel boiling outside 330-400 K."))
        list.add(tr("Fuel that cokes fouls it slowly; clean fuel clears it, wool at once."))
    }
}

/**
 * Fuel Generator. Its fuel is in a vessel (DeviceInlet, as the gas turbine's; refinery M1, Baughn 2026-11-15) filled by
 * a pipe on its top flange or by a bucket: 2000 mB, the old two-bucket tank; it burns as much fuel per joule as the old
 * tank did. The fuel is rated (FuelQuality, window GENERATOR_LO..HI): it loses 2 % of its power per % outside the
 * window, and fuel over the engines' coking balance fouls it, ENGINE_SLOWER times slower than a burner; clean fuel
 * clears it, wool cleans it. An old save's tank (tankLevel of tankFluid) goes into the vessel as that much fuel
 * (gasoline if the fluid is unknown, as the old tank burnt anything at the same rate).
 */
class FuelGeneratorElement(transparentNode: TransparentNode, descriptor_: TransparentNodeDescriptor) :
    TransparentNodeElement(transparentNode, descriptor_), mods.eln.sim.fluid.FluidDevice {
    internal var positiveLoad = NbtElectricalLoad("positiveLoad")
    internal var powerSource = PowerSource("powerSource", positiveLoad)
    internal var slowProcess = FuelGeneratorSlowProcess(this)
    internal var descriptor = descriptor_ as FuelGeneratorDescriptor
    internal val fuels = FuelRegistry.fluidListToFluids(descriptor.fuels).map { FuelRegistry.fluidName(it) } // 1.12: fluids by name (no ids)
    /** Its fuel vessel, filled from a pipe on its top flange or by bucket. */
    @JvmField
    val inlet = mods.eln.fluid.DeviceInlet("fuelIn", TANK_MB.toDouble(), *FuelRegistry.fluidListToFluids(descriptor.fuels))   // (not sealed: it breathes, so a gravity tank fills it; a sealed one's air stops the head at ~1 B)
    internal var on by published(false)
    internal var voltageGracePeriod = 0.0
    /** What it burnt last, rated for its window, and how fouled it is (0..FOUL_MAX). An old save loads clean. */
    @JvmField
    var fuel: mods.eln.sim.fluid.FuelQuality.Rating = mods.eln.sim.fluid.FuelQuality.Rating.CLEAN
    @JvmField
    var foul = 0.0
    /** Fuel burnt in all (mB; tests). */
    @JvmField
    var burntMb = 0.0

    /** Is it running, and the power its source gives (W; tests). */
    fun running() = on
    fun sourceP() = powerSource.p

    /** Its fuel as a share of the old two-bucket tank (0..1). */
    val tankLevel: Double get() = Math.min(1.0, inlet.feedMb() / TANK_MB)

    /** The share of its power its fuel's quality and its fouling leave it. */
    fun quality() = fuel.power() * (1 - foul)

    companion object {
        const val TANK_MB = FuelGeneratorDescriptor.TankCapacityInBuckets * 1000
    }

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
        inlet.make(node.coordonate.y.toDouble(), node.coordonate.y + 1.0, mods.eln.sim.Ambient.kelvin(node.coordonate))
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
            // (wool on a fouled generator cleans it, and the wool is used: as the gas turbine and the furnace burner)
            if (foul > 0.01 && bucket.`is`(net.minecraft.tags.ItemTags.WOOL)) {
                foul = 0.0
                if (!player!!.isCreative) bucket.shrink(1)
                needPublish()
                return true
            }
            val fluidStack = BucketCompat.filledBucketFluid(bucket) // 1.7.10 isBucket && isFilledContainer
            if (fluidStack != null) {
                // (a bucket goes into the vessel if it is a fuel it takes and there is room for it: the old tank's rule)
                val ph = mods.eln.fluid.FluidSpecies.of(fluidStack.fluid)
                if (inlet.vessel != null && inlet.liquidMb() <= inlet.ratedMb() - 1000 + 1e-6) {
                    if (ph != null && inlet.takes(ph.id()) && FuelRegistry.fluidName(fluidStack.fluid) in fuels) {
                        inlet.put(fluidStack.fluid, 1000, mods.eln.sim.Ambient.kelvin(node.coordonate))
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
                    rate()   // (what is in the vessel now: a stop on bad fuel must not latch after it is diluted)
                    if (inlet.feedMb() > 0 && quality() > 0) {
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
        if (nbt == null) return
        if (!inlet.read(nbt)) {
            // (an old save: its tank, tankLevel x 2000 mB of tankFluid, goes into the vessel. The old tank burnt every
            // fuel it took at the same rate, and 1.7.10 reloaded every tank as "lava": an unknown fluid is gasoline,
            // so no fuel is lost)
            val level = nbt.getDoubleOr("tankLevel", 0.0)
            if (level > 0) {
                val named = FuelRegistry.fluidListToFluids(arrayOf(nbt.getStringOr("tankFluid", "")))
                    .firstOrNull { f -> mods.eln.fluid.FluidSpecies.of(f)?.let { inlet.takes(it.id()) } ?: false }
                val fluid = named ?: mods.eln.oil.Oil.Cut.GASOLINE.source.get()
                inlet.legacy(net.neoforged.neoforge.fluids.FluidStack(fluid, Math.round(Math.min(1.0, level) * TANK_MB).toInt()))
            }
        }
        on = nbt.getBooleanOr("on", false)
        foul = nbt.getDoubleOr("foul", 0.0)   // (an old save: clean)
        fuel = mods.eln.sim.fluid.FuelQuality.Rating(nbt.getDoubleOr("fuelLight", 0.0), nbt.getDoubleOr("fuelHeavy", 0.0), nbt.getDoubleOr("fuelCoking", 0.0))
        burntMb = nbt.getDoubleOr("burntMb", 0.0)
    }

    override fun writeToNBT(nbt: CompoundTag?) {
        super.writeToNBT(nbt)
        if (nbt == null) return
        inlet.write(nbt)
        nbt.putBoolean("on", on)
        nbt.putDouble("foul", foul)
        nbt.putDouble("fuelLight", fuel.light())
        nbt.putDouble("fuelHeavy", fuel.heavy())
        nbt.putDouble("fuelCoking", fuel.coking())
        nbt.putDouble("burntMb", burntMb)
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf(
            Pair(I18N.tr("State"), if (on) I18N.tr("ON") else I18N.tr("OFF")),
            Pair(I18N.tr("Fuel level"), Utils.plotPercent("", tankLevel)),
            Pair(I18N.tr("Generated power"), Utils.plotPower("", powerSource.effectiveP)),
            Pair(I18N.tr("Voltage"), Utils.plotVolt("", powerSource.u))
        )
        val v = inlet.vessel
        if (v != null) info[I18N.tr("Fuel")] = String.format("%.0f / %d mB, %s", inlet.feedMb(), TANK_MB, mods.eln.sim.fluid.EaUnits.composition(v))
        val wrong = inlet.blockedBy()
        if (wrong != null) info[I18N.tr("State")] = I18N.tr("blocked: it holds %1$, which it can't burn", wrong)
        if (v != null && inlet.feedId() >= 0) {
            val q = mods.eln.sim.fluid.FuelQuality.rate(v, inlet.feedId(), mods.eln.sim.fluid.FuelQuality.GENERATOR_LO, mods.eln.sim.fluid.FuelQuality.GENERATOR_HI)
            val perHour = mods.eln.sim.fluid.FuelQuality.engineFoulPerHour(q.coking())
            info[I18N.tr("Fuel quality")] = String.format("%s; %s", mods.eln.sim.fluid.FuelQuality.describe(q),
                if (perHour > 0) String.format(I18N.tr("fouls it ~%.0f%% an hour at full power (coking %.3f, clean under %.2f)"), perHour * 100, q.coking(), mods.eln.sim.fluid.FuelQuality.ENGINE_BALANCE)
                else I18N.tr("burns clean"))
        }
        if (foul > 0.01) info[I18N.tr("Fouling")] = String.format(I18N.tr("%.0f%% fouled: %.0f%% power (clean fuel clears it slowly; right-click with wool to clean)"), foul * 100, (1 - foul) * 100)
        return info
    }

    // ---- fluid: fuel in at its top flange

    override fun fluidLevel(): Any = node.coordonate.dimention

    override fun fluidPorts(): List<mods.eln.sim.fluid.FluidDevice.FluidPort> =
        if (inlet.stub == null) listOf()
        else listOf(inlet.port(node.coordonate.pos().asLong(), 1, mods.eln.sim.fluid.FluidDevice.FluidPort.CENTRE))

    override fun fluidLinks(): List<mods.eln.sim.fluid.FluidDevice.Link> = inlet.links()

    override fun fluidVents(): List<mods.eln.sim.fluid.FluidDevice.Vent> = inlet.vents()

    /** The burn is here, between the view's sync and show (a change in the slow process would leave the view stale). */
    override fun beforeFluidStep(dt: Double) {
        inlet.sync(dt, mods.eln.sim.Ambient.kelvin(node.coordonate))
        burn(dt)
        inlet.settle(dt)
    }

    override fun afterFluidStep(dt: Double) {
        inlet.after()
    }

    override fun getFluidHandler(): Any? = inlet.view

    override fun connectJob() {
        super.connectJob()
        if (inlet.stub != null) mods.eln.fluid.FluidWorld.add(this)
    }

    override fun disconnectJob() {
        super.disconnectJob()
        mods.eln.fluid.FluidWorld.remove(this)
    }

    /** Rate the feed in its vessel for its window (no feed: the last rating stays). */
    fun rate() {
        val v = inlet.vessel ?: return
        val id = inlet.feedId()
        if (id >= 0) fuel = mods.eln.sim.fluid.FuelQuality.rate(v, id, mods.eln.sim.fluid.FuelQuality.GENERATOR_LO, mods.eln.sim.fluid.FuelQuality.GENERATOR_HI)
    }

    /**
     * A step's fuel out of the vessel: as much per joule as the old tank (tank energy / 2000 mB), at the old
     * efficiency-by-load curve and minimum load. Off-window fuel and fouling give less power for the same fuel (the
     * rest is heat): it burns the fuel of power / quality.
     */
    fun burn(dt: Double) {
        if (!on) return
        val d = descriptor
        val v = inlet.vessel ?: return
        val id = inlet.feedId()
        rate()
        val quality = Math.max(0.05, quality())
        val power = Math.max(powerSource.effectiveP / quality, d.nominalPower * FuelGeneratorDescriptor.MinimalLoadFractionOfNominalPower)
        val want = dt * FuelGeneratorDescriptor.EfficiencyFactorVsLoadFactor(power / d.nominalPower) * power / (d.tankEnergyCapacity / TANK_MB)
        val got = if (id >= 0 && fuel.power() > 0) inlet.take(want) else 0.0
        burntMb += got
        if (got > 0) foul = mods.eln.sim.fluid.FuelQuality.foulEngine(foul, fuel.coking(), power / d.nominalPower, dt * got / want)
        if (got < want * 0.999) {
            on = false
            needPublish()
        }
    }
}

class FuelGeneratorRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(tileEntity, descriptor) {
    internal var descriptor: FuelGeneratorDescriptor
    private var renderPreProcess: CableRenderType? = null
    private val eConn = LRDUMask()
    private var on = false
    private var voltageRatio = SlewLimiter(1f)
    private val sound = object : LoopedSound("eln:FuelGenerator", coordonate(), LoopedSound.Attenuation.LINEAR) {
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
            // (the fuel is burnt in the fluid step: FuelGeneratorElement.burn)
            if (generator.voltageGracePeriod > 0) {
                generator.voltageGracePeriod -= time;
            } else if (generator.positiveLoad.u <
                FuelGeneratorDescriptor.GeneratorBailOutVoltageRatio * generator.descriptor.maxVoltage) {
                generator.on = false;
            }
        }

        if (generator.on) {
            generator.powerSource.p = generator.descriptor.nominalPower * generator.quality()
        } else {
            generator.powerSource.p = 0.0
        }
    }
}
