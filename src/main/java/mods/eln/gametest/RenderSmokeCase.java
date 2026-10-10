package mods.eln.gametest;

import mods.eln.Eln;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.ElnRenders;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The draw shim without a GPU (P3): every ported element render draws once into a GlFrame on the server thread. It must
 * not throw and must emit vertices; the vertex count and the recording time per draw are logged (the CPU half of the
 * frame-time gate, R2). Renders come from the client's registry (ClientRenders), which names no client classes.
 */
final class RenderSmokeCase implements TestCase {
    public String id() {
        return "render_smoke";
    }

    public int width() {
        return 1;
    }

    public int ticks() {
        return 1;
    }

    public void build(TestContext ctx) {
    }

    static final int ITER = 2000, WARMUP = 500;

    interface Draw {
        void run();
    }

    static void bench(TestContext ctx, String name, Draw draw) {
        try {
            GlFrame.beginRenderFrame();
            GlFrame f = GlFrame.open();
            draw.run();
            GlFrame.close();
            int verts = 0;
            for (GlFrame.Batch b : f.batches().values()) verts += b.vertices();
            int batches = f.batches().size();
            for (int i = 0; i < WARMUP; i++) {
                GlFrame.beginRenderFrame();
                GlFrame.open();
                draw.run();
                GlFrame.close();
            }
            long t0 = System.nanoTime();
            for (int i = 0; i < ITER; i++) {
                GlFrame.beginRenderFrame();
                GlFrame.open();
                draw.run();
                GlFrame.close();
            }
            double us = (System.nanoTime() - t0) / 1000.0 / ITER;
            ctx.line(String.format("INFO render %s: %d vertices in %d batches, %.1f us/draw", name, verts, batches, us));
            ctx.check("render " + name + " emits vertices", verts > 0, verts + " vertices");
        } catch (RuntimeException e) {
            GlFrame.close();
            ctx.check("render " + name + " draws", false, e.toString());
        }
    }

    public void measure(TestContext ctx) {
        mods.eln.client.ClientRenders.registerAll();
        BlockPos p = ctx.at(0, 1, 0);
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        for (SixNodeDescriptor d : Eln.sixNodeItem.all().values()) {
            ElnRenders.Six f = ElnRenders.six(d);
            if (f == null || seen.put(d, true) != null) continue;
            SixNodeEntity be = new SixNodeEntity(p, ElnRegistry.SIX_NODE_BLOCK.get().defaultBlockState());
            be.setLevel(ctx.level());
            SixNodeElementRender r = f.create(be, Direction.YN, d);
            r.front = LRDU.Up;
            r.connectedSide.set(0xF);
            r.needRedrawCable();
            bench(ctx, d.name, r::draw);
        }
        for (TransparentNodeDescriptor d : Eln.transparentNodeItem.all().values()) {
            ElnRenders.Transparent f = ElnRenders.transparent(d);
            if (f == null || seen.put(d, true) != null) continue;
            net.minecraft.world.level.block.state.BlockState state = null;
            for (net.minecraft.world.level.block.Block b : net.minecraft.core.registries.BuiltInRegistries.BLOCK)
                if (b instanceof mods.eln.node.transparent.TransparentNodeBlock tb && tb.descriptor == d) state = b.defaultBlockState();
            if (state == null) {
                ctx.check("render " + d.name + " block", false, "no block");
                continue;
            }
            TransparentNodeEntity be = new TransparentNodeEntity(p, state);
            be.setLevel(ctx.level());
            TransparentNodeElementRender r = f.create(be, d);
            r.front = Direction.XN;
            bench(ctx, d.name, r::draw);
        }
    }
}
