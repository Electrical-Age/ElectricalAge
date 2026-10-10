package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.ElnRenders;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Server publish -> client render, for every device with a render (P3 R3): each device is placed, its node's real
 * publish packet is read by a fresh client-side block entity (as the client does on chunk load), and then drawn into
 * a GlFrame (its recording time is logged). The render must consume the packet exactly (a short read on one face of a six-node block garbles the
 * faces after it), not throw, and emit vertices.
 */
final class PublishRenderCase implements TestCase {
    public String id() {
        return "publish_render";
    }

    public int width() {
        return 9;
    }

    public int height() {
        return 7;
    }

    public int depth() {
        return 7;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    /** Transparent devices 1.12 drew nothing for (empty descriptor draw; see gallery-1.12 close-ups 111, 112). */
    static final java.util.Set<String> INVISIBLE_IN_112 = java.util.Set.of("Power inductor", "Power capacitor",
        "Signal 20H inductor"); // the last: lines to connected sides only, none here
    /** Need a setting this box can't give (water; a 3x3x4 free volume beside the floor): drawn in the gallery instead. */
    static final java.util.Set<String> NOT_HERE = java.util.Set.of("Water Turbine", "Experimental Transporter");

    public void measure(TestContext ctx) {
        mods.eln.client.ClientRenders.registerAll();
        ServerLevel level = ctx.level();
        BlockPos p = ctx.at(4, 1, 1);
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        int ok = 0, all = 0;
        for (var e : Eln.sixNodeItem.all().entrySet()) {
            SixNodeDescriptor d = e.getValue();
            if (ElnRenders.six(d) == null || seen.put(d, true) != null) continue;
            all++;
            NodeBase node = null;
            Direction side = Direction.YN;
            String why = null;
            for (Direction tryside : new Direction[]{Direction.YN, Direction.XN, Direction.YP}) {
                try {
                    if (tryside != Direction.YN) ctx.floor(p.relative(tryside.toEnumFacing()));
                    node = ctx.placeSix(e.getKey(), p, tryside).sixNode;
                    side = tryside;
                    break;
                } catch (RuntimeException ex) {
                    if (why == null) why = ex.getMessage();
                    GoldenSlotsCase.clear(ctx);
                }
            }
            if (node == null) {
                ctx.check("publish " + d.name + " placed", false, why);
                continue;
            }
            SixNodeEntity be = new SixNodeEntity(p, ElnRegistry.SIX_NODE_BLOCK.get().defaultBlockState());
            be.setLevel(level);
            if (roundTrip(ctx, d.name, node, be)) {
                var r = be.elementRenderList[side.getInt()];
                if (r == null) ctx.check("publish " + d.name + " render made", false, "none on " + side);
                else if (INVISIBLE_IN_112.contains(d.name)) {
                    r.draw();
                    ok++;
                } else if (draw(ctx, d.name, r::draw)) ok++;
            }
            GoldenSlotsCase.clear(ctx);
        }
        for (var e : Eln.transparentNodeItem.all().entrySet()) {
            TransparentNodeDescriptor d = e.getValue();
            if (ElnRenders.transparent(d) == null || seen.put(d, true) != null || NOT_HERE.contains(d.name)) continue;
            all++;
            NodeBase node;
            try {
                ctx.floor(p.below());
                node = ctx.placeTransparent(e.getKey(), p).node;
            } catch (RuntimeException ex) {
                ctx.check("publish " + d.name + " placed", false, ex.getMessage());
                GoldenSlotsCase.clear(ctx);
                continue;
            }
            BlockPos at = node.coordonate.pos();
            TransparentNodeEntity be = new TransparentNodeEntity(at, level.getBlockState(at));
            be.setLevel(level);
            if (roundTrip(ctx, d.name, node, be)) {
                if (be.elementRender == null) ctx.check("publish " + d.name + " render made", false, "none");
                else if (INVISIBLE_IN_112.contains(d.name)) {
                    be.elementRender.draw();
                    ok++;
                } else if (draw(ctx, d.name, be.elementRender::draw)) ok++;
            }
            GoldenSlotsCase.clear(ctx);
        }
        ctx.check("publish_render: devices", ok == all, ok + " of " + all);
    }

    /** Feed the node's publish packet to a client-side entity; it must read every byte. */
    static boolean roundTrip(TestContext ctx, String name, NodeBase node, mods.eln.node.NodeBlockEntity be) {
        try {
            byte[] bytes = node.getPublishPacket().toByteArray();
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            in.readByte();
            in.readInt();
            in.readInt();
            in.readInt();
            in.readInt();
            in.readUTF();
            be.serverPublishUnserialize(in);
            int left = in.available();
            ctx.check("publish " + name + " read exactly", left == 0, left + " bytes left of " + bytes.length);
            return left == 0;
        } catch (Exception ex) {
            ctx.check("publish " + name + " read", false, ex.toString());
            return false;
        }
    }

    static boolean draw(TestContext ctx, String name, Runnable draw) {
        try {
            GlFrame.beginRenderFrame();
            GlFrame f = GlFrame.open();
            draw.run();
            GlFrame.close();
            int verts = 0;
            for (GlFrame.Batch b : f.batches().values()) verts += b.vertices();
            ctx.check("publish " + name + " draws", verts > 0, verts + " vertices");
            // recording cost on this CPU (the client's share of the frame-time gate, R2)
            for (int i = 0; i < 300; i++) {
                GlFrame.beginRenderFrame();
                GlFrame.open();
                draw.run();
                GlFrame.close();
            }
            long t0 = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                GlFrame.beginRenderFrame();
                GlFrame.open();
                draw.run();
                GlFrame.close();
            }
            ctx.line(String.format("INFO render %s: %d vertices, %.1f us/draw", name, verts, (System.nanoTime() - t0) / 1000.0 / 1000));
            return verts > 0;
        } catch (RuntimeException ex) {
            GlFrame.close();
            ctx.check("publish " + name + " draws", false, ex.toString());
            return false;
        }
    }
}
