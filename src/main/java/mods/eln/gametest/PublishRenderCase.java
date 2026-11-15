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
 * faces after it), not throw, and emit vertices. Each device is then drawn with its unchanging parts in the chunk's
 * geometry ({@link BakeOnceCheck}): the picture must be the same.
 */
public final class PublishRenderCase implements TestCase {
    /** Takes each device's draw in place of the checks on it (the render benchmark, gametest/bench/RenderBench). */
    public interface Probe {
        void device(String name, boolean six, Object owner, BlockPos pos, Runnable draw);
    }

    public static Probe probe;

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
    /** Devices with no render class in the port yet (Modbus RTU: modbus is off, the device unregistered). */
    static final java.util.Set<String> NO_RENDER_YET = java.util.Set.of("Modbus RTU");
    /** Need a setting this box can't give (water; a 3x3x4 free volume beside the floor): drawn in the gallery instead. */
    static final java.util.Set<String> NOT_HERE = java.util.Set.of("Water Turbine", "Experimental Transporter");

    public void measure(TestContext ctx) {
        mods.eln.client.ClientRenders.registerAll();
        ServerLevel level = ctx.level();
        BlockPos p = ctx.at(4, 1, 1);
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        int ok = 0, all = 0;
        BakeOnceCheck.reset();
        // every device has a render registered (ClientRenders): without one its node draws nothing in the world, while
        // its item (drawn from the descriptor) looks fine (the electrical anvil, 2026-10-28)
        java.util.Set<String> noRender = new java.util.TreeSet<>();
        for (SixNodeDescriptor d : Eln.sixNodeItem.all().values()) if (ElnRenders.six(d) == null && !NO_RENDER_YET.contains(d.name)) noRender.add(d.name);
        for (TransparentNodeDescriptor d : Eln.transparentNodeItem.all().values()) if (ElnRenders.transparent(d) == null) noRender.add(d.name);
        ctx.check("publish: every device has a render (ClientRenders)", noRender.isEmpty(), "none for " + noRender);
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
                } else if (probe != null) {
                    ok++;
                    probe.device(d.name, true, be, p, r::draw);
                } else if (draw(ctx, d.name, r::draw)) {
                    ok++;
                    BakeOnceCheck.check(ctx, d.name, be, p, r::draw);
                }
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
                if (d instanceof mods.eln.oil.SubmersiblePumpDescriptor)   // (it goes in water)
                    ctx.level().setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
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
                // a device drawn by its block model (the FE exporter) has nothing for the element render to draw
                if (be.elementRender == null) ctx.check("publish " + d.name + " render made", false, "none");
                else if (INVISIBLE_IN_112.contains(d.name) || level.getBlockState(at).getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL) {
                    be.elementRender.draw();
                    ok++;
                } else if (probe != null) {
                    ok++;
                    probe.device(d.name, false, be, at, be.elementRender::draw);
                } else if (draw(ctx, d.name, be.elementRender::draw)) {
                    ok++;
                    BakeOnceCheck.check(ctx, d.name, be, at, be.elementRender::draw);
                }
            }
            GoldenSlotsCase.clear(ctx);
        }
        // a transformer's coils (a fresh one has no windings, so the loop above drew none): wire of each set of sprites
        // (WindingTexture: normal, dark, bright), one coil overfull
        {
            var tr = mods.eln.transparentnode.transformer.Coil.of(12, 2.5, 150, 117);
            var fine = mods.eln.transparentnode.transformer.Coil.of(58, 1.5, 150, 117);
            var over = mods.eln.transparentnode.transformer.Coil.of(64, 2.5, 150, 320);
            int[][] wires = {{0xB02E26, 0x1D1D21}, {0xF9FFFE, 0x3C44AA}};
            for (int[] w : wires) {
                boolean first = w == wires[0];
                BakeOnceCheck.check(ctx, "Transformer coils " + (first ? "(red, black)" : "(white, blue overfull)"), new Object(), p, () -> {
                    mods.eln.transparentnode.transformer.TransformerDescriptor.drawCoil(tr, w[0]);
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.gl.GL11.glRotatef(180, 0f, 1f, 0f);
                    mods.eln.transparentnode.transformer.TransformerDescriptor.drawCoil(first ? fine : over, w[1]);
                    mods.eln.gl.GL11.glPopMatrix();
                });
            }
        }
        woundTransformer(ctx, p);
        ctx.check("publish_render: devices", ok == all, ok + " of " + all);
        if (probe == null) BakeOnceCheck.summary(ctx, 0.9);   // (98.9% when this was written)
    }

    /**
     * A transformer with windings on a core, drawn by its render from its publish packet: its coils (todo #423) are the
     * chunk's once settled, so winding it leaves the renderer no more vertices a frame than the bare one has.
     */
    private static void woundTransformer(TestContext ctx, BlockPos p) {
        ServerLevel level = ctx.level();
        String name = "DC-DC Converter wound (14 and 7 turns, cheap core)";
        try {
            ctx.floor(p.below());
            var el = ctx.placeTransparent(0 + (2 << 6), p);   // (Wp10aCases.TRANSFORMER)
            var core = Eln.sharedItem.getDescriptor(0 + (7 << 6));   // (Wp10aCases.CORE_CHEAP)
            if (!(el instanceof mods.eln.transparentnode.transformer.TransformerElement tr) || !(core instanceof mods.eln.item.FerromagneticCoreDescriptor)) {
                ctx.check("publish " + name + " placed", false, el + " with " + core);
                return;
            }
            BlockPos at = el.node.coordonate.pos();
            int[][] counts = new int[2][];
            int coilVertices = 0;
            for (int wound = 0; wound < 2; wound++) {
                if (wound == 1) {
                    var cable = mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor;
                    tr.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.primaryCableSlotId, cable.newItemStack(14));
                    tr.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.secondaryCableSlotId, cable.newItemStack(7));
                    tr.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.ferromagneticSlotId, core.newItemStack(1));
                    tr.getInventory().setChanged();
                }
                TransparentNodeEntity be = new TransparentNodeEntity(at, level.getBlockState(at));
                be.setLevel(level);
                if (!roundTrip(ctx, wound == 1 ? name : "DC-DC Converter bare", el.node, be) || be.elementRender == null) return;
                if (wound == 1) {
                    GlFrame.beginRenderFrame();
                    GlFrame f = GlFrame.open();
                    try {
                        be.elementRender.draw();
                    } finally {
                        GlFrame.close();
                    }
                    for (var b : f.batches().entrySet()) {
                        var t = b.getKey().texture();
                        if (t != null && t.getPath().startsWith("sprites/winding/")) coilVertices += b.getValue().vertices();
                    }
                }
                counts[wound] = BakeOnceCheck.check(ctx, wound == 1 ? name : "DC-DC Converter bare", be, at, be.elementRender::draw);
            }
            int bareLeft = counts[0][0] - counts[0][1], woundLeft = counts[1][0] - counts[1][1];
            ctx.check("publish " + name + ": it draws two coils", coilVertices >= 2 * mods.eln.transparentnode.transformer.TransformerDescriptor.coilVertices(1), coilVertices + " vertices under winding sprites");
            ctx.check("publish " + name + ": the coils are the chunk's", woundLeft <= bareLeft && counts[1][1] - counts[0][1] >= coilVertices,
                woundLeft + " vertices a frame left with the renderer (bare: " + bareLeft + "); the chunk has " + counts[1][1] + " (bare: " + counts[0][1] + ")");
            ctx.line(String.format("INFO coil bake: a wound transformer draws %d vertices, %d of them coils; left with the renderer once settled: %d (bare: %d)",
                counts[1][0], coilVertices, woundLeft, bareLeft));
        } catch (RuntimeException ex) {
            ctx.check("publish " + name, false, ex.toString());
        } finally {
            GoldenSlotsCase.clear(ctx);
        }
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
