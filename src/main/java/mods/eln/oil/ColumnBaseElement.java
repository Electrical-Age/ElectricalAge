package mods.eln.oil;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ColumnDescriptor (base): distils crude into the segments stacked on it, by the heat on its sides. */
public class ColumnBaseElement extends TransparentNodeElement {
    /** Cuts per segment count, top to bottom (MACHINES-PLAN); the residue always stays in the base. */
    static final Oil.Cut[][] CUTS = {
        {},
        {Oil.Cut.HEAVYOIL},
        {Oil.Cut.GASOLINE, Oil.Cut.HEAVYOIL},
        {Oil.Cut.NATURALGAS, Oil.Cut.GASOLINE, Oil.Cut.DIESEL},
        {Oil.Cut.NATURALGAS, Oil.Cut.GASOLINE, Oil.Cut.DIESEL, Oil.Cut.HEAVYOIL},
    };
    /** Yield of light crude per cut, by Cut ordinal: gas, gasoline, diesel, heavy oil, residue. */
    static final double[] YIELD = {0.05, 0.30, 0.30, 0.25, 0.10};

    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final ColumnBaseTanks tanks = new ColumnBaseTanks();
    /** Segments above, bottom to top, from the last scan. */
    final List<ColumnSegmentElement> segments = new ArrayList<>();
    final double[] carry = new double[Oil.Cut.values().length];
    double crudeCarry;
    public double crudeRate;
    double meterCrude, meterTime;
    String status = "idle";

    public ColumnBaseElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        thermalLoadList.add(thermal);
        thermalFastProcessList.add(new Distil());
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new Scan());
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(700).set(new Failure(this).machine()));
    }

    /** Where each cut goes with n segments: the cut itself if collected, else the next heavier one (residue last). */
    static Oil.Cut[] fold(int n) {
        List<Oil.Cut> present = new ArrayList<>(List.of(CUTS[n]));
        present.add(Oil.Cut.RESIDUE);
        Oil.Cut[] to = new Oil.Cut[Oil.Cut.values().length];
        for (Oil.Cut c : Oil.Cut.values())
            for (Oil.Cut p : present)
                if (p.ordinal() >= c.ordinal()) {
                    to[c.ordinal()] = p;
                    break;
                }
        return to;
    }

    /** The fraction of crude each cut (by ordinal) gets with n segments. */
    public static double[] fractions(int n) {
        double[] f = new double[Oil.Cut.values().length];
        if (n == 0) return f;
        Oil.Cut[] to = fold(n);
        for (Oil.Cut c : Oil.Cut.values()) f[to[c.ordinal()].ordinal()] += YIELD[c.ordinal()];
        return f;
    }

    class Scan implements IProcess {
        double t = 1;

        @Override
        public void process(double time) {
            t += time;
            if (t < 1) return;
            t = 0;
            scan();
        }
    }

    /** Find the segments stacked on top (up to 4) and tell each its cut. */
    public void scan() {
        for (ColumnSegmentElement s : segments) s.cut = null;
        segments.clear();
        if (NodeManager.instance == null) return;
        for (int i = 1; i <= ColumnDescriptor.MAX_SEGMENTS; i++) {
            NodeBase n = NodeManager.instance.getNodeFromCoordonate(node.coordonate.newWithOffset(0, i, 0));
            if (!(n instanceof TransparentNode tn) || !(tn.element instanceof ColumnSegmentElement seg)) break;
            segments.add(seg);
        }
        int k = segments.size();
        for (int i = 0; i < k; i++) segments.get(i).cut = CUTS[k][k - 1 - i];
    }

    class Distil implements IProcess {
        @Override
        public void process(double time) {
            double used = 0;
            int k = segments.size();
            double over = thermal.Tc + PhysicalConstant.Tref - PhysicalConstant.TCelsius - ColumnDescriptor.DISTIL_C;
            if (k == 0) status = "no column segments on top";
            else if (tanks.crude() <= 0) status = "no crude";
            else if (over <= 0) status = "heating";
            else {
                double[] f = fractions(k);
                // limited by the heat above the distilling point, the crude, and the room in every output
                double n = over * thermal.C / ColumnDescriptor.J_PER_MB + crudeCarry;
                n = Math.min(n, tanks.crude());
                for (Oil.Cut c : Oil.Cut.values()) {
                    if (f[c.ordinal()] <= 0) continue;
                    double room = c == Oil.Cut.RESIDUE ? ColumnDescriptor.RESIDUE_MB - tanks.residue() : roomFor(c);
                    n = Math.min(n, (room - carry[c.ordinal()]) / f[c.ordinal()]);
                }
                int whole = (int) Math.floor(Math.max(0, n));
                crudeCarry = Math.max(0, Math.min(1, n - whole));
                if (whole > 0) {
                    tanks.setCrude(tanks.crude() - whole);
                    for (Oil.Cut c : Oil.Cut.values()) {
                        if (f[c.ordinal()] <= 0) continue;
                        double out = whole * f[c.ordinal()] + carry[c.ordinal()];
                        int put = (int) Math.floor(out);
                        carry[c.ordinal()] = out - put;
                        if (c == Oil.Cut.RESIDUE) tanks.setResidue(tanks.residue() + put);
                        else segmentFor(c).tank.put(c.source.get(), put);
                    }
                    thermal.Tc -= whole * ColumnDescriptor.J_PER_MB / thermal.C;
                    used = whole;
                    status = "distilling";
                } else if (n <= 0) status = "an output is full";
            }
            meterCrude += used;
            meterTime += time;
            if (meterTime >= 1) {
                crudeRate = meterCrude / meterTime;
                meterCrude = 0;
                meterTime = 0;
            }
        }
    }

    ColumnSegmentElement segmentFor(Oil.Cut c) {
        for (ColumnSegmentElement s : segments) if (s.cut == c) return s;
        return null;
    }

    double roomFor(Oil.Cut c) {
        ColumnSegmentElement s = segmentFor(c);
        return s == null ? 0 : s.tank.room(c.source.get());
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermal;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP) return 0;
        return NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    @Override
    public void initialize() {
        // ~1 kJ/K of steel and crude; well lagged (~90 W lost at 350 C)
        thermal.set(0.01, 4.0, 1000);
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tanks;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tanks.writeToNBT(nbt, "column");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tanks.readFromNBT(nbt, "column");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Status"), status);
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        info.put(I18N.tr("Crude"), tanks.crude() + " / " + ColumnDescriptor.CRUDE_MB + " mB");
        info.put(I18N.tr("Residue"), tanks.residue() + " / " + ColumnDescriptor.RESIDUE_MB + " mB");
        info.put(I18N.tr("Segments"), String.valueOf(segments.size()));
        info.put(I18N.tr("Crude distilled"), String.format("%.1f mB/s", crudeRate));
        return info;
    }
}
