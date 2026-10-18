package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.sim.process.destruct.IBurnable;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The electrical side of a voxel node's cables (notes/cables/PLAN.md, "Revisions"): its cable pieces grouped into
 * conductors, each one ElectricalLoad (line-simplifiable) and one ThermalLoad with a heater and watchdogs, as one
 * six-node cable element. EA's ElectricalConnection adds both ends' Rs, so a conductor joined on two sides is passed
 * through with twice its Rs: a straight block of cable is 2 Rs, as before. Rebuilt on every connect (cheap: a block
 * holds a few pieces).
 */
final class CableNet {
    /** A knuckle's resistance, as a share of its tier's per-element Rs (a corner adds almost nothing). */
    static final double KNUCKLE_SHARE = 1e-3;

    final VoxelNode node;
    final List<Conductor> conductors = new ArrayList<>();
    final Map<CablePart, Conductor> of = new IdentityHashMap<>();
    final List<ElectricalConnection> internalE = new ArrayList<>();
    final List<ThermalConnection> internalT = new ArrayList<>();

    CableNet(VoxelNode node) {
        this.node = node;
    }

    /** One conductor: pieces of one tier on one face, joined. */
    final class Conductor implements IBurnable {
        final List<CablePart> parts = new ArrayList<>();
        final ElectricalCableDescriptor d;
        final int tier, face;
        final ElectricalLoad load = new ElectricalLoad();
        ThermalLoad thermal;
        final List<IProcess> slow = new ArrayList<>(), thermalSlow = new ArrayList<>();

        Conductor(int tier, int face) {
            this.tier = tier;
            this.face = face;
            this.d = CablePart.TIERS[tier].descriptor();
        }

        double share() {
            if (face < 0) return KNUCKLE_SHARE;
            int len = 0;
            for (CablePart p : parts) len += p.length();
            return Math.max(0.25, Math.min(1, len / 16.0));
        }

        void make() {
            double s = share();
            load.setCanBeSimplifiedByLine(true);
            d.applyTo(load, s);
            // (save() gives every piece its conductor's state; a piece added since starts from the first's)
            double u = parts.get(0).u, t = parts.get(0).t;
            load.state = u;
            Failure failure = new Failure(this, node, node.coordonate, "cable " + d.name).cable();
            if (!d.signalWire && face >= 0) {
                thermal = new ThermalLoad();
                d.applyTo(thermal);
                // a shorter piece: less to heat and less surface to lose it from (Gemini: else short pieces never burn)
                thermal.C = Math.max(d.thermalC * s, Eln.simulator.getMinimalThermalC(d.thermalRs * s, d.thermalRp / s));
                thermal.Rp = d.thermalRp / s;
                thermal.Rs = d.thermalRs * s;
                thermal.setAsSlow();
                thermal.Tc = t;
                thermalSlow.add(new ElectricalLoadHeatThermalLoad(load, thermal));
                slow.add(new ThermalLoadWatchDog().set(thermal).setLimit(d.thermalWarmLimit, d.thermalCoolLimit).set(failure));
            }
            slow.add(new VoltageStateWatchDog().set(load).setUMaxMin(d.electricalNominalVoltage).set(failure));
        }

        int mask() {
            return d.getNodeMask();
        }

        boolean isBurntNow() {
            for (CablePart p : parts) if (p.burnt != null) return true;
            return false;
        }

        @Override
        public FailureCause getBurnt() {
            for (CablePart p : parts) if (p.burnt != null) return p.burnt;
            return null;
        }

        @Override
        public Failure.Kind getBurntKind() {
            return getBurnt() == null ? null : Failure.Kind.CABLE;
        }

        @Override
        public void burn(FailureCause cause, Failure.Kind kind) {
            if (getBurnt() != null) return;
            node.disconnect();
            for (CablePart p : parts) p.burnt = cause;
            node.connect();
            node.setNeedPublish(true);
        }

        @Override
        public void repair() {
            if (getBurnt() == null) return;
            node.disconnect();
            for (CablePart p : parts) {
                p.burnt = null;
                p.t = 0;
            }
            node.connect();
            node.setNeedPublish(true);
        }

        @Override
        public Coordonate getBurnableCoordonate() {
            return node.coordonate;
        }

        @Override
        public ItemStack getDropItemStack() {
            return ItemStack.EMPTY;
        }
    }

