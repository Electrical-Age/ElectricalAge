package mods.eln.gametest;

import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.transparentnode.autominer.AutoMinerRender;
import net.minecraft.core.BlockPos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Autominer drill animation (P3): a client render told "job ore, power ok" spins the pipe and drill head (1.12: rotSpeed
 * eases to 360 deg/s, pipeAlpha integrates it in refresh), and stops again on another job. Mining itself needs an
 * 800 V supply, a drill, pipes and a chest: the render is fed the publish packet the element would send instead.
 */
final class AutoMinerSpinCase implements TestCase {
    public String id() {
        return "autominer_spin";
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

    static void publish(AutoMinerRender r, int job, boolean powerOk) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(bos);
        o.writeByte(Direction.XN.getInt());
        o.writeShort(3);
        o.writeByte(job);
        o.writeBoolean(powerOk);
        o.writeBoolean(false);
        r.networkUnserialize(new DataInputStream(new ByteArrayInputStream(bos.toByteArray())));
    }

    static float alpha(AutoMinerRender r) throws Exception {
        var f = AutoMinerRender.class.getDeclaredField("pipeAlpha");
        f.setAccessible(true);
        return f.getFloat(r);
    }

    /** Degrees turned over `seconds` of 0.05 s client frames (unwrapped). */
    static float turn(AutoMinerRender r, float seconds) throws Exception {
        float total = 0, last = alpha(r);
        for (float t = 0; t < seconds; t += 0.05f) {
            r.refresh(0.05f);
            float a = alpha(r);
            float d = a - last;
            if (d < -180) d += 360;
            total += d;
            last = a;
        }
        return total;
    }

    public void measure(TestContext ctx) {
        mods.eln.client.ClientRenders.registerAll();
        try {
            var miner = ctx.placeTransparent(LabCommands.transparentId("Auto Miner"), ctx.at(4, 3, 3));
            BlockPos at = miner.node.coordonate.pos();
            TransparentNodeEntity be = new TransparentNodeEntity(at, ctx.level().getBlockState(at));
            be.setLevel(ctx.level());
            if (!PublishRenderCase.roundTrip(ctx, "Auto Miner", miner.node, be)) return;
            AutoMinerRender r = (AutoMinerRender) be.elementRender;
            int ore = 4, none = 0; // AutoMinerSlowProcess.jobType: none, done, full, chestFull, ore, ...
            publish(r, none, true);
            float idle = turn(r, 2);
            ctx.check("autominer: idle drill stands still", Math.abs(idle) < 1, idle + " deg in 2 s");
            publish(r, ore, true);
            turn(r, 5); // spin up (RC time constant 1 s)
            float spinning = turn(r, 1);
            ctx.check("autominer: mining drill turns ~360 deg/s", spinning > 330 && spinning < 370, spinning + " deg in 1 s");
            ctx.check("autominer: spinning drill draws", PublishRenderCase.draw(ctx, "Auto Miner (mining)", r::draw), "");
            publish(r, none, true);
            turn(r, 6);
            float after = turn(r, 1);
            ctx.check("autominer: drill stops after the job", Math.abs(after) < 5, after + " deg in 1 s");
        } catch (Exception e) {
            ctx.check("autominer spin", false, e.toString());
        }
    }
}
