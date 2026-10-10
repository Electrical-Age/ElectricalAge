package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.selftest.Reload;
import mods.eln.selftest.SlotsDump;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sim.process.destruct.IBurnable;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Every EA device, alone: it can be placed, it survives the run unconnected (not destroyed, not burnt), and a
 * save/load round trip (Reload) gives the same NBT and the same client packet. Port step T3 (notes/port26/PLAN.md).
 * Devices sit on single stone blocks: six-node ones 10 apart (on their first allowed face), transparent ones 16 apart, from x = +40 of the case row (clear of the other cases);
 * multiblocks get their ghost space from the spacing.
 */
public final class SmokeCases {
    private SmokeCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new SmokeCase());
    }

    static final int SPACING = 10, COLUMNS = 14, X0 = 40;

    public static final class Device {
        final boolean six;
        final int damage;
        final String name;
        BlockPos at;
        Direction side = Direction.YN;
        String placeError;

        Device(boolean six, int damage, String name) {
            this.six = six;
            this.damage = damage;
            this.name = name;
        }
    }

    /** One entry per descriptor (aliases share a descriptor), six-node then transparent, in damage order. */
    static List<Device> allDevices() {
        List<Device> out = new ArrayList<>();
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        for (Map.Entry<Integer, SixNodeDescriptor> e : new TreeMap<>(Eln.sixNodeItem.subItemList).entrySet())
            if (seen.put(e.getValue(), true) == null) out.add(new Device(true, e.getKey(), e.getValue().name));
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.subItemList).entrySet())
            if (seen.put(e.getValue(), true) == null) out.add(new Device(false, e.getKey(), e.getValue().name));
        return out;
    }

    /** Place d at p with its support block(s); sets d.at/d.side, or d.placeError. */
    static void place(SelfTestContext ctx, Device d, BlockPos p) {
        try {
            if (d.six) {
                // the first face it may sit on: floor unless the descriptor says otherwise (wall/ceiling sockets)
                Direction[] faces = Eln.sixNodeItem.getDescriptor(d.damage).getPlaceDirection();
                Direction side = faces == null || faces.length == 0 ? Direction.YN : faces[0];
                ctx.floor(p.add(side.toEnumFacing().getDirectionVec()));
                ctx.placeSix(d.damage, p, side);
                d.side = side;
                d.at = p;
            } else {
                // floor under the clicked block and under the node (spawn delta, for front XN as placed below;
                // teleporter: (4, 0, 0)). No more: the water turbine's ghosts reach down.
                TransparentNodeDescriptor td = Eln.transparentNodeItem.getDescriptor(d.damage);
                ctx.floor(p.down());
                ctx.floor(p.add(td.getSpawnDeltaX(), td.getSpawnDeltaY() - 1, td.getSpawnDeltaZ()));
                // facing XN like the per-family cases (the player's view otherwise picks the front)
                TransparentNodeElement e;
                try {
                    e = Wp10bCases.placeFacing(ctx, d.damage, p, Direction.XN);
                } catch (IllegalStateException noSuchFront) {
                    e = ctx.placeTransparent(d.damage, p);
                }
                d.at = new BlockPos(e.node.coordonate.x, e.node.coordonate.y, e.node.coordonate.z);
            }
        } catch (RuntimeException ex) {
            d.placeError = ex.toString();
        }
    }

    static final class SmokeCase implements SelfTestCase {
        final List<Device> devices = new ArrayList<>();

        public String name() {
            return "smoke every device";
        }

        public int width() {
            return 1;
        }

        public void build(SelfTestContext ctx) {
            devices.addAll(allDevices());

            // six-node devices on a 10-block grid; transparent ones (multiblocks, spawn deltas up to 4) 16 apart behind them
            int i = 0, t = 0, sixRows = 0;
            for (Device d : devices) if (d.six) sixRows = Math.max(sixRows, i++ / COLUMNS + 1);
            i = 0;
            for (Device d : devices) {
                BlockPos p = d.six
                    ? ctx.at(X0 + (i % COLUMNS) * SPACING, 1, (i / COLUMNS) * SPACING)
                    : ctx.at(X0 + (t % COLUMNS) * 16, 1, sixRows * SPACING + 8 + (t / COLUMNS) * 16);
                if (d.six) i++;
                else t++;
                place(ctx, d, p);
            }
        }

        public void measure(SelfTestContext ctx) {
            java.util.Map<String, com.google.gson.JsonObject> slots = new java.util.TreeMap<>();
            net.minecraft.entity.player.EntityPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(ctx.world());
            for (Device d : devices) {
                String what = "smoke " + (d.six ? "six " : "transparent ") + d.damage + " " + d.name;
                if (d.placeError != null) {
                    ctx.check(what + ": placed", false, d.placeError);
                    continue;
                }
                NodeBase n = NodeManager.instance.getNodeFromCoordonate(
                    new Coordonate(d.at.getX(), d.at.getY(), d.at.getZ(), ctx.world()));
                boolean alive = n != null && !n.isDestructing();
                IBurnable b = null;
                if (n instanceof SixNode) b = ((SixNode) n).getElement(d.side);
                if (n instanceof TransparentNode) b = ((TransparentNode) n).element;
                ctx.check(what + ": placed, survives unconnected", alive && b != null && !b.isBurnt(),
                    n == null ? "node gone" : b == null ? "no element" : b.isBurnt() ? "burnt: " + b.getBurnt() : "");
                if (!alive) continue;
                // GUI slot rules (golden eln-slots.json) of devices with a GUI, from the element as placed, before the reload
                com.google.gson.JsonObject sl = null;
                if (n instanceof SixNode) {
                    SixNodeElement e = ((SixNode) n).getElement(d.side);
                    if (e.hasGui()) sl = SlotsDump.record(d.name, e::newContainer, d.side, player);
                } else if (n instanceof TransparentNode) {
                    TransparentNodeElement e = ((TransparentNode) n).element;
                    if (e.hasGui()) sl = SlotsDump.record(d.name, e::newContainer, Direction.YP, player);
                }
                if (sl != null) slots.put(String.format("%s %05d %s", d.six ? "six" : "transparent", d.damage, d.name), sl);
                try {
                    Reload r = Reload.node(ctx.world(), d.at);
                    ctx.check(what + ": save/load keeps the NBT", r.tagsEqual(), String.join("; ", r.differences()));
                    ctx.check(what + ": save/load keeps the client state", r.publishEqual(),
                        r.publishBefore.length + " -> " + r.publishAfter.length + " bytes");
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    ctx.check(what + ": save/load", false, ex.toString());
                }
            }
            SlotsDump.write(slots);
            ctx.line("INFO smoke: GUI slot rules of " + slots.size() + " devices written to eln-slots.json");
        }
    }
}
