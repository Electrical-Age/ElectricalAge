package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Condenser Coil (refinery M2; Baughn 2026-11-24: "Condenser Coil", worm is the distiller's word): a 1x1x2 vertical
 * drum with a cooling coil, one fluid node of COIL_B, two blocks high, so its condensate drains at the floor and its
 * holdup stays small (the step-0 sketch held 1-3 g; the heat exchanger kept a permanent heel at its mid-height ports).
 * Its node is the lower block, the upper one a ghost cell (GhostCells). The coil passes heat from its contents to its
 * shell at COIL_UA inside the fluid core's step; the shell gives it to whatever cools its thermal faces (thermal
 * dissipators, any heat sink) and loses a little to the air. Ports:
 * <ul>
 * <li>vapour in: the top of the upper block (draw 1);</li>
 * <li>gas off: the front of the upper block, high (what doesn't condense: natural gas, air, a too-warm coil's vapour);</li>
 * <li>condensate out: the underside of the lower block at its floor (draw 0), and the front of the lower block, low;</li>
 * <li>thermal: the back and both flanks of both blocks.</li>
 * </ul>
 */
public class CondenserCoilElement extends TransparentNodeElement implements FluidDevice, GhostCells.Host {
    /** Its volume (B) and height (blocks); the coil's conductance (W/K, contents to shell). */
    public static final double COIL_B = 2, HEIGHT = 2, COIL_UA = 120;
    public static final double VOLUME = TankDescriptor.bucketsVolume(COIL_B);
    /** Its shell: heat capacity (J/K), loss to the air (K/W: ~2 W/K), a face's resistance (K/W); its limit (K over 20 °C). */
    public static final double SHELL_C = 600, LOSS_RP = 0.5, FACE_RS = 0.005, SHELL_MAX_K = 400;
    public static final double PORT_K = TankElement.PORT_K;
    /** Over this (K) while vapour comes in, gasoline (370 K) no longer condenses well: Jade warns. */
    public static final double WARM_K = 333;
    /**
     * The side flanges' heights (px in their block, as a tank's: LOW, HIGH) and where they draw on the 2-block node, by a
     * tank's rule: the low flange at its bottom edge (liquid leaves a flange over its bottom), the high one at its centre.
     */
    static final int LOW = TankElement.LOW, HIGH = TankElement.HIGH;
    public static final double LOW_DRAW = (LOW - TankElement.FLANGE_HALF) / 32.0, HIGH_DRAW = (16 + HIGH) / 32.0;

    /** Its tooltip lines. */
    static List<String> tooltip() {
        return List.of(I18N.tr("Condenses still vapour into liquid (1×1×2 multiblock)."));
    }

    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    private final GhostCells cells = new GhostCells();
    public FluidNode coil;
    private double[] loaded;
    private List<FluidPort> ports = List.of();
    /** Averaged over a second: what came in (kg/s) and its temperature (K, flow-weighted), condensate out and gas out (kg/s). */
    public double inRate, inK = Double.NaN, liquidRate, gasRate;
    double meterTime, meterIn, meterInK, meterLiquid, meterGas;

    public CondenserCoilElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        thermalLoadList.add(thermal);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(SHELL_MAX_K).set(new Failure(this).machine()));
    }

    // ---- the frame and faces

    @Override
    public boolean thermalFace(int x, int y, int z, Direction side) {
        return side == front.getInverse() || side == front.left() || side == front.right();
    }

    @Override
    public ThermalLoad shell() {
        return thermal;
    }


    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalFace(0, 0, 0, side) ? thermal : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && thermalFace(0, 0, 0, side) ? NodeBase.maskThermal : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    // ---- the fluid device

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    @Override
    public void beforeFluidStep(double dt) {
        coil.wallT = thermal.Tc + PhysicalConstant.Tref;
        coil.wallUA = COIL_UA;
    }

    @Override
    public void afterFluidStep(double dt) {
        thermal.moveEnergyTo(-coil.wallJ);
        // the meters: what flows in (and how hot), condensate out of the low ports, gas out of the high one
        for (FluidEdge e : FluidWorld.edgesOf(coil)) {
            boolean atA = e.a == coil;
            double out = atA ? e.flow : -e.flow, draw = atA ? e.portA : e.portB;
            FluidNode other = atA ? e.b : e.a;
            if (out < 0) {
                meterIn += -out * dt;
                meterInK += -out * dt * other.T();
            } else if (draw < 0.5) meterLiquid += out * dt;
            else meterGas += out * dt;
        }
        meterTime += dt;
        if (meterTime >= 1) {
            inRate = meterIn / meterTime;
            inK = meterIn > 0 ? meterInK / meterIn : Double.NaN;
            liquidRate = meterLiquid / meterTime;
            gasRate = meterGas / meterTime;
            meterIn = meterInK = meterLiquid = meterGas = meterTime = 0;
        }
    }

    // ---- life cycle

    @Override
    public void connectJob() {
        super.connectJob();
        if (coil != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public void initialize() {
        thermal.set(FACE_RS, LOSS_RP, SHELL_C);
        var c = node.coordonate;
        coil = StillPotElement.restored(new FluidNode(VOLUME, c.y, HEIGHT, false), loaded, mods.eln.sim.Ambient.kelvin(c));
        loaded = null;
        int f = front.toEnumFacing().get3DDataValue();
        long lower = c.pos().asLong(), upper = GhostCells.at(this, 0, 1, 0).pos().asLong();
        ports = List.of(new FluidPort(coil, upper, 1, 1.0, PORT_K),
            new FluidPort(coil, upper, f, HIGH_DRAW, PORT_K, TankElement.sideSpot(f, HIGH)),
            new FluidPort(coil, lower, 0, 0, PORT_K),
            new FluidPort(coil, lower, f, LOW_DRAW, PORT_K, TankElement.sideSpot(f, LOW)));
        cells.make(this, List.of(new int[]{0, 1, 0}));
        connect();
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        cells.drop();
    }

    /** (Burst or burnt away: its ghosts go too.) */
    @Override
    public void onSelfDestruction() {
        cells.drop();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (coil != null) mods.eln.fluid.FluidSaves.put(nbt, "coil", coil);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (nbt.contains("coilW")) loaded = mods.eln.fluid.FluidSaves.array(nbt, "coil");
    }

    // ---- what the player reads

    @Override
    public Map<String, String> getWaila() {
        var info = mods.eln.compat.jade.ProbeLines.of();
        if (coil == null) return info;
        if (inRate > 1e-5 && coil.T() > WARM_K) info.headline(I18N.tr("Warning"), I18N.tr("Vapour escaping"));
        String inT = Double.isNaN(inK) || inRate <= 0 ? I18N.tr("None") : mods.eln.api.Meters.celsius(inK - Water.T0);
        info.headline(I18N.tr("Temperature"), inT + " → " + mods.eln.api.Meters.celsius(coil.T() - Water.T0));
        info.headline(I18N.tr("Condensing"), EaUnits.grams(liquidRate * 1000) + "/s");
        info.detail(I18N.tr("Gas out"), EaUnits.grams(gasRate * 1000) + "/s");
        info.detail(I18N.tr("Shell"), mods.eln.api.Meters.celsius(thermal.Tc + PhysicalConstant.Tref - Water.T0));
        info.detail(I18N.tr("Pressure"), String.format("%.2f bar", coil.P() / 1e5));
        info.detail(I18N.tr("Contents"), EaUnits.composition(coil));
        return info;
    }
}