    /** Group the node's cable pieces into conductors and make their loads (not yet in the simulation). */
    void build() {
        conductors.clear();
        of.clear();
        internalE.clear();
        internalT.clear();
        List<CablePart> cables = new ArrayList<>();
        for (VoxelPart p : node.parts) if (p instanceof CablePart c) cables.add(c);
        // union by tier, face and touching (knuckles alone)
        int n = cables.size();
        int[] root = new int[n];
        for (int i = 0; i < n; i++) root[i] = i;
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                CablePart a = cables.get(i), b = cables.get(j);
                if (a.isKnuckle() || b.isKnuckle() || a.tier != b.tier || a.face != b.face || !a.touches(b)) continue;
                int ra = find(root, i), rb = find(root, j);
                if (ra != rb) root[ra] = rb;
            }
        Map<Integer, Conductor> byRoot = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            CablePart c = cables.get(i);
            Conductor k = byRoot.computeIfAbsent(find(root, i), r -> new Conductor(c.tier, c.face));
            k.parts.add(c);
            of.put(c, k);
        }
        conductors.addAll(byRoot.values());
        for (Conductor k : conductors) if (k.d != null) k.make();
        // inner corners and tier changes: conductors touching inside the block are joined, as six-node elements were
        for (int i = 0; i < conductors.size(); i++)
            for (int j = i + 1; j < conductors.size(); j++) {
                Conductor a = conductors.get(i), b = conductors.get(j);
                if (a.d == null || b.d == null || a.isBurntNow() || b.isBurntNow()) continue;
                if (!NodeBase.compareConnectionMask(a.mask(), b.mask()) || !touch(a, b)) continue;
                internalE.add(new ElectricalConnection(a.load, b.load));
                if (a.thermal != null && b.thermal != null) internalT.add(new ThermalConnection(a.thermal, b.thermal));
            }
    }

    private static boolean touch(Conductor a, Conductor b) {
        for (CablePart p : a.parts) for (CablePart q : b.parts) if (p.touches(q)) return true;
        return false;
    }

    private static int find(int[] root, int i) {
        while (root[i] != i) i = root[i] = root[root[i]];
        return i;
    }

    /** The working conductors (in the simulation). */
    List<Conductor> live() {
        List<Conductor> out = new ArrayList<>();
        for (Conductor k : conductors) if (k.d != null && !k.isBurntNow()) out.add(k);
        return out;
    }

    void addToSimulation() {
        for (Conductor k : live()) {
            Eln.simulator.addElectricalLoad(k.load);
            if (k.thermal != null) Eln.simulator.addThermalLoad(k.thermal);
            for (IProcess p : k.slow) Eln.simulator.addSlowProcess(p);
            for (IProcess p : k.thermalSlow) Eln.simulator.addThermalSlowProcess(p);
        }
        for (ElectricalConnection c : internalE) Eln.simulator.addElectricalComponent(c);
        for (ThermalConnection c : internalT) Eln.simulator.addThermalConnection(c);
    }

    void removeFromSimulation() {
        save();
        for (ElectricalConnection c : internalE) Eln.simulator.removeElectricalComponent(c);
        for (ThermalConnection c : internalT) Eln.simulator.removeThermalConnection(c);
        for (Conductor k : live()) {
            for (IProcess p : k.slow) Eln.simulator.removeSlowProcess(p);
            for (IProcess p : k.thermalSlow) Eln.simulator.removeThermalSlowProcess(p);
            if (k.thermal != null) Eln.simulator.removeThermalLoad(k.thermal);
            Eln.simulator.removeElectricalLoad(k.load);
        }
    }

    /** Copy each conductor's state into its pieces (for saving, or before a rebuild). */
    void save() {
        for (Conductor k : conductors) {
            if (k.d == null || k.isBurntNow()) continue;
            for (CablePart p : k.parts) {
                p.u = k.load.state;
                if (k.thermal != null) p.t = k.thermal.Tc;
            }
        }
    }

    /** The working conductor with a port at p, or null. */
    Conductor at(CablePart.Port p) {
        for (Conductor k : live())
            for (CablePart c : k.parts) for (CablePart.Port q : c.cablePorts()) if (q.equals(p)) return k;
        return null;
    }

    /** Every port of a working conductor. */
    List<CablePart.Port> ports() {
        List<CablePart.Port> out = new ArrayList<>();
        for (Conductor k : live()) for (CablePart c : k.parts) out.addAll(c.cablePorts());
        return out;
    }

    /** Processes of the conductors (FaultNetwork relieves their watchdogs). */
    List<IProcess> slowProcesses() {
        List<IProcess> out = new ArrayList<>();
        for (Conductor k : live()) out.addAll(k.slow);
        return out;
    }
}
