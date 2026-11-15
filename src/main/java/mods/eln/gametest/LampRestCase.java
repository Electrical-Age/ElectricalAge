package mods.eln.gametest;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.registry.ElnRegistry;
import mods.eln.sixnode.lampsocket.LampSocketRender;
import mods.eln.sixnode.lampsocket.LampSwing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

/**
 * A hanging lamp hangs still indoors and swings outdoors (todo #473), in a world: one lamp under a single block with
 * the sky around it, one under the middle of a 5 x 5 roof. Each one's client render is refreshed in 0.05 s frames;
 * it reads the sky light at its block about once a second (LampSocketRender.refresh).
 */
final class LampRestCase implements TestCase {
    public String id() {
        return "lamp_rest";
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
        return 40;   // (the sky light under the roof has to be worked out first)
    }

    mods.eln.node.six.SixNodeElement open, roofed;

    public void build(TestContext ctx) {
        int id = LabCommands.sixId("Suspended Lamp Socket");
        BlockPos a = ctx.at(0, 3, 3), b = ctx.at(5, 3, 3);
        ctx.floor(a.above());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) ctx.floor(b.offset(dx, 1, dz));
        open = ctx.placeSix(id, a, Direction.YP);
        roofed = ctx.placeSix(id, b, Direction.YP);
    }

    LampSocketRender render(TestContext ctx, String name, mods.eln.node.six.SixNodeElement lamp) {
        BlockPos p = lamp.sixNode.coordonate.pos();
        SixNodeEntity be = new SixNodeEntity(p, ElnRegistry.SIX_NODE_BLOCK.get().defaultBlockState());
        be.setLevel(ctx.level());
        if (!PublishRenderCase.roundTrip(ctx, name, lamp.sixNode, be)) return null;
        return (LampSocketRender) be.elementRenderList[Direction.YP.getInt()];
    }

    /** Frames of 0.05 s in which the lamp moved, of n. */
    static int moved(LampSocketRender r, int n) {
        int moved = 0;
        for (int i = 0; i < n; i++) {
            float before = r.swing().angle();
            r.refresh(0.05f);
            if (r.swing().angle() != before) moved++;
        }
        return moved;
    }

    public void measure(TestContext ctx) {
        mods.eln.client.ClientRenders.registerAll();
        LampSocketRender o = render(ctx, "lamp under the sky", open), r = render(ctx, "lamp under a roof", roofed);
        if (o == null || r == null) return;
        int skyOpen = ctx.level().getBrightness(LightLayer.SKY, open.sixNode.coordonate.pos());
        int skyRoofed = ctx.level().getBrightness(LightLayer.SKY, roofed.sixNode.coordonate.pos());
        ctx.check("lamp_rest: sky light under one block is outdoors", skyOpen >= LampSwing.OUTDOOR_SKY_LIGHT, "sky light " + skyOpen);
        ctx.check("lamp_rest: sky light under the middle of a 5 x 5 roof is indoors", skyRoofed < LampSwing.OUTDOOR_SKY_LIGHT, "sky light " + skyRoofed);

        int mo = moved(o, 1200), mr = moved(r, 1200);
        ctx.check("lamp_rest: the lamp under the sky is outdoors", o.outdoors(), "");
        ctx.check("lamp_rest: it moves in every frame of a minute", mo == 1200 && !o.swing().atRest(), mo + " of 1200 frames, now at " + o.swing().angle() + " deg");
        ctx.check("lamp_rest: the lamp under the roof is indoors", !r.outdoors(), "");
        ctx.check("lamp_rest: it never moves", mr == 0 && r.swing().atRest(), mr + " of 1200 frames");

        // something passes the roofed lamp for a second: it swings, the swing dies out, it rests again
        for (int i = 0; i < 20; i++) {
            r.swing().push(0.2, 0.1, 4f, 0.05f);
            r.refresh(0.05f);
        }
        float pushed = r.swing().angle();
        int frames = 0;
        while (!r.swing().atRest() && frames < 20 * 600) {
            r.refresh(0.05f);
            frames++;
        }
        ctx.check("lamp_rest: a pushed lamp indoors swings and comes to rest", pushed > 1 && r.swing().atRest(), pushed + " deg after the push, at rest " + frames * 0.05f + " s later");
        ctx.check("lamp_rest: and stays there", moved(r, 200) == 0, "");
    }
}
